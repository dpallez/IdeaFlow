package org.ideaflow.nodes.trace;

import org.ideaflow.knime.ModernNodeDialogFactory;
import org.ideaflow.knime.ModernNodeParameters;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/** Creates Record Population nodes and exposes their classic and modern dialogs. */
public final class RecordPopulationNodeFactory extends NodeFactory<RecordPopulationNodeModel>
    implements ModernNodeDialogFactory {
  @Override
  public RecordPopulationNodeModel createNodeModel() {
    return new RecordPopulationNodeModel();
  }

  @Override
  protected int getNrNodeViews() {
    return 0;
  }

  @Override
  public NodeView<RecordPopulationNodeModel> createNodeView(
      final int viewIndex, final RecordPopulationNodeModel nodeModel) {
    return null;
  }

  @Override
  protected boolean hasDialog() {
    return true;
  }

  @Override
  protected NodeDialogPane createNodeDialogPane() {
    return new RecordPopulationNodeDialog();
  }

  @Override
  public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass() {
    return ModernNodeParameters.RecordPopulation.class;
  }
}
