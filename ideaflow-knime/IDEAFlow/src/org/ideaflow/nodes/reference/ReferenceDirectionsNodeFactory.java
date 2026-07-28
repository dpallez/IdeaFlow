package org.ideaflow.nodes.reference;

import org.ideaflow.knime.ModernNodeDialogFactory;
import org.ideaflow.knime.ModernNodeParameters;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/** Creates Reference Directions nodes and exposes their classic and modern dialogs. */
public final class ReferenceDirectionsNodeFactory extends NodeFactory<ReferenceDirectionsNodeModel>
    implements ModernNodeDialogFactory {
  @Override
  public ReferenceDirectionsNodeModel createNodeModel() {
    return new ReferenceDirectionsNodeModel();
  }

  @Override
  protected int getNrNodeViews() {
    return 0;
  }

  @Override
  public NodeView<ReferenceDirectionsNodeModel> createNodeView(
      int i, ReferenceDirectionsNodeModel m) {
    return null;
  }

  @Override
  protected boolean hasDialog() {
    return true;
  }

  @Override
  protected NodeDialogPane createNodeDialogPane() {
    return new ReferenceDirectionsNodeDialog();
  }

  @Override
  public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass() {
    return ModernNodeParameters.ReferenceDirections.class;
  }
}
