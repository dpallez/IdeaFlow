package org.ideaflow.knime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ideaflow.api.ConstraintDefinition;
import org.ideaflow.api.ConstraintRelation;
import org.ideaflow.api.IdeaFlowState;
import org.ideaflow.api.OptimizationDirection;
import org.ideaflow.core.EvolutionSchedule;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;

/** Shared exact-evaluation validation, constraint accounting, NFE, and clean event creation. */
public final class EvaluationFinalizer {
  public record Objective(String column, OptimizationDirection direction) {}

  public record ProblemDetails(
      String problemId,
      List<Objective> objectives,
      List<ConstraintDefinition> constraints,
      ProblemMetadata.Schema metadata) {
    public ProblemDetails {
      objectives = List.copyOf(objectives);
      constraints = List.copyOf(constraints);
    }
  }

  private EvaluationFinalizer() {}

  public static ProblemDetails fromMetadata(final DataTableSpec problemSpec)
      throws InvalidSettingsException {
    final ProblemMetadata.Schema metadata = ProblemMetadata.require(problemSpec);
    return new ProblemDetails(
        metadata.problemId(),
        metadata.objectives().stream()
            .map(objective -> new Objective(objective.column(), objective.direction()))
            .toList(),
        metadata.constraints().stream()
            .map(
                constraint ->
                    new ConstraintDefinition(
                        constraint.column(),
                        constraint.relation(),
                        constraint.threshold(),
                        constraint.tolerance()))
            .toList(),
        metadata);
  }

  // Runtime validation mirrors configure-time metadata checks and protects against edited input
  // tables.
  public static ProblemDetails readProblemDefinition(final BufferedDataTable table)
      throws InvalidSettingsException {
    final DataTableSpec spec = table.getDataTableSpec();
    requireProblemDefinitionStructure(spec);
    final int problemIdIndex = spec.findColumnIndex("problem_id");
    final int kindIndex = spec.findColumnIndex("kind");
    final int nameIndex = spec.findColumnIndex("name");
    final int directionIndex = spec.findColumnIndex("direction");
    final int relationIndex = spec.findColumnIndex("relation");
    final int thresholdIndex = spec.findColumnIndex("threshold");
    final int toleranceIndex = spec.findColumnIndex("tolerance");
    final List<Objective> objectives = new ArrayList<>();
    final List<ConstraintDefinition> constraints = new ArrayList<>();
    final Set<String> resultColumns = new HashSet<>();
    String problemId = null;
    for (DataRow row : table) {
      final String rowProblemId = requiredText(row.getCell(problemIdIndex), "problem_id");
      if (problemId == null) problemId = rowProblemId;
      else if (!problemId.equals(rowProblemId)) {
        throw new InvalidSettingsException("Evaluation accepts one problem definition at a time.");
      }
      final String kind = requiredText(row.getCell(kindIndex), "kind");
      if ("objective".equalsIgnoreCase(kind)) {
        final String column = requiredText(row.getCell(nameIndex), "objective name");
        requireUniqueResultColumn(resultColumns, column);
        objectives.add(
            new Objective(
                column,
                OptimizationDirection.parse(
                    requiredText(row.getCell(directionIndex), "direction"))));
      } else if ("constraint".equalsIgnoreCase(kind)) {
        final String column = requiredText(row.getCell(nameIndex), "constraint name");
        requireUniqueResultColumn(resultColumns, column);
        if (row.getCell(thresholdIndex).isMissing() || row.getCell(toleranceIndex).isMissing()) {
          throw new InvalidSettingsException(
              "Constraint threshold and tolerance are required: " + column);
        }
        constraints.add(
            new ConstraintDefinition(
                column,
                ConstraintRelation.valueOf(requiredText(row.getCell(relationIndex), "relation")),
                ((DoubleValue) row.getCell(thresholdIndex)).getDoubleValue(),
                ((DoubleValue) row.getCell(toleranceIndex)).getDoubleValue()));
      }
    }
    if (problemId == null)
      throw new InvalidSettingsException("Problem definition must not be empty.");
    if (objectives.isEmpty()) {
      throw new InvalidSettingsException("Problem definition must contain an objective.");
    }
    return new ProblemDetails(
        problemId, objectives, constraints, ProblemMetadata.read(spec).orElse(null));
  }

  public static DataTableSpec evaluatedPopulationSpec(
      final DataTableSpec input, final ProblemDetails problem) {
    final List<DataColumnSpec> additions = new ArrayList<>();
    additions.add(KnimeTableSupport.longColumn(PopulationState.NFE));
    if (!problem.constraints().isEmpty()) {
      additions.add(KnimeTableSupport.doubleColumn(PopulationState.CONSTRAINT_VIOLATION));
      additions.add(
          new org.knime.core.data.DataColumnSpecCreator(PopulationState.FEASIBLE, BooleanCell.TYPE)
              .createSpec());
    }
    return KnimeTableSupport.appendOrReplace(input, additions.toArray(DataColumnSpec[]::new));
  }

  public static DataTableSpec eventSpec(final ProblemDetails problem) {
    final List<DataColumnSpec> columns =
        new ArrayList<>(
            List.of(
                KnimeTableSupport.stringColumn("Run"),
                KnimeTableSupport.stringColumn("Population"),
                KnimeTableSupport.stringColumn("Individual"),
                KnimeTableSupport.longColumn(PopulationState.NFE),
                KnimeTableSupport.stringColumn("Stage"),
                KnimeTableSupport.stringColumn("Operator")));
    if (!problem.constraints().isEmpty()) {
      columns.add(KnimeTableSupport.doubleColumn(PopulationState.CONSTRAINT_VIOLATION));
      columns.add(
          new org.knime.core.data.DataColumnSpecCreator(PopulationState.FEASIBLE, BooleanCell.TYPE)
              .createSpec());
    }
    for (Objective objective : problem.objectives()) {
      columns.add(KnimeTableSupport.doubleColumn(objective.column()));
    }
    final DataTableSpec result = new DataTableSpec(columns.toArray(DataColumnSpec[]::new));
    return problem.metadata() == null
        ? result
        : ProblemMetadata.attach(result, "Run", problem.metadata());
  }

  // This is the single accounting boundary: one finalized row consumes one function evaluation.
  public static BufferedDataTable[] finalizePopulation(
      final BufferedDataTable population,
      final ProblemDetails problem,
      final ExecutionContext execution,
      final String operator)
      throws Exception {
    final DataTableSpec inputSpec = population.getDataTableSpec();
    validateEvaluationColumns(inputSpec, problem);
    final DataTableSpec outputSpec = evaluatedPopulationSpec(inputSpec, problem);
    final DataTableSpec eventsSpec = eventSpec(problem);
    final BufferedDataContainer populationOut = execution.createDataContainer(outputSpec);
    final BufferedDataContainer eventOut = execution.createDataContainer(eventsSpec);
    final List<String> objectiveNames =
        problem.objectives().stream().map(Objective::column).toList();
    final List<String> constraintNames =
        problem.constraints().stream().map(ConstraintDefinition::column).toList();
    final int[] objectiveIndices =
        KnimeTableSupport.requireNumericColumns(inputSpec, objectiveNames);
    final int[] constraintIndices =
        constraintNames.isEmpty()
            ? new int[0]
            : KnimeTableSupport.requireNumericColumns(inputSpec, constraintNames);

    final Map<String, Long> previousNfe = new HashMap<>();
    final Map<String, Long> unevaluatedCount = new HashMap<>();
    for (DataRow row : population) {
      final IdeaFlowState state = PopulationState.get(row, inputSpec);
      final String run = state.text(IdeaFlowState.RUN, "run");
      previousNfe.merge(run, PopulationState.nfe(row, inputSpec), Math::max);
      if (!state.booleanValue(IdeaFlowState.EVALUATED, false)) {
        unevaluatedCount.merge(run, 1L, Long::sum);
      }
    }
    final long maximumNfe = ProblemMetadata.require(inputSpec).maxEvaluations();
    for (Map.Entry<String, Long> entry : unevaluatedCount.entrySet()) {
      final long currentNfe = previousNfe.getOrDefault(entry.getKey(), 0L);
      if (!EvolutionSchedule.canEvaluateBatch(currentNfe, entry.getValue(), maximumNfe)) {
        throw new InvalidSettingsException(
            "Evaluating "
                + entry.getValue()
                + " candidates for run '"
                + entry.getKey()
                + "' at NFE "
                + currentNfe
                + " would exceed the maximum NFE "
                + maximumNfe
                + ". Reduce the initial population or stop before this complete generation.");
      }
    }
    final Map<String, Long> nextEvaluation = new HashMap<>(previousNfe);
    final Map<String, Long> finalNfe = new HashMap<>();
    previousNfe.forEach(finalNfe::put);
    unevaluatedCount.forEach(
        (run, count) -> finalNfe.put(run, previousNfe.getOrDefault(run, 0L) + count));

    long eventNumber = 0;
    long processed = 0;
    for (DataRow row : population) {
      IdeaFlowState state = PopulationState.get(row, inputSpec);
      final String run = state.text(IdeaFlowState.RUN, "run");
      final String populationId = state.text(IdeaFlowState.POPULATION, "population-0");
      final String individual = state.text(IdeaFlowState.INDIVIDUAL, row.getKey().getString());
      final boolean alreadyEvaluated = state.booleanValue(IdeaFlowState.EVALUATED, false);
      final DataCell[] output = KnimeTableSupport.copyToSpec(row, inputSpec, outputSpec);

      double violation = 0.0;
      for (int index = 0; index < constraintIndices.length; index++) {
        final ConstraintDefinition constraint = problem.constraints().get(index);
        violation +=
            constraint.violation(
                KnimeTableSupport.number(
                    row.getCell(constraintIndices[index]), row, constraint.column()));
      }
      final double[] objectives = new double[objectiveIndices.length];
      for (int index = 0; index < objectives.length; index++) {
        objectives[index] =
            KnimeTableSupport.number(
                row.getCell(objectiveIndices[index]), row, objectiveNames.get(index));
      }

      final long evaluation =
          alreadyEvaluated
              ? state.longValue(IdeaFlowState.EVALUATION, PopulationState.nfe(row, inputSpec))
              : nextEvaluation.compute(run, (key, value) -> value == null ? 1L : value + 1L);
      final long currentNfe = finalNfe.getOrDefault(run, evaluation);
      state = state.with(IdeaFlowState.EVALUATED, true).with(IdeaFlowState.EVALUATION, evaluation);
      PopulationState.set(output, outputSpec, state);
      PopulationState.setNfe(output, outputSpec, currentNfe);
      if (!problem.constraints().isEmpty()) {
        output[outputSpec.findColumnIndex(PopulationState.CONSTRAINT_VIOLATION)] =
            new DoubleCell(violation);
        output[outputSpec.findColumnIndex(PopulationState.FEASIBLE)] =
            BooleanCell.get(violation <= 0.0);
      }
      populationOut.addRowToTable(new DefaultRow(row.getKey(), output));

      if (!alreadyEvaluated) {
        final int base = problem.constraints().isEmpty() ? 6 : 8;
        final DataCell[] event = new DataCell[base + objectives.length];
        event[0] = new StringCell(run);
        event[1] = new StringCell(populationId);
        event[2] = new StringCell(individual);
        event[3] = new LongCell(evaluation);
        event[4] = new StringCell("evaluation");
        event[5] = new StringCell(operator);
        if (!problem.constraints().isEmpty()) {
          event[6] = new DoubleCell(violation);
          event[7] = BooleanCell.get(violation <= 0.0);
        }
        for (int index = 0; index < objectives.length; index++) {
          event[base + index] = new DoubleCell(objectives[index]);
        }
        eventOut.addRowToTable(new DefaultRow(new RowKey("Event" + eventNumber++), event));
      }
      execution.checkCanceled();
      processed++;
      if (population.size() > 0) execution.setProgress((double) processed / population.size());
    }
    populationOut.close();
    eventOut.close();
    return new BufferedDataTable[] {populationOut.getTable(), eventOut.getTable()};
  }

  public static void validateEvaluationColumns(
      final DataTableSpec spec, final ProblemDetails problem) throws InvalidSettingsException {
    requirePopulationStructure(spec);
    KnimeTableSupport.requireNumericColumns(
        spec, problem.objectives().stream().map(Objective::column).toList());
    final List<String> constraints =
        problem.constraints().stream().map(ConstraintDefinition::column).toList();
    if (!constraints.isEmpty()) KnimeTableSupport.requireNumericColumns(spec, constraints);
  }

  public static void requirePopulationStructure(final DataTableSpec spec)
      throws InvalidSettingsException {
    PopulationState.require(spec);
  }

  public static void requireProblemDefinitionStructure(final DataTableSpec spec)
      throws InvalidSettingsException {
    for (String required :
        List.of("problem_id", "kind", "name", "direction", "relation", "threshold", "tolerance")) {
      if (spec.findColumnIndex(required) < 0) {
        throw new InvalidSettingsException("Problem definition input is missing " + required);
      }
    }
  }

  private static String requiredText(final DataCell cell, final String label)
      throws InvalidSettingsException {
    if (cell.isMissing() || cell.toString().isBlank()) {
      throw new InvalidSettingsException("Problem definition value is required: " + label);
    }
    return cell.toString().trim();
  }

  private static void requireUniqueResultColumn(final Set<String> names, final String name)
      throws InvalidSettingsException {
    if (!names.add(name)) {
      throw new InvalidSettingsException("Duplicate evaluation result column: " + name);
    }
  }
}
