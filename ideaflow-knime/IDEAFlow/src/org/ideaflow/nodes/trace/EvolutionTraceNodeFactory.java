package org.ideaflow.nodes.trace;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class EvolutionTraceNodeFactory extends NodeFactory<EvolutionTraceNodeModel> implements ModernNodeDialogFactory{
 @Override public EvolutionTraceNodeModel createNodeModel(){return new EvolutionTraceNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<EvolutionTraceNodeModel> createNodeView(int i,EvolutionTraceNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new EvolutionTraceNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return ModernNodeParameters.EvolutionTrace.class;}
}
