package org.ideaflow.nodes.selection;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class ParentSelectionNodeFactory extends NodeFactory<ParentSelectionNodeModel> implements ModernNodeDialogFactory{
 @Override public ParentSelectionNodeModel createNodeModel(){return new ParentSelectionNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<ParentSelectionNodeModel> createNodeView(int i,ParentSelectionNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new ParentSelectionNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return ModernNodeParameters.ParentSelection.class;}
}
