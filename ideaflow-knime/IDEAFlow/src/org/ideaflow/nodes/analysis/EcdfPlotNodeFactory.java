package org.ideaflow.nodes.analysis;

import org.ideaflow.knime.ModernNodeDialogFactory;
import org.ideaflow.knime.ModernNodeParameters;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.webui.node.view.NodeViewFactory;

public final class EcdfPlotNodeFactory extends NodeFactory<EcdfPlotNodeModel>
        implements ModernNodeDialogFactory, NodeViewFactory<EcdfPlotNodeModel> {
    @Override public EcdfPlotNodeModel createNodeModel() {
        return new EcdfPlotNodeModel();
    }
    @Override protected int getNrNodeViews() { return 1; }
    @Override public NodeView<EcdfPlotNodeModel> createNodeView(final int index,
            final EcdfPlotNodeModel model) {
        if (index != 0) throw new IndexOutOfBoundsException("Unknown ECDF view: " + index);
        return new EcdfPlotNodeView(model);
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
