package org.ideaflow.nodes.metrics;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class CompareParetoFrontsNodeFactory extends NodeFactory<CompareParetoFrontsNodeModel> implements ModernNodeDialogFactory{
 @Override public CompareParetoFrontsNodeModel createNodeModel(){return new CompareParetoFrontsNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<CompareParetoFrontsNodeModel> createNodeView(int i,CompareParetoFrontsNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new CompareParetoFrontsNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return ModernNodeParameters.CompareParetoFronts.class;}
}
