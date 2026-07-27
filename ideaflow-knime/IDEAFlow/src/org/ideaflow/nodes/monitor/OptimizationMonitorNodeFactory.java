package org.ideaflow.nodes.monitor;

import org.ideaflow.knime.ModernNodeDialogFactory;
import org.ideaflow.knime.ModernNodeParameters;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.node.view.NodeViewFactory;
import org.knime.core.webui.node.view.table.TableNodeView;

public final class OptimizationMonitorNodeFactory extends NodeFactory<OptimizationMonitorNodeModel>
        implements ModernNodeDialogFactory, NodeViewFactory<OptimizationMonitorNodeModel> {
    @Override public OptimizationMonitorNodeModel createNodeModel() { return new OptimizationMonitorNodeModel(); }
    @Override protected int getNrNodeViews() { return 0; }
    @Override public NodeView<OptimizationMonitorNodeModel> createNodeView(final int index,
        final OptimizationMonitorNodeModel model) { return null; }
    @Override protected boolean hasDialog() { return true; }
    @Override protected NodeDialogPane createNodeDialogPane() { return new OptimizationMonitorNodeDialog(); }
    @Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass() {
        return ModernNodeParameters.OptimizationMonitor.class;
    }
    @Override public org.knime.core.webui.node.view.NodeView createNodeView(final OptimizationMonitorNodeModel model) {
        return new TableNodeView(model::summaryTable, NodeContext.getContext().getNodeContainer(), 0);
    }
}
