package org.ideaflow.nodes.loop.end;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.ideaflow.api.OptimizationDirection;
import org.ideaflow.core.EvolutionSchedule;
import org.ideaflow.knime.KnimeTableSupport;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.ideaflow.knime.OptimizationSummary;
import org.ideaflow.knime.PopulationState;
import org.ideaflow.nodes.loop.start.OptimizationLoopStartNodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
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
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.LoopEndNode;

/** Recursive loop end with NFE and objective-target stopping. */
public final class OptimizationLoopEndNodeModel extends NodeModel implements LoopEndNode {
  static final String CFG_TARGET_CONDITIONS = "target_conditions";
  static final String CFG_TARGET_RULE = "target_rule";

  private final SettingsModelString m_targetConditions =
      new SettingsModelString(CFG_TARGET_CONDITIONS, "");
  private final SettingsModelString m_targetRule = new SettingsModelString(CFG_TARGET_RULE, "ALL");
  private final List<OptimizationSummary.Entry> m_history = new ArrayList<>();
  private BufferedDataTable m_feedbackPopulation;
  private BufferedDataTable m_feedbackArchive;

  private record Target(
      String objective, int column, double value, OptimizationDirection direction) {}

  OptimizationLoopEndNodeModel() {
    super(
        new PortType[] {
          BufferedDataTable.TYPE, BufferedDataTable.TYPE_OPTIONAL, BufferedDataTable.TYPE_OPTIONAL
        },
        new PortType[] {BufferedDataTable.TYPE, BufferedDataTable.TYPE, BufferedDataTable.TYPE});
  }

  public BufferedDataTable feedbackPopulation() {
    if (m_feedbackPopulation == null)
      throw new IllegalStateException("The loop has no feedback population yet.");
    return m_feedbackPopulation;
  }

  public BufferedDataTable feedbackArchive() {
    if (m_feedbackArchive == null)
      throw new IllegalStateException("The loop has no feedback archive yet.");
    return m_feedbackArchive;
  }

  private static DataTableSpec summarySpec(final ProblemMetadata.Schema problem) {
    final DataTableSpec raw =
        new DataTableSpec(
            KnimeTableSupport.stringColumn("Run"),
            KnimeTableSupport.stringColumn("Population"),
            KnimeTableSupport.longColumn(PopulationState.NFE),
            KnimeTableSupport.stringColumn("Stopped"),
            KnimeTableSupport.stringColumn("Stop reason"),
            KnimeTableSupport.doubleColumn("Best objective"));
    return ProblemMetadata.attach(raw, "Run", problem);
  }

  @Override
  protected DataTableSpec[] configure(final DataTableSpec[] input) throws InvalidSettingsException {
    validate(input[0]);
    final ProblemMetadata.Schema problem = ProblemMetadata.require(input[0]);
    if (input[1] != null)
      KnimeTableSupport.requireCompatibleSchema(input[0], input[1], "Replaced DE parents");
    if (input[2] != null) OptimizationSummary.validate(input[2]);
    return new DataTableSpec[] {input[0], summarySpec(problem), OptimizationSummary.spec(problem)};
  }

  @Override
  protected BufferedDataTable[] execute(
      final BufferedDataTable[] input, final ExecutionContext execution) throws Exception {
    if (!(getLoopStartNode() instanceof OptimizationLoopStartNodeModel start)) {
      throw new IllegalStateException(
          "Connect this node to an IdeaFlow Evolution Loop Start node.");
    }
    final DataTableSpec spec = input[0].getDataTableSpec();
    validate(spec);
    final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
    if (input[1] != null)
      KnimeTableSupport.requireCompatibleSchema(
          spec, input[1].getDataTableSpec(), "Replaced DE parents");
    if (input[2] != null) appendHistory(input[2]);
    final ProblemMetadata.Objective stoppingObjective = problem.objectives().get(0);
    final OptimizationDirection direction = stoppingObjective.direction();
    final List<Target> targets = targets(spec, problem);
    final int objective = spec.findColumnIndex(stoppingObjective.column());
    final int violation = spec.findColumnIndex(PopulationState.CONSTRAINT_VIOLATION);
    final Map<String, List<DataRow>> groups = new LinkedHashMap<>();
    final Map<String, Integer> nextGenerationSize = new LinkedHashMap<>();
    final Map<String, Long> runNfe = new LinkedHashMap<>();
    for (DataRow row : input[0]) {
      groups
          .computeIfAbsent(PopulationState.groupKey(row, spec), ignored -> new ArrayList<>())
          .add(row);
      final String run = PopulationState.run(row, spec);
      nextGenerationSize.merge(run, 1, Integer::sum);
      runNfe.merge(run, PopulationState.nfe(row, spec), Math::max);
    }

    final BufferedDataContainer feedback = execution.createDataContainer(spec, true);
    final BufferedDataContainer summary = execution.createDataContainer(summarySpec(problem));
    boolean allStopped = !groups.isEmpty();
    int feedbackRow = 0;
    int summaryRow = 0;
    for (List<DataRow> rows : groups.values()) {
      long currentNfe = 0;
      double best =
          direction == OptimizationDirection.MINIMIZE
              ? Double.POSITIVE_INFINITY
              : Double.NEGATIVE_INFINITY;
      for (DataRow row : rows) {
        currentNfe = Math.max(currentNfe, PopulationState.nfe(row, spec));
        if (!row.getCell(objective).isMissing()) {
          final double value = ((DoubleValue) row.getCell(objective)).getDoubleValue();
          best =
              direction == OptimizationDirection.MINIMIZE
                  ? Math.min(best, value)
                  : Math.max(best, value);
        }
      }
      final DataRow first = rows.get(0);
      final String run = PopulationState.run(first, spec);
      String reason = "continue";
      boolean stopped = false;
      if (currentNfe >= problem.maxEvaluations()) {
        stopped = true;
        reason = "maximum evaluations";
      } else if (!EvolutionSchedule.canEvaluateBatch(
          runNfe.get(run), nextGenerationSize.get(run), problem.maxEvaluations())) {
        stopped = true;
        reason = "remaining evaluation budget is smaller than one complete generation";
      } else if (targetsReached(rows, targets, violation, m_targetRule.getStringValue())) {
        stopped = true;
        reason = "objective target reached (" + m_targetRule.getStringValue().toLowerCase() + ")";
      }
      allStopped &= stopped;
      for (DataRow row : rows) {
        feedback.addRowToTable(new DefaultRow("Next-" + feedbackRow++, row));
      }
      summary.addRowToTable(
          new DefaultRow(
              "Run-" + summaryRow++,
              new DataCell[] {
                new StringCell(PopulationState.run(first, spec)),
                new StringCell(PopulationState.population(first, spec)),
                new LongCell(currentNfe),
                new StringCell(Boolean.toString(stopped)),
                new StringCell(reason),
                Double.isFinite(best)
                    ? new DoubleCell(best)
                    : org.knime.core.data.DataType.getMissingCell()
              }));
    }
    feedback.close();
    summary.close();
    m_feedbackPopulation = feedback.getTable();
    m_feedbackArchive = updateArchive(start.currentArchive(), input[1], input[0], spec, execution);
    if (allStopped) {
      final BufferedDataContainer history =
          execution.createDataContainer(OptimizationSummary.spec(problem));
      int historyRow = 0;
      for (OptimizationSummary.Entry entry : m_history) {
        history.addRowToTable(new DefaultRow("History-" + historyRow++, entry.cells()));
        execution.checkCanceled();
      }
      history.close();
      return new BufferedDataTable[] {m_feedbackPopulation, summary.getTable(), history.getTable()};
    }
    super.continueLoop();
    return new BufferedDataTable[3];
  }

  // The loop owns a fresh archive table so feedback does not retain an upstream table instance.
  private static BufferedDataTable updateArchive(
      final BufferedDataTable existing,
      final BufferedDataTable replaced,
      final BufferedDataTable population,
      final DataTableSpec spec,
      final ExecutionContext execution)
      throws InvalidSettingsException {
    final Map<String, Integer> limits = new LinkedHashMap<>();
    for (DataRow row : population) {
      limits.merge(PopulationState.groupKey(row, spec), 1, Integer::sum);
    }

    final Map<String, LinkedHashMap<String, DataRow>> groups = new LinkedHashMap<>();
    addArchiveRows(existing, spec, groups);
    addArchiveRows(replaced, spec, groups);

    final BufferedDataContainer output = execution.createDataContainer(spec, true);
    int rowNumber = 0;
    for (Map.Entry<String, Integer> limit : limits.entrySet()) {
      final List<DataRow> rows =
          new ArrayList<>(groups.getOrDefault(limit.getKey(), new LinkedHashMap<>()).values());
      final int first = Math.max(0, rows.size() - limit.getValue());
      for (int index = first; index < rows.size(); index++) {
        output.addRowToTable(new DefaultRow("Archive-" + rowNumber++, rows.get(index)));
      }
    }
    output.close();
    return output.getTable();
  }

  private static void addArchiveRows(
      final BufferedDataTable table,
      final DataTableSpec spec,
      final Map<String, LinkedHashMap<String, DataRow>> groups)
      throws InvalidSettingsException {
    if (table == null) return;
    for (DataRow row : table) {
      final String group = PopulationState.groupKey(row, spec);
      final String individual = PopulationState.individual(row, spec);
      final LinkedHashMap<String, DataRow> unique =
          groups.computeIfAbsent(group, ignored -> new LinkedHashMap<>());
      unique.remove(individual);
      unique.put(individual, row);
    }
  }

  // Progress summaries are optional and remain separate from stopping decisions.
  private void appendHistory(final BufferedDataTable table) throws InvalidSettingsException {
    final DataTableSpec spec = table.getDataTableSpec();
    OptimizationSummary.validate(spec);
    for (DataRow row : table) m_history.add(OptimizationSummary.read(row, spec));
  }

  private void validate(final DataTableSpec spec) throws InvalidSettingsException {
    PopulationState.requireVisibleColumns(spec);
    final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
    KnimeTableSupport.requireNumericColumns(spec, problem.objectiveNames());
    targets(spec, problem);
    final String rule = m_targetRule.getStringValue();
    if (!"ALL".equals(rule) && !"ANY".equals(rule)) {
      throw new InvalidSettingsException("Target rule must be ALL or ANY.");
    }
  }

  // Resolve encoded target settings against current problem metadata during validation and
  // execution.
  private List<Target> targets(final DataTableSpec spec, final ProblemMetadata.Schema problem)
      throws InvalidSettingsException {
    final List<Target> result = new ArrayList<>();
    final java.util.Set<String> seen = new java.util.HashSet<>();
    final String encoded = m_targetConditions.getStringValue();
    if (encoded == null || encoded.isBlank()) return result;
    for (String item : encoded.split(",")) {
      try {
        final int separator = item.indexOf(':');
        if (separator < 1) throw new IllegalArgumentException();
        final String objective =
            new String(
                java.util.Base64.getUrlDecoder().decode(item.substring(0, separator)),
                java.nio.charset.StandardCharsets.UTF_8);
        final double value = Double.parseDouble(item.substring(separator + 1));
        if (!Double.isFinite(value)) throw new IllegalArgumentException();
        final ProblemMetadata.Objective definition =
            problem.objectives().stream()
                .filter(candidate -> candidate.column().equals(objective))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
        if (!seen.add(objective)) {
          throw new InvalidSettingsException(
              "Objective target is configured more than once: " + objective);
        }
        final int column = KnimeTableSupport.requireNumericColumns(spec, List.of(objective))[0];
        result.add(new Target(objective, column, value, definition.direction()));
      } catch (InvalidSettingsException exception) {
        throw exception;
      } catch (IllegalArgumentException exception) {
        throw new InvalidSettingsException("Invalid objective target setting: " + item, exception);
      }
    }
    return result;
  }

  private static boolean targetsReached(
      final List<DataRow> rows,
      final List<Target> targets,
      final int violation,
      final String rule) {
    if (targets.isEmpty()) return false;
    for (DataRow row : rows) {
      if (violation >= 0
          && (row.getCell(violation).isMissing()
              || ((DoubleValue) row.getCell(violation)).getDoubleValue() > 0.0)) continue;
      boolean allForRow = true;
      for (Target target : targets) {
        final DataCell cell = row.getCell(target.column());
        final boolean reached =
            !cell.isMissing()
                && (target.direction() == OptimizationDirection.MINIMIZE
                    ? ((DoubleValue) cell).getDoubleValue() <= target.value()
                    : ((DoubleValue) cell).getDoubleValue() >= target.value());
        if ("ANY".equals(rule) && reached) return true;
        allForRow &= reached;
      }
      if ("ALL".equals(rule) && allForRow) return true;
    }
    return false;
  }

  private org.knime.core.node.defaultnodesettings.SettingsModel[] models() {
    return new org.knime.core.node.defaultnodesettings.SettingsModel[] {
      m_targetConditions, m_targetRule
    };
  }

  @Override
  protected void saveSettingsTo(final NodeSettingsWO settings) {
    for (var model : models()) model.saveSettingsTo(settings);
  }

  @Override
  protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
    m_targetConditions.validateSettings(settings);
    m_targetRule.validateSettings(settings);
  }

  @Override
  protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
      throws InvalidSettingsException {
    m_targetConditions.loadSettingsFrom(settings);
    m_targetRule.loadSettingsFrom(settings);
  }

  @Override
  protected void reset() {
    m_feedbackPopulation = null;
    m_feedbackArchive = null;
    m_history.clear();
  }

  @Override
  protected void loadInternals(final File directory, final ExecutionMonitor monitor)
      throws IOException, CanceledExecutionException {}

  @Override
  protected void saveInternals(final File directory, final ExecutionMonitor monitor)
      throws IOException, CanceledExecutionException {}
}
