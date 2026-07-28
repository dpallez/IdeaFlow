package org.ideaflow.nodes.initialization;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class InitialPopulationNodeFactory extends NodeFactory<InitialPopulationNodeModel> implements ModernNodeDialogFactory{
 @Override public InitialPopulationNodeModel createNodeModel(){return new InitialPopulationNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<InitialPopulationNodeModel> createNodeView(int i,InitialPopulationNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new InitialPopulationNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return ModernNodeParameters.InitialPopulation.class;}
}
