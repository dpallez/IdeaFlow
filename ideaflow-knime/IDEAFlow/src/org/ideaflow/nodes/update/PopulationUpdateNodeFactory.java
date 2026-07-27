package org.ideaflow.nodes.update;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class PopulationUpdateNodeFactory extends NodeFactory<PopulationUpdateNodeModel> implements ModernNodeDialogFactory{
 @Override public PopulationUpdateNodeModel createNodeModel(){return new PopulationUpdateNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<PopulationUpdateNodeModel> createNodeView(int i,PopulationUpdateNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new PopulationUpdateNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return ModernNodeParameters.PopulationUpdate.class;}
}
