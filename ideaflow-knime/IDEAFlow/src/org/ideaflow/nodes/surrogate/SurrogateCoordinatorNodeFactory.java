package org.ideaflow.nodes.surrogate;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class SurrogateCoordinatorNodeFactory extends NodeFactory<SurrogateCoordinatorNodeModel> implements ModernNodeDialogFactory{
 @Override public SurrogateCoordinatorNodeModel createNodeModel(){return new SurrogateCoordinatorNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<SurrogateCoordinatorNodeModel> createNodeView(int i,SurrogateCoordinatorNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new SurrogateCoordinatorNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return ModernNodeParameters.SurrogateCoordinator.class;}
}
