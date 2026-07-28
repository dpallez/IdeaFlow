package org.ideaflow.nodes.analysis.convergence;

import org.ideaflow.knime.ModernNodeDialogFactory;
import org.ideaflow.knime.ModernNodeParameters;
import org.ideaflow.nodes.analysis.OptimizationModernPlotView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.webui.node.view.NodeViewFactory;

public final class ConvergencePlotNodeFactory extends NodeFactory<ConvergencePlotNodeModel>
        implements ModernNodeDialogFactory, NodeViewFactory<ConvergencePlotNodeModel> {
    @Override public ConvergencePlotNodeModel createNodeModel() {
        return new ConvergencePlotNodeModel();
    }
    @Override protected int getNrNodeViews() { return 0; }
    @Override public org.knime.core.node.NodeView<ConvergencePlotNodeModel> createNodeView(
            final int index, final ConvergencePlotNodeModel model) {
        throw new IndexOutOfBoundsException("Convergence Plot has no legacy views.");
    }
    @Override protected boolean hasDialog() { return true; }
    @Override protected NodeDialogPane createNodeDialogPane() {
        return new ConvergencePlotNodeDialog();
    }
    @Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass() {
        return ModernNodeParameters.ConvergencePlot.class;
    }
    @Override public org.knime.core.webui.node.view.NodeView createNodeView(
            final ConvergencePlotNodeModel model) {
        return new OptimizationModernPlotView(model::plotData);
    }
}
