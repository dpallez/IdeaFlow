package org.ideaflow.nodes.analysis.ecdf;

import org.ideaflow.knime.ModernNodeDialogFactory;
import org.ideaflow.knime.ModernNodeParameters;
import org.ideaflow.nodes.analysis.OptimizationModernPlotView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.webui.node.view.NodeViewFactory;

public final class EcdfPlotNodeFactory extends NodeFactory<EcdfPlotNodeModel>
        implements ModernNodeDialogFactory, NodeViewFactory<EcdfPlotNodeModel> {
    @Override public EcdfPlotNodeModel createNodeModel() {
        return new EcdfPlotNodeModel();
    }
    @Override protected int getNrNodeViews() { return 0; }
    @Override public org.knime.core.node.NodeView<EcdfPlotNodeModel> createNodeView(
            final int index, final EcdfPlotNodeModel model) {
        throw new IndexOutOfBoundsException("ECDF Plot has no legacy views.");
    }
    @Override protected boolean hasDialog() { return true; }
    @Override protected NodeDialogPane createNodeDialogPane() {
        return new EcdfPlotNodeDialog();
    }
    @Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass() {
        return ModernNodeParameters.EcdfPlot.class;
    }
    @Override public org.knime.core.webui.node.view.NodeView createNodeView(
            final EcdfPlotNodeModel model) {
        return new OptimizationModernPlotView(model::plotData);
    }
}
