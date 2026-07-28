package org.ideaflow.nodes.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ideaflow.api.OptimizationDirection;
import org.ideaflow.api.ReservedColumns;
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

    private final SettingsModelString m_output = new SettingsModelString(CFG_OUTPUT, "ioh-output");
    private final SettingsModelString m_folder = new SettingsModelString(CFG_FOLDER, "ioh_data");
    private final SettingsModelString m_suite = new SettingsModelString(CFG_SUITE, "unknown_suite");
    private final SettingsModelString m_problemId = new SettingsModelString(CFG_PROBLEM_ID, "1");
    private final SettingsModelString m_algorithm = new SettingsModelString(CFG_ALGORITHM, "IdeaFlow");
    private final SettingsModelString m_info = new SettingsModelString(CFG_INFO, "");
    private final SettingsModelString m_rawY = new SettingsModelString(CFG_RAW_Y, "");
    private final SettingsModelString m_properties = new SettingsModelString(CFG_PROPERTIES, "");
    private final SettingsModelIntegerBounded m_instance =
        new SettingsModelIntegerBounded(CFG_INSTANCE, 1, 1, Integer.MAX_VALUE);
    private final SettingsModelBoolean m_complete = new SettingsModelBoolean(CFG_COMPLETE, true);

    ExportToIohProfilerNodeModel() { super(1, 1); }

    private static DataTableSpec outSpec() {
        return new DataTableSpec(KnimeTableSupport.stringColumn("Output path"),
            KnimeTableSupport.intColumn("Event count"), KnimeTableSupport.stringColumn("Format"));
    }

    @Override protected DataTableSpec[] configure(final DataTableSpec[] input) throws InvalidSettingsException {
        validate(input[0]);
        return new DataTableSpec[]{outSpec()};
    }

    @Override protected BufferedDataTable[] execute(final BufferedDataTable[] input,
            final ExecutionContext execution) throws Exception {
        final DataTableSpec spec = input[0].getDataTableSpec();
        validate(spec);
        final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
        final String performance = performanceColumn(spec, problem);
        final int run = spec.findColumnIndex("Run");
        final int nfe = spec.findColumnIndex(PopulationState.NFE);
        final boolean populationTable = spec.findColumnIndex(PopulationState.COLUMN) >= 0;
        final int raw = spec.findColumnIndex(performance);
        final List<String> propertyNames = KnimeTableSupport.names(m_properties.getStringValue());
        final int[] propertyIndices = propertyNames.isEmpty() ? new int[0] : indices(spec, propertyNames);
        final List<IohProfilerWriter.Record> records = new ArrayList<>();
        for (DataRow row : input[0]) {
            final Map<String, String> attributes = new LinkedHashMap<>();
            for (int index = 0; index < propertyIndices.length; index++) {
                attributes.put(propertyNames.get(index), row.getCell(propertyIndices[index]).toString());
            }
            final String runId = populationTable ? PopulationState.run(row, spec) : row.getCell(run).toString();
            records.add(new IohProfilerWriter.Record(runId, m_instance.getIntValue(),
                ((LongValue)row.getCell(nfe)).getLongValue(),
                KnimeTableSupport.number(row.getCell(raw), row, performance), attributes));
            execution.checkCanceled();
        }
        final IohProfilerWriter.Metadata metadata = new IohProfilerWriter.Metadata(
            m_suite.getStringValue(), m_problemId.getStringValue(), problem.problemId(),
            problem.variables().size(), m_algorithm.getStringValue(), m_info.getStringValue(),
            maximization(problem, performance));
        final Path path = IohProfilerWriter.write(
            Path.of(m_output.getStringValue()).toAbsolutePath().normalize(), m_folder.getStringValue(),
            metadata, records, m_complete.getBooleanValue());
        final BufferedDataContainer output = execution.createDataContainer(outSpec());
        output.addRowToTable(new DefaultRow("IOHExport", new DataCell[]{
            new StringCell(path.toString()), new IntCell(records.size()),
            new StringCell("IOHprofiler-0.3.14")
        }));
        output.close();
        return new BufferedDataTable[]{output.getTable()};
    }

    private String performanceColumn(final DataTableSpec spec, final ProblemMetadata.Schema problem)
            throws InvalidSettingsException {
        if (!m_rawY.getStringValue().isBlank() && spec.findColumnIndex(m_rawY.getStringValue()) >= 0) {
            KnimeTableSupport.requireNumericColumns(spec, List.of(m_rawY.getStringValue()));
            return m_rawY.getStringValue();
        }
        if (problem.objectives().size() == 1) return problem.objectives().get(0).column();
        if (spec.findColumnIndex("Hypervolume") >= 0) return "Hypervolume";
        throw new InvalidSettingsException(
            "Select a scalar performance column. Multi-objective IOH export normally uses hypervolume.");
    }

    private static boolean maximization(final ProblemMetadata.Schema problem, final String performance) {
        if ("hypervolume".equalsIgnoreCase(performance)) return true;
        return problem.objectives().stream().filter(objective -> objective.column().equals(performance))
            .findFirst().map(objective -> objective.direction() == OptimizationDirection.MAXIMIZE).orElse(false);
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
        if (m_output.getStringValue().isBlank() || m_folder.getStringValue().isBlank()
                || m_problemId.getStringValue().isBlank() || m_algorithm.getStringValue().isBlank()) {
            throw new InvalidSettingsException("Output, folder, problem ID, and algorithm are required.");
        }
        final boolean population = spec.findColumnIndex(PopulationState.COLUMN) >= 0;
        if ((!population && spec.findColumnIndex("Run") < 0)
                || spec.findColumnIndex(PopulationState.NFE) < 0) {
            throw new InvalidSettingsException(
                "Input must contain Run and NFE columns from Evaluation, Track Progress, or Evolution Trace.");
        }
        if (population) PopulationState.requireVisibleColumns(spec);
        KnimeTableSupport.requireNumericColumns(spec, List.of(performanceColumn(spec, problem)));
        indices(spec, KnimeTableSupport.names(m_properties.getStringValue()));
    }


    private SettingsModel[] models() {
        return new SettingsModel[]{m_output, m_folder, m_suite, m_problemId, m_algorithm, m_info,
            m_rawY, m_instance, m_complete, m_properties};
    }
    @Override protected void saveSettingsTo(final NodeSettingsWO settings) {
        for (SettingsModel model : models()) model.saveSettingsTo(settings);
    }
    @Override protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        for (SettingsModel model : models()) model.validateSettings(settings);
    }
    @Override protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        for (SettingsModel model : models()) model.loadSettingsFrom(settings);
    }
    @Override protected void loadInternals(final File directory, final ExecutionMonitor monitor)
            throws IOException, CanceledExecutionException { }
    @Override protected void saveInternals(final File directory, final ExecutionMonitor monitor)
            throws IOException, CanceledExecutionException { }
    @Override protected void reset() { }
}
