package org.ideaflow.nodes.metrics;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class ReferenceIndicatorsNodeFactory extends NodeFactory<ReferenceIndicatorsNodeModel> implements ModernNodeDialogFactory{
 @Override public ReferenceIndicatorsNodeModel createNodeModel(){return new ReferenceIndicatorsNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<ReferenceIndicatorsNodeModel> createNodeView(int i,ReferenceIndicatorsNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new ReferenceIndicatorsNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return ModernNodeParameters.ReferenceIndicators.class;}
}
