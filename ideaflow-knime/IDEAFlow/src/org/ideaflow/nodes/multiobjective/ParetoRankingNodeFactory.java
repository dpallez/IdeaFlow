package org.ideaflow.nodes.multiobjective;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class ParetoRankingNodeFactory extends NodeFactory<ParetoRankingNodeModel> implements ModernNodeDialogFactory{
 @Override public ParetoRankingNodeModel createNodeModel(){return new ParetoRankingNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<ParetoRankingNodeModel> createNodeView(int i,ParetoRankingNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new ParetoRankingNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return ModernNodeParameters.ParetoRanking.class;}
}
