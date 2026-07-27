package org.ideaflow.nodes.variation;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class MutationNodeFactory extends NodeFactory<MutationNodeModel> implements ModernNodeDialogFactory{
 @Override public MutationNodeModel createNodeModel(){return new MutationNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<MutationNodeModel> createNodeView(int i,MutationNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new MutationNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return ModernNodeParameters.Mutation.class;}
}
