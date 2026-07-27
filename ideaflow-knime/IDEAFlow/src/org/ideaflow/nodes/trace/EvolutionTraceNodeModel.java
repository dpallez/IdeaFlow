package org.ideaflow.nodes.trace;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ideaflow.api.ReservedColumns;
import org.ideaflow.knime.KnimeTableSupport;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.ideaflow.knime.PopulationState;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
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

/** Records all numeric result and state columns without asking users to retype their names. */
public final class EvolutionTraceNodeModel extends NodeModel {
    static final String CFG_STAGE = "stage";
    static final String CFG_OPERATOR = "operator";
    private final SettingsModelString m_stage = new SettingsModelString(CFG_STAGE, "operator-stage");
    private final SettingsModelString m_operator = new SettingsModelString(CFG_OPERATOR, "custom");

    EvolutionTraceNodeModel() { super(1, 2); }

    @Override protected DataTableSpec[] configure(final DataTableSpec[] input) throws InvalidSettingsException {
        validate(input[0]);
        return new DataTableSpec[]{input[0], traceSpec(input[0], valueNames(input[0]))};
    }

    @Override protected BufferedDataTable[] execute(final BufferedDataTable[] input,
            final ExecutionContext execution) throws Exception {
        final DataTableSpec spec = input[0].getDataTableSpec();
        validate(spec);
        final List<String> names = valueNames(spec);
        final int[] valueIndices = KnimeTableSupport.requireNumericColumns(spec, names);
        final BufferedDataContainer trace =
            execution.createDataContainer(traceSpec(spec, names));
        long index = 0;
        for (DataRow row : input[0]) {
            final DataCell[] cells = new DataCell[6 + names.size()];
            cells[0] = new StringCell(PopulationState.run(row, spec));
            cells[1] = new StringCell(PopulationState.population(row, spec));
            cells[2] = new StringCell(PopulationState.individual(row, spec));
            cells[3] = new LongCell(PopulationState.nfe(row, spec));
            cells[4] = new StringCell(m_stage.getStringValue());
            cells[5] = new StringCell(m_operator.getStringValue());
            for (int value = 0; value < names.size(); value++) {
                final DataCell source = row.getCell(valueIndices[value]);
                cells[6 + value] = source.isMissing() ? org.knime.core.data.DataType.getMissingCell()
                    : new DoubleCell(KnimeTableSupport.number(source, row, names.get(value)));
            }
            trace.addRowToTable(new DefaultRow("Trace" + index++, cells));
            execution.checkCanceled();
        }
        trace.close();
        return new BufferedDataTable[]{input[0], trace.getTable()};
    }

    private static DataTableSpec traceSpec(final DataTableSpec input, final List<String> names)
            throws InvalidSettingsException {
        final List<DataColumnSpec> columns = new ArrayList<>(List.of(
            KnimeTableSupport.stringColumn("Run"),
            KnimeTableSupport.stringColumn("Population"),
            KnimeTableSupport.stringColumn("Individual"),
            KnimeTableSupport.longColumn(PopulationState.NFE),
            KnimeTableSupport.stringColumn("Stage"),
            KnimeTableSupport.stringColumn("Operator")));
        for (String name : names) columns.add(KnimeTableSupport.doubleColumn(name));
        final DataTableSpec raw = new DataTableSpec(columns.toArray(DataColumnSpec[]::new));
        return ProblemMetadata.attach(raw, "Run", ProblemMetadata.require(input));
    }

    private static List<String> valueNames(final DataTableSpec spec) throws InvalidSettingsException {
        final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
        final Set<String> decisionColumns = new HashSet<>();
        problem.variables().forEach(variable -> decisionColumns.addAll(variable.populationColumns()));
        final Set<String> base = Set.of(PopulationState.COLUMN, PopulationState.NFE);
        final List<String> result = new ArrayList<>();
        for (DataColumnSpec column : spec) {
            final Class<?> value = column.getType().getPreferredValueClass();
            if (!decisionColumns.contains(column.getName()) && !base.contains(column.getName())
                    && (DoubleValue.class.isAssignableFrom(value) || IntValue.class.isAssignableFrom(value)
                        || LongValue.class.isAssignableFrom(value))) {
                result.add(column.getName());
            }
        }
        return List.copyOf(result);
    }

    private void validate(final DataTableSpec spec) throws InvalidSettingsException {
        ProblemMetadata.require(spec);
        if (m_stage.getStringValue().isBlank() || m_operator.getStringValue().isBlank()) {
            throw new InvalidSettingsException("Stage and operator names are required.");
        }
        PopulationState.requireVisibleColumns(spec);
    }

    @Override protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_stage.saveSettingsTo(settings);
        m_operator.saveSettingsTo(settings);
    }
    @Override protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_stage.validateSettings(settings);
        m_operator.validateSettings(settings);
    }
    @Override protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_stage.loadSettingsFrom(settings);
        m_operator.loadSettingsFrom(settings);
    }
    @Override protected void loadInternals(final File directory, final ExecutionMonitor monitor)
            throws IOException, CanceledExecutionException { }
    @Override protected void saveInternals(final File directory, final ExecutionMonitor monitor)
            throws IOException, CanceledExecutionException { }
    @Override protected void reset() { }
}
