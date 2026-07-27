package org.ideaflow.nodes.variation;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class RecombinationNodeFactory extends NodeFactory<RecombinationNodeModel> implements ModernNodeDialogFactory{
 @Override public RecombinationNodeModel createNodeModel(){return new RecombinationNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<RecombinationNodeModel> createNodeView(int i,RecombinationNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new RecombinationNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return ModernNodeParameters.Recombination.class;}
}
