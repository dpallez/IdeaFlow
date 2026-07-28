package org.ideaflow.nodes.loop.start;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public final class OptimizationLoopStartNodeFactory extends NodeFactory<OptimizationLoopStartNodeModel> {
    @Override public OptimizationLoopStartNodeModel createNodeModel() { return new OptimizationLoopStartNodeModel(); }
    @Override protected int getNrNodeViews() { return 0; }
    @Override public NodeView<OptimizationLoopStartNodeModel> createNodeView(final int index,
        final OptimizationLoopStartNodeModel model) { return null; }
    @Override protected boolean hasDialog() { return false; }
    @Override protected NodeDialogPane createNodeDialogPane() { return null; }
}
