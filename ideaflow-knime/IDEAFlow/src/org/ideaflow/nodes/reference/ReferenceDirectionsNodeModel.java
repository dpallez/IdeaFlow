package org.ideaflow.nodes.reference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.ideaflow.core.ReferenceDirections;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

final class ReferenceDirectionsNodeModel extends NodeModel {
    static final String CFG_DIVISIONS = "divisions";
    private final SettingsModelIntegerBounded divisions = new SettingsModelIntegerBounded(CFG_DIVISIONS, 12, 1, 1000);

    ReferenceDirectionsNodeModel() { super(1, 1); }

    @Override protected DataTableSpec[] configure(final DataTableSpec[] input) throws InvalidSettingsException {
        final int objectives = objectiveCount(input[0]);
        guardSize(objectives);
        return new DataTableSpec[] {spec(objectives)};
    }

    @Override protected BufferedDataTable[] execute(final BufferedDataTable[] input, final ExecutionContext execution) throws Exception {
        final int objectiveCount = objectiveCount(input[0].getDataTableSpec());
        guardSize(objectiveCount);
        final List<double[]> directions = ReferenceDirections.dasDennis(objectiveCount, divisions.getIntValue());
        final DataTableSpec spec = spec(objectiveCount);
        final BufferedDataContainer output = execution.createDataContainer(spec);
        for (int row = 0; row < directions.size(); row++) {
            final DataCell[] cells = new DataCell[objectiveCount + 1];
            cells[0] = new StringCell("direction-" + row);
            for (int objective = 0; objective < objectiveCount; objective++) {
                cells[objective + 1] = new DoubleCell(directions.get(row)[objective]);
            }
            output.addRowToTable(new DefaultRow("Direction" + row, cells));
        }
        output.close();
        return new BufferedDataTable[] {output.getTable()};
    }

    private DataTableSpec spec(final int objectiveCount) {
        final List<DataColumnSpec> columns = new ArrayList<>();
        columns.add(new org.knime.core.data.DataColumnSpecCreator("direction_id", StringCell.TYPE).createSpec());
        for (int index = 0; index < objectiveCount; index++) {
            columns.add(new org.knime.core.data.DataColumnSpecCreator("weight_" + index, DoubleCell.TYPE).createSpec());
        }
        return new DataTableSpec(columns.toArray(DataColumnSpec[]::new));
    }

    private static int objectiveCount(final DataTableSpec spec) throws InvalidSettingsException {
        final int count = ProblemMetadata.require(spec).objectives().size();
        if (count < 2) throw new InvalidSettingsException("Reference Directions requires at least two objectives.");
        return count;
    }

    private void guardSize(final int objectiveCount) throws InvalidSettingsException {
        long count = 1;
        for (int i = 1; i <= objectiveCount - 1; i++) {
            count = count * (divisions.getIntValue() + i) / i;
            if (count > 1_000_000) throw new InvalidSettingsException("The settings create more than one million directions; reduce objectives or divisions.");
        }
    }

    @Override protected void saveSettingsTo(final NodeSettingsWO settings) { divisions.saveSettingsTo(settings); }
    @Override protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException { divisions.validateSettings(settings); }
    @Override protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException { divisions.loadSettingsFrom(settings); }
    @Override protected void loadInternals(final File directory, final ExecutionMonitor monitor) throws IOException, CanceledExecutionException {}
    @Override protected void saveInternals(final File directory, final ExecutionMonitor monitor) throws IOException, CanceledExecutionException {}
    @Override protected void reset() {}
}
