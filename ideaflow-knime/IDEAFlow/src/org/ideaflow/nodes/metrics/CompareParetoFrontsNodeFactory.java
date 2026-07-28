package org.ideaflow.nodes.metrics;

import org.ideaflow.knime.ModernNodeDialogFactory;
import org.ideaflow.knime.ModernNodeParameters;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/** Creates Pareto-front comparison nodes and exposes their classic and modern dialogs. */
public final class CompareParetoFrontsNodeFactory extends NodeFactory<CompareParetoFrontsNodeModel>
    implements ModernNodeDialogFactory {
  @Override
  public CompareParetoFrontsNodeModel createNodeModel() {
    return new CompareParetoFrontsNodeModel();
  }

  @Override
  protected int getNrNodeViews() {
    return 0;
  }

  @Override
  public NodeView<CompareParetoFrontsNodeModel> createNodeView(
      int i, CompareParetoFrontsNodeModel m) {
    return null;
  }

  @Override
  protected boolean hasDialog() {
    return true;
  }

  @Override
  protected NodeDialogPane createNodeDialogPane() {
    return new CompareParetoFrontsNodeDialog();
  }

  @Override
  public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass() {
    return ModernNodeParameters.CompareParetoFronts.class;
  }
}
