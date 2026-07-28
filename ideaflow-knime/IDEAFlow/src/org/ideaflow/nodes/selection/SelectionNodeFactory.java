package org.ideaflow.nodes.selection;

import org.ideaflow.knime.ModernNodeDialogFactory;
import org.ideaflow.knime.ModernNodeParameters;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/** Creates Selection nodes and exposes their classic and modern dialogs. */
public final class SelectionNodeFactory extends NodeFactory<SelectionNodeModel>
    implements ModernNodeDialogFactory {
  @Override
  public SelectionNodeModel createNodeModel() {
    return new SelectionNodeModel();
  }

  @Override
  protected int getNrNodeViews() {
    return 0;
  }

  @Override
  public NodeView<SelectionNodeModel> createNodeView(
      final int viewIndex, final SelectionNodeModel nodeModel) {
    return null;
  }

  @Override
  protected boolean hasDialog() {
    return true;
  }

  @Override
  protected NodeDialogPane createNodeDialogPane() {
    return new SelectionNodeDialog();
  }

  @Override
  public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass() {
    return ModernNodeParameters.Selection.class;
  }
}
