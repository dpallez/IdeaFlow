package org.ideaflow.nodes.update;

import org.ideaflow.knime.ModernNodeDialogFactory;
import org.ideaflow.knime.ModernNodeParameters;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/** Creates Elitism nodes and exposes their classic and modern dialogs. */
public final class ElitismNodeFactory extends NodeFactory<ElitismNodeModel>
    implements ModernNodeDialogFactory {
  @Override
  public ElitismNodeModel createNodeModel() {
    return new ElitismNodeModel();
  }

  @Override
  protected int getNrNodeViews() {
    return 0;
  }

  @Override
  public NodeView<ElitismNodeModel> createNodeView(
      final int viewIndex, final ElitismNodeModel nodeModel) {
    return null;
  }

  @Override
  protected boolean hasDialog() {
    return true;
  }

  @Override
  protected NodeDialogPane createNodeDialogPane() {
    return new ElitismNodeDialog();
  }

  @Override
  public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass() {
    return ModernNodeParameters.Elitism.class;
  }
}
