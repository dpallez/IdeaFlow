package org.ideaflow.nodes.update;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class ElitismNodeFactory extends NodeFactory<ElitismNodeModel> implements ModernNodeDialogFactory{
 @Override public ElitismNodeModel createNodeModel(){return new ElitismNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<ElitismNodeModel> createNodeView(int i,ElitismNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new ElitismNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return ModernNodeParameters.Elitism.class;}
}
