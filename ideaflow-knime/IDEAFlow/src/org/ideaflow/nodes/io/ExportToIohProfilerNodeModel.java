package org.ideaflow.nodes.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.ideaflow.api.OptimizationDirection;
import org.ideaflow.io.IohProfilerWriter;
import org.ideaflow.knime.KnimeTableSupport;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.ideaflow.knime.PopulationState;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.LongValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/** IOH export with problem name, dimension and performance direction derived from Problem Setup. */
public final class ExportToIohProfilerNodeModel extends NodeModel {
  static final String CFG_OUTPUT = "output_directory";
  static final String CFG_FOLDER = "folder_name";
  static final String CFG_SUITE = "suite";
  static final String CFG_PROBLEM_ID = "problem_id";
  static final String CFG_ALGORITHM = "algorithm";
  static final String CFG_INFO = "algorithm_info";
  static final String CFG_RAW_Y = "raw_y_column";
  static final String CFG_INSTANCE = "instance";
  static final String CFG_COMPLETE = "write_complete";
  static final String CFG_PROPERTIES = "additional_properties";
  static final String CFG_DIRECTION = "performance_direction";

  private final SettingsModelString m_output = new SettingsModelString(CFG_OUTPUT, "ioh-output");
  private final SettingsModelString m_folder = new SettingsModelString(CFG_FOLDER, "ioh_data");
  private final SettingsModelString m_suite = new SettingsModelString(CFG_SUITE, "unknown_suite");
  private final SettingsModelString m_problemId = new SettingsModelString(CFG_PROBLEM_ID, "1");
  private final SettingsModelString m_algorithm =
      new SettingsModelString(CFG_ALGORITHM, "IdeaFlow");
  private final SettingsModelString m_info = new SettingsModelString(CFG_INFO, "");
  private final SettingsModelString m_rawY = new SettingsModelString(CFG_RAW_Y, "");
  private final SettingsModelString m_properties = new SettingsModelString(CFG_PROPERTIES, "");
  private final SettingsModelIntegerBounded m_instance =
      new SettingsModelIntegerBounded(CFG_INSTANCE, 1, 1, Integer.MAX_VALUE);
  private final SettingsModelBoolean m_complete = new SettingsModelBoolean(CFG_COMPLETE, true);
  private final SettingsModelString m_direction =
      new SettingsModelString(CFG_DIRECTION, "AUTO");

  ExportToIohProfilerNodeModel() {
    super(1, 1);
  }

  private static DataTableSpec outSpec() {
    return new DataTableSpec(
        KnimeTableSupport.stringColumn("Output path"),
        KnimeTableSupport.stringColumn("Performance column"),
        KnimeTableSupport.stringColumn("Direction"),
        KnimeTableSupport.intColumn("Run count"),
        KnimeTableSupport.intColumn("Event count"),
        KnimeTableSupport.longColumn("Minimum NFE"),
        KnimeTableSupport.longColumn("Maximum NFE"),
        KnimeTableSupport.stringColumn("Format"));
  }

  @Override
  protected DataTableSpec[] configure(final DataTableSpec[] input) throws InvalidSettingsException {
    validate(input[0]);
    return new DataTableSpec[] {outSpec()};
  }

  @Override
  protected BufferedDataTable[] execute(
      final BufferedDataTable[] input, final ExecutionContext execution) throws Exception {
    final DataTableSpec spec = input[0].getDataTableSpec();
    validate(spec);
    final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
    final String performance = performanceColumn(spec, problem);
    final int run = spec.findColumnIndex("Run");
    final int nfe = spec.findColumnIndex(PopulationState.NFE);
    final int raw = spec.findColumnIndex(performance);
    final List<String> propertyNames = KnimeTableSupport.names(m_properties.getStringValue());
    final int[] propertyIndices =
        propertyNames.isEmpty() ? new int[0] : indices(spec, propertyNames);
    final List<IohProfilerWriter.Record> records = new ArrayList<>();
    for (DataRow row : input[0]) {
      if (row.getCell(run).isMissing() || row.getCell(run).toString().isBlank()) {
        throw new InvalidSettingsException("Run must not be missing at row " + row.getKey() + ".");
      }
      if (row.getCell(nfe).isMissing() || !(row.getCell(nfe) instanceof LongValue)) {
        throw new InvalidSettingsException(
            "NFE must contain non-missing long integers; invalid row: " + row.getKey() + ".");
      }
      final String runId = row.getCell(run).toString();
      final long evaluations = ((LongValue) row.getCell(nfe)).getLongValue();
      final Map<String, String> attributes = new LinkedHashMap<>();
      for (int index = 0; index < propertyIndices.length; index++) {
        attributes.put(propertyNames.get(index), row.getCell(propertyIndices[index]).toString());
      }
      records.add(
          new IohProfilerWriter.Record(
              runId,
              m_instance.getIntValue(),
              evaluations,
              KnimeTableSupport.number(row.getCell(raw), row, performance),
              attributes));
      execution.checkCanceled();
    }
    final boolean maximize = maximization(problem, performance);
    final IohProfilerWriter.Metadata metadata =
        new IohProfilerWriter.Metadata(
            m_suite.getStringValue(),
            m_problemId.getStringValue(),
            problem.problemId(),
            problem.variables().size(),
            m_algorithm.getStringValue(),
            m_info.getStringValue(),
            maximize);
    final Path path =
        IohProfilerWriter.write(
            Path.of(m_output.getStringValue()).toAbsolutePath().normalize(),
            m_folder.getStringValue(),
            metadata,
            records,
            m_complete.getBooleanValue());
    final BufferedDataContainer output = execution.createDataContainer(outSpec());
    final int runCount =
        (int) records.stream().map(IohProfilerWriter.Record::runId).distinct().count();
    final long minimumNfe =
        records.stream().mapToLong(IohProfilerWriter.Record::evaluations).min().orElseThrow();
    final long maximumNfe =
        records.stream().mapToLong(IohProfilerWriter.Record::evaluations).max().orElseThrow();
    output.addRowToTable(
        new DefaultRow(
            "IOHExport",
            new DataCell[] {
              new StringCell(path.toString()),
              new StringCell(performance),
              new StringCell(maximize ? "MAXIMIZE" : "MINIMIZE"),
              new IntCell(runCount),
              new IntCell(records.size()),
              new LongCell(minimumNfe),
              new LongCell(maximumNfe),
              new StringCell(IohProfilerWriter.FORMAT_LABEL)
            }));
    output.close();
    return new BufferedDataTable[] {output.getTable()};
  }

  // Automatic selection only succeeds when the intended scalar measure is unambiguous.
  private String performanceColumn(final DataTableSpec spec, final ProblemMetadata.Schema problem)
      throws InvalidSettingsException {
    final String selected = m_rawY.getStringValue();
    if (!selected.isBlank()) {
      if (spec.findColumnIndex(selected) < 0) {
        throw new InvalidSettingsException("Selected performance column is missing: " + selected);
      }
      KnimeTableSupport.requireNumericColumns(spec, List.of(selected));
      return selected;
    }
    if (problem.objectives().size() == 1) {
      final String objective = problem.objectives().get(0).column();
      if (spec.findColumnIndex(objective) >= 0) return objective;
    }
    if (problem.objectives().size() == 1 && spec.findColumnIndex("Best") >= 0) return "Best";
    if (spec.findColumnIndex("Hypervolume") >= 0) return "Hypervolume";
    throw new InvalidSettingsException(
        "Select a scalar performance column. No objective, Best, or Hypervolume column could be selected automatically.");
  }

  private boolean maximization(final ProblemMetadata.Schema problem, final String performance)
      throws InvalidSettingsException {
    if ("MAXIMIZE".equals(m_direction.getStringValue())) return true;
    if ("MINIMIZE".equals(m_direction.getStringValue())) return false;
    if ("hypervolume".equalsIgnoreCase(performance)) return true;
    final var objective =
        problem.objectives().stream()
        .filter(candidate -> candidate.column().equals(performance))
        .findFirst();
    if (objective.isPresent()) {
      return objective.get().direction() == OptimizationDirection.MAXIMIZE;
    }
    if ("Best".equals(performance) && problem.objectives().size() == 1) {
      return problem.objectives().get(0).direction() == OptimizationDirection.MAXIMIZE;
    }
    throw new InvalidSettingsException(
        "Choose Minimize or Maximize for a custom performance column.");
  }

  private static int[] indices(final DataTableSpec spec, final List<String> names)
      throws InvalidSettingsException {
    final int[] result = new int[names.size()];
    for (int index = 0; index < names.size(); index++) {
      result[index] = spec.findColumnIndex(names.get(index));
      if (result[index] < 0) {
        throw new InvalidSettingsException("Missing property column: " + names.get(index));
      }
    }
    return result;
  }

  private void validate(final DataTableSpec spec) throws InvalidSettingsException {
    final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
    if (m_output.getStringValue().isBlank()
        || m_folder.getStringValue().isBlank()
        || m_problemId.getStringValue().isBlank()
        || m_algorithm.getStringValue().isBlank()) {
      throw new InvalidSettingsException("Output, folder, problem ID, and algorithm are required.");
    }
    if (spec.findColumnIndex(PopulationState.COLUMN) >= 0) {
      throw new InvalidSettingsException(
          "Connect an event-history table, not a population table. Use Evaluation output, Track Progress, or Record Population trace.");
    }
    final int nfe = spec.findColumnIndex(PopulationState.NFE);
    if (spec.findColumnIndex("Run") < 0 || nfe < 0) {
      throw new InvalidSettingsException("Input must contain visible Run and NFE columns.");
    }
    if (!spec.getColumnSpec(nfe).getType().isCompatible(LongValue.class)) {
      throw new InvalidSettingsException("NFE must be a long-integer column.");
    }
    if (!List.of("AUTO", "MINIMIZE", "MAXIMIZE").contains(m_direction.getStringValue())) {
      throw new InvalidSettingsException("Unknown performance direction.");
    }
    KnimeTableSupport.requireNumericColumns(spec, List.of(performanceColumn(spec, problem)));
    indices(spec, KnimeTableSupport.names(m_properties.getStringValue()));
  }

  private SettingsModel[] models() {
    return new SettingsModel[] {
      m_output,
      m_folder,
      m_suite,
      m_problemId,
      m_algorithm,
      m_info,
      m_rawY,
      m_instance,
      m_complete,
      m_properties,
      m_direction
    };
  }

  @Override
  protected void saveSettingsTo(final NodeSettingsWO settings) {
    for (SettingsModel model : models()) model.saveSettingsTo(settings);
  }

  @Override
  protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
    for (SettingsModel model : models()) model.validateSettings(settings);
  }

  @Override
  protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
      throws InvalidSettingsException {
    for (SettingsModel model : models()) model.loadSettingsFrom(settings);
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
