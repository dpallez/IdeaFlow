package org.ideaflow.nodes.multiobjective;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class RankParetoSolutionsNodeFactory extends NodeFactory<RankParetoSolutionsNodeModel> implements ModernNodeDialogFactory{
 @Override public RankParetoSolutionsNodeModel createNodeModel(){return new RankParetoSolutionsNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<RankParetoSolutionsNodeModel> createNodeView(int i,RankParetoSolutionsNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new RankParetoSolutionsNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return ModernNodeParameters.RankParetoSolutions.class;}
}
