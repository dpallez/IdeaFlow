package org.ideaflow.testing;

import java.util.List;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.NodeID;

/** Runs a node model through KNIME's public node wrapper and an in-memory execution context. */
public final class NodeTestHarness implements AutoCloseable {
  private final Node m_node;
  private final ExecutionContext m_executionContext;

  @SuppressWarnings({"unchecked", "rawtypes"})
  public NodeTestHarness(final NodeFactory<? extends NodeModel> factory) {
    m_node = new Node((NodeFactory) factory);
    final NodeID nodeId = new NodeID(0);
    final FlowObjectStack incoming =
        FlowObjectStack.createFromFlowVariableList(List.of(), nodeId);
    final FlowObjectStack outgoing =
        FlowObjectStack.createFromFlowVariableList(List.of(), nodeId);
    m_node.setFlowObjectStack(incoming, outgoing);
    m_executionContext = new ExecutionContext(new DefaultNodeProgressMonitor(), m_node);
  }

  public Node node() {
    return m_node;
  }

  public PortObjectSpec[] configure(final PortObjectSpec... input) throws Exception {
    return m_node.invokeNodeModelConfigure(input);
  }
  public NodeSettings settings() {
    final NodeSettings settings = new NodeSettings("model");
    m_node.saveModelSettingsTo(settings);
    return settings;
  }

  public void loadSettings(final NodeSettings settings) throws Exception {
    m_node.validateModelSettings(settings);
    m_node.loadModelSettingsFrom(settings);
  }

  public PortObject[] execute(final PortObject... input) throws Exception {
    return m_node.invokeNodeModelExecute(m_executionContext, input);
  }

  public BufferedDataTable table(final DataTableSpec spec, final List<DataCell[]> rows) {
    final BufferedDataContainer container = m_executionContext.createDataContainer(spec);
    for (int index = 0; index < rows.size(); index++) {
      container.addRowToTable(new DefaultRow("Row" + index, rows.get(index)));
    }
    container.close();
    return container.getTable();
  }

  @Override
  public void close() {
    m_node.cleanup();
  }
}
