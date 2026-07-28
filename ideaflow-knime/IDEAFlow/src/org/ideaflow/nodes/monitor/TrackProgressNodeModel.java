package org.ideaflow.nodes.monitor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.ObjectiveDefinition;
import org.ideaflow.api.OptimizationDirection;
import org.ideaflow.core.FastNonDominatedSort;
import org.ideaflow.core.Hypervolume;
import org.ideaflow.knime.KnimeTableSupport;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.ideaflow.knime.OptimizationSummary;
import org.ideaflow.knime.PopulationState;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/** One beginner-facing monitoring node for scalar statistics, Pareto metrics and event traces. */
final class TrackProgressNodeModel extends NodeModel {
  static final String CFG_STAGE = "stage";
  private final SettingsModelString m_stage = new SettingsModelString(CFG_STAGE, "population");
  private BufferedDataTable m_lastSummary;

  TrackProgressNodeModel() {
    super(1, 3);
  }

  private DataTableSpec eventSpec(final ProblemMetadata.Schema problem) {
    final List<DataColumnSpec> columns =
        new ArrayList<>(
            List.of(
                KnimeTableSupport.stringColumn("Run"),
                KnimeTableSupport.stringColumn("Population"),
                KnimeTableSupport.stringColumn("Individual"),
                KnimeTableSupport.longColumn(PopulationState.NFE),
                KnimeTableSupport.stringColumn("Stage")));
    for (String objective : problem.objectiveNames()) {
      columns.add(KnimeTableSupport.doubleColumn(objective));
    }
    final DataTableSpec raw = new DataTableSpec(columns.toArray(DataColumnSpec[]::new));
    return ProblemMetadata.attach(raw, "Run", problem);
  }

  @Override
  protected DataTableSpec[] configure(final DataTableSpec[] input) throws InvalidSettingsException {
    validate(input[0]);
    final ProblemMetadata.Schema problem = ProblemMetadata.require(input[0]);
    return new DataTableSpec[] {input[0], OptimizationSummary.spec(problem), eventSpec(problem)};
  }

  @Override
  protected BufferedDataTable[] execute(
      final BufferedDataTable[] input, final ExecutionContext execution) throws Exception {
    final DataTableSpec spec = input[0].getDataTableSpec();
    validate(spec);
    final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
    final List<String> names = problem.objectiveNames();
    final int[] objectives = KnimeTableSupport.requireNumericColumns(spec, names);
    final List<OptimizationDirection> directions =
        problem.objectives().stream().map(ProblemMetadata.Objective::direction).toList();
    final List<ObjectiveDefinition> definitions =
        KnimeTableSupport.objectives(names, directions, List.of());
    final double[] reference = reference(problem);
    final int violation = spec.findColumnIndex(PopulationState.CONSTRAINT_VIOLATION);
    final Map<String, List<DataRow>> groups = new LinkedHashMap<>();
    for (DataRow row : input[0])
      groups
          .computeIfAbsent(PopulationState.groupKey(row, spec), ignored -> new ArrayList<>())
          .add(row);
    final BufferedDataContainer summaries =
        execution.createDataContainer(OptimizationSummary.spec(problem));
    final BufferedDataContainer events = execution.createDataContainer(eventSpec(problem));
    int summaryRow = 0;
    int eventRow = 0;
    for (List<DataRow> rows : groups.values()) {
      final List<Candidate> candidates = new ArrayList<>();
      final List<Double> scalar = new ArrayList<>();
      long maxNfe = 0;
      int feasible = 0;
      for (DataRow row : rows) {
        final Candidate candidate =
            KnimeTableSupport.candidate(row, new int[0], objectives, violation, List.of(), names);
        candidates.add(candidate);
        if (candidate.constraintViolation() <= 0) {
          feasible++;
          scalar.add(candidate.objectives()[0]);
        }
        maxNfe = Math.max(maxNfe, PopulationState.nfe(row, spec));
        final DataCell[] event = new DataCell[5 + names.size()];
        event[0] = new StringCell(PopulationState.run(row, spec));
        event[1] = new StringCell(PopulationState.population(row, spec));
        event[2] = new StringCell(PopulationState.individual(row, spec));
        event[3] = new LongCell(PopulationState.nfe(row, spec));
        event[4] = new StringCell(m_stage.getStringValue());
        for (int i = 0; i < names.size(); i++)
          event[5 + i] = new DoubleCell(candidate.objectives()[i]);
        events.addRowToTable(new DefaultRow("Event-" + eventRow++, event));
      }
      final OptimizationDirection primaryDirection = directions.get(0);
      final double best =
          scalar.stream()
              .mapToDouble(Double::doubleValue)
              .reduce(primaryDirection == OptimizationDirection.MINIMIZE ? Math::min : Math::max)
              .orElse(Double.NaN);
      final double worst =
          scalar.stream()
              .mapToDouble(Double::doubleValue)
              .reduce(primaryDirection == OptimizationDirection.MINIMIZE ? Math::max : Math::min)
              .orElse(Double.NaN);
      final double mean =
          scalar.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
      final double variance =
          scalar.stream()
              .mapToDouble(value -> (value - mean) * (value - mean))
              .average()
              .orElse(Double.NaN);
      final List<Integer> front =
          candidates.isEmpty()
              ? List.of()
              : FastNonDominatedSort.sort(candidates, definitions).get(0);
      final List<Candidate> nondominated =
          front.stream()
              .map(candidates::get)
              .filter(candidate -> candidate.constraintViolation() <= 0)
              .toList();
      double hypervolume = Double.NaN;
      if (reference.length == names.size() && !nondominated.isEmpty()) {
        hypervolume = Hypervolume.compute(nondominated, definitions, reference);
      }
      final DataRow first = rows.get(0);
      summaries.addRowToTable(
          new DefaultRow(
              "Summary-" + summaryRow++,
              new OptimizationSummary.Entry(
                      PopulationState.run(first, spec),
                      PopulationState.population(first, spec),
                      maxNfe,
                      rows.size(),
                      feasible,
                      best,
                      mean,
                      worst,
                      Math.sqrt(variance),
                      nondominated.size(),
                      hypervolume,
                      m_stage.getStringValue(),
                      PopulationState.seed(first, spec),
                      problem.problemId())
                  .cells()));
    }
    summaries.close();
    events.close();
    m_lastSummary = summaries.getTable();
    return new BufferedDataTable[] {input[0], m_lastSummary, events.getTable()};
  }

  private static DataCell finite(final double value) {
    return Double.isFinite(value)
        ? new DoubleCell(value)
        : org.knime.core.data.DataType.getMissingCell();
  }

  // Hypervolume is available only when every objective has a configured reference coordinate.
  private static double[] reference(final ProblemMetadata.Schema problem) {
    if (problem.objectives().stream().anyMatch(objective -> objective.referencePoint() == null)) {
      return new double[0];
    }
    return problem.objectives().stream()
        .mapToDouble(ProblemMetadata.Objective::referencePoint)
        .toArray();
  }

  private void validate(final DataTableSpec spec) throws InvalidSettingsException {
    final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
    KnimeTableSupport.requireNumericColumns(spec, problem.objectiveNames());
    PopulationState.requireVisibleColumns(spec);
  }

  private org.knime.core.node.defaultnodesettings.SettingsModel[] models() {
    return new org.knime.core.node.defaultnodesettings.SettingsModel[] {m_stage};
  }

  @Override
  protected void saveSettingsTo(final NodeSettingsWO settings) {
    for (var model : models()) model.saveSettingsTo(settings);
  }

  @Override
  protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
    for (var model : models()) model.validateSettings(settings);
  }

  @Override
  protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
      throws InvalidSettingsException {
    for (var model : models()) model.loadSettingsFrom(settings);
  }

  BufferedDataTable summaryTable() {
    return m_lastSummary;
  }

  @Override
  protected void reset() {
    m_lastSummary = null;
  }

  @Override
  protected void loadInternals(final File directory, final ExecutionMonitor monitor)
      throws IOException, CanceledExecutionException {}

  @Override
  protected void saveInternals(final File directory, final ExecutionMonitor monitor)
      throws IOException, CanceledExecutionException {}
}
