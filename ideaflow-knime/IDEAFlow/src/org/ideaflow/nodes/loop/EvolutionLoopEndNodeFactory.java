package org.ideaflow.nodes.loop;

import org.ideaflow.knime.ModernNodeDialogFactory;
import org.ideaflow.knime.ModernNodeParameters;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public final class EvolutionLoopEndNodeFactory extends NodeFactory<EvolutionLoopEndNodeModel>
        implements ModernNodeDialogFactory {
    @Override public EvolutionLoopEndNodeModel createNodeModel() { return new EvolutionLoopEndNodeModel(); }
    @Override protected int getNrNodeViews() { return 0; }
    @Override public NodeView<EvolutionLoopEndNodeModel> createNodeView(final int index,
        final EvolutionLoopEndNodeModel model) { return null; }
    @Override protected boolean hasDialog() { return true; }
    @Override protected NodeDialogPane createNodeDialogPane() { return new EvolutionLoopEndNodeDialog(); }
    @Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass() {
        return ModernNodeParameters.EvolutionLoopEnd.class;
    }
}
