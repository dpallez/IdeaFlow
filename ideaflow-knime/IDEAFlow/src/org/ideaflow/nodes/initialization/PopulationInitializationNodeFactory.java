package org.ideaflow.nodes.initialization;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class PopulationInitializationNodeFactory extends NodeFactory<PopulationInitializationNodeModel> implements ModernNodeDialogFactory{
 @Override public PopulationInitializationNodeModel createNodeModel(){return new PopulationInitializationNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<PopulationInitializationNodeModel> createNodeView(int i,PopulationInitializationNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new PopulationInitializationNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return ModernNodeParameters.PopulationInitialization.class;}
}
