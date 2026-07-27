package org.ideaflow.nodes.adaptation;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class PopulationSizeSchedulerNodeFactory extends NodeFactory<PopulationSizeSchedulerNodeModel> implements ModernNodeDialogFactory{
 @Override public PopulationSizeSchedulerNodeModel createNodeModel(){return new PopulationSizeSchedulerNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<PopulationSizeSchedulerNodeModel> createNodeView(int i,PopulationSizeSchedulerNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new PopulationSizeSchedulerNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return ModernNodeParameters.PopulationSizeScheduler.class;}
}
