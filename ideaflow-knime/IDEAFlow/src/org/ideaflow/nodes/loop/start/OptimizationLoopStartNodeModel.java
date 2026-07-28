package org.ideaflow.nodes.loop.start;

import java.io.File;
import java.io.IOException;
import org.ideaflow.nodes.loop.end.OptimizationLoopEndNodeModel;
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
public final class OptimizationLoopStartNodeModel extends NodeModel implements LoopStartNode {
  private int m_iteration;
  private BufferedDataTable m_currentArchive;

  OptimizationLoopStartNodeModel() {
    super(1, 2);
  }

  @Override
  protected BufferedDataTable[] execute(
      final BufferedDataTable[] input, final ExecutionContext execution) throws Exception {
    final BufferedDataTable population;
    final BufferedDataTable archive;
    if (m_iteration == 0) {
      population = input[0];
      archive = empty(input[0].getDataTableSpec(), execution);
    } else {
      if (!(getLoopEndNode() instanceof OptimizationLoopEndNodeModel end)) {
        throw new IllegalStateException(
            "Connect this node to an IdeaFlow Evolution Loop End node.");
      }
      population = copy(end.feedbackPopulation(), execution);
      archive = copy(end.feedbackArchive(), execution);
    }
    pushFlowVariableInt("ideaflow_iteration", m_iteration);
    m_currentArchive = archive;
    m_iteration++;

    return new BufferedDataTable[] {population, archive};
  }

  private static BufferedDataTable copy(
      final BufferedDataTable source, final ExecutionContext execution) {
    final BufferedDataContainer output =
        execution.createDataContainer(source.getDataTableSpec(), true);
    for (DataRow row : source) output.addRowToTable(row);

    output.close();
    return output.getTable();
  }

  public BufferedDataTable currentArchive() {
    if (m_currentArchive == null)
      throw new IllegalStateException("The loop has no current archive yet.");
    return m_currentArchive;
  }

  private static BufferedDataTable empty(
      final DataTableSpec spec, final ExecutionContext execution) {
    final BufferedDataContainer output = execution.createDataContainer(spec, true);
    output.close();
    return output.getTable();
  }

  @Override
  protected DataTableSpec[] configure(final DataTableSpec[] input) throws InvalidSettingsException {
    pushFlowVariableInt("ideaflow_iteration", m_iteration);
    return new DataTableSpec[] {input[0], input[0]};
  }

  @Override
  protected void reset() {
    m_iteration = 0;
    m_currentArchive = null;
  }

  @Override
  protected void saveSettingsTo(final NodeSettingsWO settings) {}

  @Override
  protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {}

  @Override
  protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
      throws InvalidSettingsException {}

  @Override
  protected void loadInternals(final File directory, final ExecutionMonitor monitor)
      throws IOException, CanceledExecutionException {}

  @Override
  protected void saveInternals(final File directory, final ExecutionMonitor monitor)
      throws IOException, CanceledExecutionException {}
}
