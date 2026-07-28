package org.ideaflow.nodes.variation.mutation;

import org.ideaflow.knime.ModernNodeDialogFactory;
import org.ideaflow.knime.ModernNodeParameters;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/** Creates Mutation nodes and exposes their classic and modern dialogs. */
public final class MutationNodeFactory extends NodeFactory<MutationNodeModel>
    implements ModernNodeDialogFactory {
  @Override
  public MutationNodeModel createNodeModel() {
    return new MutationNodeModel();
  }

  @Override
  protected int getNrNodeViews() {
    return 0;
  }

  @Override
  public NodeView<MutationNodeModel> createNodeView(
      final int viewIndex, final MutationNodeModel nodeModel) {
    return null;
  }

  @Override
  protected boolean hasDialog() {
    return true;
  }

  @Override
  protected NodeDialogPane createNodeDialogPane() {
    return new MutationNodeDialog();
  }

  @Override
  public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass() {
    return ModernNodeParameters.Mutation.class;
  }
}
