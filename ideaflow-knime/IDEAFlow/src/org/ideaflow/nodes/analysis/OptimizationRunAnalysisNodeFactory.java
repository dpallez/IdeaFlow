package org.ideaflow.nodes.analysis;

import org.ideaflow.knime.ModernNodeDialogFactory;
import org.ideaflow.knime.ModernNodeParameters;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public final class OptimizationRunAnalysisNodeFactory
        extends NodeFactory<OptimizationRunAnalysisNodeModel> implements ModernNodeDialogFactory {
    @Override public OptimizationRunAnalysisNodeModel createNodeModel() {
        return new OptimizationRunAnalysisNodeModel();
    }
    @Override protected int getNrNodeViews() { return 0; }
    @Override public NodeView<OptimizationRunAnalysisNodeModel> createNodeView(
            final int index, final OptimizationRunAnalysisNodeModel model) {
        return null;
    }
    @Override protected boolean hasDialog() { return true; }
    @Override protected NodeDialogPane createNodeDialogPane() {
        return new OptimizationRunAnalysisNodeDialog();
    }
    @Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass() {
        return ModernNodeParameters.OptimizationRunAnalysis.class;
    }
}
