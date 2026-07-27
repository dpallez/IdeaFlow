package org.ideaflow.nodes.loop;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public final class EvolutionLoopStartNodeFactory extends NodeFactory<EvolutionLoopStartNodeModel> {
    @Override public EvolutionLoopStartNodeModel createNodeModel() { return new EvolutionLoopStartNodeModel(); }
    @Override protected int getNrNodeViews() { return 0; }
    @Override public NodeView<EvolutionLoopStartNodeModel> createNodeView(final int index,
        final EvolutionLoopStartNodeModel model) { return null; }
    @Override protected boolean hasDialog() { return false; }
    @Override protected NodeDialogPane createNodeDialogPane() { return null; }
}
