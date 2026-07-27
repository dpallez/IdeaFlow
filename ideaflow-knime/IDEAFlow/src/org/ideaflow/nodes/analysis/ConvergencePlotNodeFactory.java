package org.ideaflow.nodes.analysis;

import org.ideaflow.knime.ModernNodeDialogFactory;
import org.ideaflow.knime.ModernNodeParameters;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.webui.node.view.NodeViewFactory;

public final class ConvergencePlotNodeFactory extends NodeFactory<ConvergencePlotNodeModel>
        implements ModernNodeDialogFactory, NodeViewFactory<ConvergencePlotNodeModel> {
    @Override public ConvergencePlotNodeModel createNodeModel() {
        return new ConvergencePlotNodeModel();
    }
    @Override protected int getNrNodeViews() { return 1; }
    @Override public NodeView<ConvergencePlotNodeModel> createNodeView(final int index,
            final ConvergencePlotNodeModel model) {
        if (index != 0) throw new IndexOutOfBoundsException("Unknown convergence view: " + index);
        return new ConvergencePlotNodeView(model);
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
