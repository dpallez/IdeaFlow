package org.ideaflow.nodes.variation.recombination;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class CrossoverNodeFactory extends NodeFactory<CrossoverNodeModel> implements ModernNodeDialogFactory{
 @Override public CrossoverNodeModel createNodeModel(){return new CrossoverNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<CrossoverNodeModel> createNodeView(int i,CrossoverNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new CrossoverNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return ModernNodeParameters.Crossover.class;}
}
