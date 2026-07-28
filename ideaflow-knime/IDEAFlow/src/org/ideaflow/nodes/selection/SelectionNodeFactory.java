package org.ideaflow.nodes.selection;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class SelectionNodeFactory extends NodeFactory<SelectionNodeModel> implements ModernNodeDialogFactory{
 @Override public SelectionNodeModel createNodeModel(){return new SelectionNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<SelectionNodeModel> createNodeView(int i,SelectionNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new SelectionNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return ModernNodeParameters.Selection.class;}
}
