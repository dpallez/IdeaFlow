package org.ideaflow.nodes.trace;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class RecordPopulationNodeFactory extends NodeFactory<RecordPopulationNodeModel> implements ModernNodeDialogFactory{
 @Override public RecordPopulationNodeModel createNodeModel(){return new RecordPopulationNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<RecordPopulationNodeModel> createNodeView(int i,RecordPopulationNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new RecordPopulationNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return ModernNodeParameters.RecordPopulation.class;}
}
