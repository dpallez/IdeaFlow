package org.ideaflow.nodes.multiobjective;

import org.ideaflow.knime.ModernNodeDialogFactory;
import org.ideaflow.knime.ModernNodeParameters;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/** Creates Pareto ranking nodes and exposes their classic and modern dialogs. */
public final class RankParetoSolutionsNodeFactory extends NodeFactory<RankParetoSolutionsNodeModel>
    implements ModernNodeDialogFactory {
  @Override
  public RankParetoSolutionsNodeModel createNodeModel() {
    return new RankParetoSolutionsNodeModel();
  }

  @Override
  protected int getNrNodeViews() {
    return 0;
  }

  @Override
  public NodeView<RankParetoSolutionsNodeModel> createNodeView(
      int i, RankParetoSolutionsNodeModel m) {
    return null;
  }

  @Override
  protected boolean hasDialog() {
    return true;
  }

  @Override
  protected NodeDialogPane createNodeDialogPane() {
    return new RankParetoSolutionsNodeDialog();
  }

  @Override
  public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass() {
    return ModernNodeParameters.RankParetoSolutions.class;
  }
}
