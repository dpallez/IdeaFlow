package org.ideaflow.nodes.variation.recombination;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import org.ideaflow.api.IdeaFlowState;
import org.ideaflow.api.ReservedColumns;
import org.ideaflow.core.DeterministicRandom;
import org.ideaflow.knime.KnimeTableSupport;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.ideaflow.knime.PopulationState;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/** Applies the configured crossover operator while preserving IdeaFlow row state. */
public final class CrossoverNodeModel extends NodeModel {
  static final String CFG_STRATEGY = "strategy";
  static final String CFG_PROBABILITY = "probability";
  static final String CFG_ETA = "distribution_index";

  private static final List<String> DE_STRATEGIES = List.of("DE_BINOMIAL", "DE_EXPONENTIAL");

  private final SettingsModelString m_strategy = new SettingsModelString(CFG_STRATEGY, "SBX");
  private final SettingsModelDoubleBounded m_probability =
      new SettingsModelDoubleBounded(CFG_PROBABILITY, 0.9, 0, 1);
  private final SettingsModelDoubleBounded m_eta =
      new SettingsModelDoubleBounded(CFG_ETA, 20, 0.01, 10000);

  CrossoverNodeModel() {
    super(1, 1);
  }

  @Override
  protected DataTableSpec[] configure(final DataTableSpec[] input) throws InvalidSettingsException {
    validate(input[0]);
    return new DataTableSpec[] {input[0]};
  }

  @Override
  protected BufferedDataTable[] execute(
      final BufferedDataTable[] input, final ExecutionContext execution) throws Exception {
    final DataTableSpec spec = input[0].getDataTableSpec();
    validate(spec);
    return new BufferedDataTable[] {
      isDifferential()
          ? differentialCrossover(input[0], execution)
          : ordinaryCrossover(input[0], execution)
    };
  }

  // Ordinary crossover pairs rows within each run and population group.
  private BufferedDataTable ordinaryCrossover(
      final BufferedDataTable input, final ExecutionContext execution) throws Exception {
    final DataTableSpec spec = input.getDataTableSpec();
    final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
    final VariableMetadata variables = variables(problem, false);
    final int[] variableIndices = KnimeTableSupport.requireNumericColumns(spec, variables.names());
    final BufferedDataContainer output = execution.createDataContainer(spec);
    int childNumber = 0;
    for (List<DataRow> group : groups(input).values()) {
      final RandomGenerator random = random(group.get(0), spec, "crossover");
      for (int pair = 0; pair < group.size(); pair += 2) {
        final DataRow left = group.get(pair);
        final DataRow right = group.get((pair + 1) % group.size());
        final double[] leftValues = values(left, variableIndices, variables.names());
        final double[] rightValues = values(right, variableIndices, variables.names());
        final double[] firstChild = leftValues.clone();
        final double[] secondChild = rightValues.clone();
        if (random.nextDouble() < m_probability.getDoubleValue()) {
          recombine(
              leftValues,
              rightValues,
              firstChild,
              secondChild,
              variables.lows(),
              variables.highs(),
              random);
        }
        childNumber =
            emitOrdinary(
                output, spec, problem, left, right, firstChild, variableIndices, childNumber);
        if (pair + 1 < group.size()) {
          childNumber =
              emitOrdinary(
                  output, spec, problem, right, left, secondChild, variableIndices, childNumber);
        }
      }
    }
    output.close();
    return output.getTable();
  }

  // DE crossover combines the target vector with the mutant vector stored by Mutation.
  private BufferedDataTable differentialCrossover(
      final BufferedDataTable input, final ExecutionContext execution) throws Exception {
    final DataTableSpec inputSpec = input.getDataTableSpec();
    final DataTableSpec outputSpec = inputSpec;
    final ProblemMetadata.Schema problem = ProblemMetadata.require(inputSpec);
    final VariableMetadata variables = variables(problem, true);
    final int[] inputVariableIndices =
        KnimeTableSupport.requireNumericColumns(inputSpec, variables.names());
    final int[] outputVariableIndices =
        KnimeTableSupport.requireNumericColumns(outputSpec, variables.names());
    final BufferedDataContainer output = execution.createDataContainer(outputSpec);
    int trialNumber = 0;
    for (List<DataRow> group : groups(input).values()) {
      final RandomGenerator random = random(group.get(0), inputSpec, "de-crossover");
      for (DataRow row : group) {
        IdeaFlowState state = PopulationState.get(row, inputSpec);
        final double[] target = state.vector(IdeaFlowState.DE_TARGET_VECTOR);
        if (target.length == 0) {
          throw new InvalidSettingsException(
              "DE crossover requires Selection followed by differential Mutation.");
        }
        final double[] mutant = values(row, inputVariableIndices, variables.names());
        final double crossoverRate = state.doubleValue(IdeaFlowState.DE_CR, Double.NaN);
        if (!Double.isFinite(crossoverRate) || crossoverRate < 0.0 || crossoverRate > 1.0) {
          throw new InvalidSettingsException(
              "DE crossover requires a valid CR produced by differential Mutation.");
        }
        final double[] trial = differentialCrossover(target, mutant, crossoverRate, random);
        final DataCell[] cells = KnimeTableSupport.copyToSpec(row, inputSpec, outputSpec);
        for (int dimension = 0; dimension < trial.length; dimension++) {
          cells[outputVariableIndices[dimension]] =
              numericCell(
                  outputSpec.getColumnSpec(outputVariableIndices[dimension]).getType(),
                  trial[dimension]);
        }
        invalidate(cells, outputSpec, evaluationResults(problem));
        final String targetId = state.text(IdeaFlowState.INDIVIDUAL, row.getKey().getString());
        state =
            state
                .with(IdeaFlowState.PARENTS, targetId)
                .with(IdeaFlowState.INDIVIDUAL, targetId + ":trial")
                .with(IdeaFlowState.DE_SUCCESS, false)
                .without(IdeaFlowState.DE_IMPROVEMENT)
                .with(IdeaFlowState.EVALUATED, false)
                .without(IdeaFlowState.EVALUATION)
                .without(
                    IdeaFlowState.DE_TARGET_VECTOR,
                    IdeaFlowState.DE_RANDOM_BASE_VECTOR,
                    IdeaFlowState.DE_DIFFERENCE_1_VECTOR,
                    IdeaFlowState.DE_DIFFERENCE_2_VECTOR,
                    IdeaFlowState.DE_BEST_VECTOR,
                    IdeaFlowState.DE_PBEST_VECTOR,
                    IdeaFlowState.DE_PBEST_DIFFERENCE_2_VECTOR);
        PopulationState.set(cells, outputSpec, state);
        output.addRowToTable(new DefaultRow("DETrial" + trialNumber++, cells));
        execution.checkCanceled();
      }
    }
    output.close();
    return output.getTable();
  }

  private double[] differentialCrossover(
      final double[] target,
      final double[] mutant,
      final double crossoverRate,
      final RandomGenerator random) {
    final double[] trial = target.clone();
    final int forced = random.nextInt(target.length);
    if ("DE_BINOMIAL".equals(m_strategy.getStringValue())) {
      for (int dimension = 0; dimension < target.length; dimension++) {
        if (dimension == forced || random.nextDouble() <= crossoverRate) {
          trial[dimension] = mutant[dimension];
        }
      }
    } else {
      int length = 0;
      int dimension = forced;
      do {
        trial[dimension] = mutant[dimension];
        dimension = (dimension + 1) % target.length;
        length++;
      } while (length < target.length && random.nextDouble() <= crossoverRate);
    }
    return trial;
  }

  private void recombine(
      final double[] left,
      final double[] right,
      final double[] firstChild,
      final double[] secondChild,
      final double[] lows,
      final double[] highs,
      final RandomGenerator random) {
    switch (m_strategy.getStringValue()) {
      case "ONE_POINT" -> {
        if (left.length > 1) {
          final int cut = 1 + random.nextInt(left.length - 1);
          for (int index = cut; index < left.length; index++) {
            firstChild[index] = right[index];
            secondChild[index] = left[index];
          }
        }
      }
      case "UNIFORM" -> {
        for (int index = 0; index < left.length; index++) {
          if (random.nextBoolean()) {
            firstChild[index] = right[index];
            secondChild[index] = left[index];
          }
        }
      }
      case "ARITHMETIC" -> {
        final double alpha = random.nextDouble();
        for (int index = 0; index < left.length; index++) {
          firstChild[index] = alpha * left[index] + (1 - alpha) * right[index];
          secondChild[index] = alpha * right[index] + (1 - alpha) * left[index];
        }
      }
      case "SBX" -> {
        for (int index = 0; index < left.length; index++) {
          if (random.nextBoolean() && Math.abs(left[index] - right[index]) > 1e-14) {
            final double x1 = Math.min(left[index], right[index]);
            final double x2 = Math.max(left[index], right[index]);
            final double sample = random.nextDouble();
            double beta = 1 + 2 * (x1 - lows[index]) / (x2 - x1);
            double alpha = 2 - Math.pow(beta, -(m_eta.getDoubleValue() + 1));
            final double betaQ =
                sample <= 1 / alpha
                    ? Math.pow(sample * alpha, 1 / (m_eta.getDoubleValue() + 1))
                    : Math.pow(1 / (2 - sample * alpha), 1 / (m_eta.getDoubleValue() + 1));
            double y1 = 0.5 * ((x1 + x2) - betaQ * (x2 - x1));
            beta = 1 + 2 * (highs[index] - x2) / (x2 - x1);
            alpha = 2 - Math.pow(beta, -(m_eta.getDoubleValue() + 1));
            final double betaQ2 =
                sample <= 1 / alpha
                    ? Math.pow(sample * alpha, 1 / (m_eta.getDoubleValue() + 1))
                    : Math.pow(1 / (2 - sample * alpha), 1 / (m_eta.getDoubleValue() + 1));
            double y2 = 0.5 * ((x1 + x2) + betaQ2 * (x2 - x1));
            y1 = Math.max(lows[index], Math.min(highs[index], y1));
            y2 = Math.max(lows[index], Math.min(highs[index], y2));
            if (random.nextBoolean()) {
              firstChild[index] = y2;
              secondChild[index] = y1;
            } else {
              firstChild[index] = y1;
              secondChild[index] = y2;
            }
          }
        }
      }
      default -> throw new IllegalStateException("Unsupported crossover strategy");
    }
  }

  private int emitOrdinary(
      final BufferedDataContainer output,
      final DataTableSpec spec,
      final ProblemMetadata.Schema problem,
      final DataRow primary,
      final DataRow secondary,
      final double[] child,
      final int[] variableIndices,
      final int number) {
    final DataCell[] cells = KnimeTableSupport.copyToSpec(primary, spec, spec);
    for (int index = 0; index < variableIndices.length; index++) {
      cells[variableIndices[index]] =
          numericCell(spec.getColumnSpec(variableIndices[index]).getType(), child[index]);
    }
    invalidate(cells, spec, evaluationResults(problem));
    try {
      final IdeaFlowState primaryState = PopulationState.get(primary, spec);
      final IdeaFlowState secondaryState = PopulationState.get(secondary, spec);
      final String left = primaryState.text(IdeaFlowState.INDIVIDUAL, primary.getKey().getString());
      final String right =
          secondaryState.text(IdeaFlowState.INDIVIDUAL, secondary.getKey().getString());
      PopulationState.set(
          cells,
          spec,
          primaryState
              .with(IdeaFlowState.INDIVIDUAL, "child-" + number)
              .with(IdeaFlowState.PARENTS, left + "," + right)
              .with(IdeaFlowState.EVALUATED, false)
              .without(IdeaFlowState.EVALUATION));
    } catch (InvalidSettingsException exception) {
      throw new IllegalStateException(exception);
    }
    output.addRowToTable(new DefaultRow("Child" + number, cells));
    return number + 1;
  }

  // A changed decision variable invalidates every result derived from the old candidate.
  public static void invalidate(
      final DataCell[] cells, final DataTableSpec spec, final List<String> evaluationResults) {
    for (String name : evaluationResults) {
      final int index = spec.findColumnIndex(name);
      if (index >= 0) cells[index] = DataType.getMissingCell();
    }
    for (String name :
        List.of(
            ReservedColumns.PARETO_RANK,
            ReservedColumns.CROWDING_DISTANCE,
            PopulationState.CONSTRAINT_VIOLATION,
            PopulationState.FEASIBLE)) {
      final int index = spec.findColumnIndex(name);
      if (index >= 0) cells[index] = DataType.getMissingCell();
    }
    final int stateIndex = spec.findColumnIndex(PopulationState.COLUMN);
    if (stateIndex >= 0
        && cells[stateIndex] instanceof org.ideaflow.api.IdeaFlowStateCell stateCell) {
      cells[stateIndex] =
          new org.ideaflow.api.IdeaFlowStateCell(
              stateCell
                  .state()
                  .with(IdeaFlowState.EVALUATED, false)
                  .without(IdeaFlowState.EVALUATION));
    }
  }

  public static DataCell numericCell(final DataType type, final double value) {
    if (type.isCompatible(IntValue.class)) return new IntCell((int) Math.round(value));
    if (type.isCompatible(LongValue.class)) return new LongCell(Math.round(value));
    return new DoubleCell(value);
  }

  private static List<String> evaluationResults(final ProblemMetadata.Schema problem) {
    final List<String> results = new ArrayList<>(problem.objectiveNames());
    problem.constraints().forEach(constraint -> results.add(constraint.column()));
    return results;
  }

  private static double[] values(final DataRow row, final int[] indices, final List<String> names)
      throws InvalidSettingsException {
    final double[] result = new double[indices.length];
    for (int index = 0; index < indices.length; index++) {
      result[index] = KnimeTableSupport.number(row.getCell(indices[index]), row, names.get(index));
    }
    return result;
  }

  private record VariableMetadata(List<String> names, double[] lows, double[] highs) {}

  private static VariableMetadata variables(
      final ProblemMetadata.Schema problem, final boolean directFloatOnly)
      throws InvalidSettingsException {
    final List<String> names = new ArrayList<>();
    final List<Double> lows = new ArrayList<>();
    final List<Double> highs = new ArrayList<>();
    for (ProblemMetadata.Variable variable : problem.variables()) {
      if (directFloatOnly && (variable.encoded() || !"REAL".equalsIgnoreCase(variable.type())))
        continue;
      for (String column : variable.populationColumns()) {
        names.add(column);
        lows.add(variable.encoded() ? 0.0 : variable.lower());
        highs.add(variable.encoded() ? 1.0 : variable.upper());
      }
    }
    if (names.isEmpty()) {
      throw new InvalidSettingsException(
          "Differential crossover requires direct Float variables in Problem Setup.");
    }
    final double[] lower = new double[lows.size()];
    final double[] upper = new double[highs.size()];
    for (int index = 0; index < lower.length; index++) {
      lower[index] = lows.get(index);
      upper[index] = highs.get(index);
    }
    return new VariableMetadata(List.copyOf(names), lower, upper);
  }

  private static Map<String, List<DataRow>> groups(final BufferedDataTable table)
      throws InvalidSettingsException {
    final DataTableSpec spec = table.getDataTableSpec();
    final Map<String, List<DataRow>> groups = new LinkedHashMap<>();
    for (DataRow row : table) {
      groups
          .computeIfAbsent(PopulationState.groupKey(row, spec), ignored -> new ArrayList<>())
          .add(row);
    }
    return groups;
  }

  private static RandomGenerator random(
      final DataRow row, final DataTableSpec spec, final String scope)
      throws InvalidSettingsException {
    return DeterministicRandom.forScope(
        PopulationState.seed(row, spec),
        PopulationState.run(row, spec),
        PopulationState.population(row, spec),
        PopulationState.nfe(row, spec),
        scope);
  }

  private boolean isDifferential() {
    return DE_STRATEGIES.contains(m_strategy.getStringValue());
  }

  private void validate(final DataTableSpec spec) throws InvalidSettingsException {
    final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
    final VariableMetadata metadata = variables(problem, isDifferential());
    KnimeTableSupport.requireNumericColumns(spec, metadata.names());
    for (String objective : problem.objectiveNames()) {
      if (spec.findColumnIndex(objective) < 0) {
        throw new InvalidSettingsException("Missing objective column: " + objective);
      }
    }
    if (!List.of("SBX", "UNIFORM", "ONE_POINT", "ARITHMETIC", "DE_BINOMIAL", "DE_EXPONENTIAL")
        .contains(m_strategy.getStringValue())) {
      throw new InvalidSettingsException("Unsupported crossover strategy.");
    }
    if (isDifferential()) PopulationState.requireVisibleColumns(spec);
    for (int index = 0; index < metadata.lows().length; index++) {
      if (metadata.lows()[index] >= metadata.highs()[index]) {
        throw new InvalidSettingsException("Invalid bounds for " + metadata.names().get(index));
      }
    }
    if ("SBX".equals(m_strategy.getStringValue())
        || "ARITHMETIC".equals(m_strategy.getStringValue())
        || isDifferential()) {
      for (String name : metadata.names()) {
        if (!spec.getColumnSpec(name).getType().isCompatible(DoubleValue.class)) {
          throw new InvalidSettingsException(
              m_strategy.getStringValue() + " requires Float variables.");
        }
      }
    }
    PopulationState.requireVisibleColumns(spec);
  }

  private SettingsModel[] models() {
    return new SettingsModel[] {m_strategy, m_probability, m_eta};
  }

  @Override
  protected void saveSettingsTo(final NodeSettingsWO settings) {
    for (SettingsModel model : models()) model.saveSettingsTo(settings);
  }

  @Override
  protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
    m_strategy.validateSettings(settings);
    m_probability.validateSettings(settings);
    m_eta.validateSettings(settings);
  }

  @Override
  protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
      throws InvalidSettingsException {
    m_strategy.loadSettingsFrom(settings);
    m_probability.loadSettingsFrom(settings);
    m_eta.loadSettingsFrom(settings);
  }

  @Override
  protected void loadInternals(final File directory, final ExecutionMonitor monitor)
      throws IOException, CanceledExecutionException {}

  @Override
  protected void saveInternals(final File directory, final ExecutionMonitor monitor)
      throws IOException, CanceledExecutionException {}

  @Override
  protected void reset() {}
}
