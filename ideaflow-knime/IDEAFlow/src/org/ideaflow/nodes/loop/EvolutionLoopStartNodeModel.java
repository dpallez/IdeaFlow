package org.ideaflow.nodes.loop;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.LoopStartNode;

/** Beginner-facing recursive population loop start. */
final class EvolutionLoopStartNodeModel extends NodeModel implements LoopStartNode {
    private int m_iteration;

    EvolutionLoopStartNodeModel() {
        super(1, 1);
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] input, final ExecutionContext execution)
            throws Exception {
        final BufferedDataTable population;
        if (m_iteration == 0) {
            population = input[0];
        } else {
            if (!(getLoopEndNode() instanceof EvolutionLoopEndNodeModel end)) {
                throw new IllegalStateException("Connect this node to an IdeaFlow Evolution Loop End node.");
            }
            population = copy(end.feedbackPopulation(), execution);
        }
        pushFlowVariableInt("ideaflow_iteration", m_iteration);
        m_iteration++;
        return new BufferedDataTable[]{population};
    }

    private static BufferedDataTable copy(final BufferedDataTable source, final ExecutionContext execution) {
        final BufferedDataContainer output = execution.createDataContainer(source.getDataTableSpec(), true);
        for (DataRow row : source) output.addRowToTable(row);
        output.close();
        return output.getTable();
    }

    @Override protected DataTableSpec[] configure(final DataTableSpec[] input) {
        pushFlowVariableInt("ideaflow_iteration", m_iteration);
        return new DataTableSpec[]{input[0]};
    }
    @Override protected void reset() { m_iteration = 0; }
    @Override protected void saveSettingsTo(final NodeSettingsWO settings) { }
    @Override protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException { }
    @Override protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException { }
    @Override protected void loadInternals(final File directory, final ExecutionMonitor monitor)
        throws IOException, CanceledExecutionException { }
    @Override protected void saveInternals(final File directory, final ExecutionMonitor monitor)
        throws IOException, CanceledExecutionException { }
}
