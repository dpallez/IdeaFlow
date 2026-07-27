package org.ideaflow.nodes.analysis;

import org.knime.core.node.NodeView;

final class EcdfPlotNodeView extends NodeView<EcdfPlotNodeModel> {
    private final OptimizationPlotPanel m_panel;

    EcdfPlotNodeView(final EcdfPlotNodeModel model) {
        super(model);
        m_panel = new OptimizationPlotPanel(model.plotData());
        setComponent(m_panel);
    }

    @Override protected void modelChanged() {
        m_panel.setData(getNodeModel().plotData());
    }
    @Override protected void onOpen() { }
    @Override protected void onClose() { }
}
