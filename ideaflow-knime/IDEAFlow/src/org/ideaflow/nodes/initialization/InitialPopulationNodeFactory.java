package org.ideaflow.nodes.initialization;

import org.ideaflow.knime.ModernNodeDialogFactory;
import org.ideaflow.knime.ModernNodeParameters;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/** Creates Initial Population nodes and exposes their classic and modern dialogs. */
public final class InitialPopulationNodeFactory extends NodeFactory<InitialPopulationNodeModel>
    implements ModernNodeDialogFactory {
  @Override
  public InitialPopulationNodeModel createNodeModel() {
    return new InitialPopulationNodeModel();
  }

  @Override
  protected int getNrNodeViews() {
    return 0;
  }

  @Override
  public NodeView<InitialPopulationNodeModel> createNodeView(
      final int viewIndex, final InitialPopulationNodeModel nodeModel) {
    return null;
  }

  @Override
  protected boolean hasDialog() {
    return true;
  }

  @Override
  protected NodeDialogPane createNodeDialogPane() {
    return new InitialPopulationNodeDialog();
  }

  @Override
  public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass() {
    return ModernNodeParameters.InitialPopulation.class;
  }
}
