package org.ideaflow.nodes.variation.recombination;

import org.ideaflow.knime.ModernNodeDialogFactory;
import org.ideaflow.knime.ModernNodeParameters;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/** Creates Crossover nodes and exposes their classic and modern dialogs. */
public final class CrossoverNodeFactory extends NodeFactory<CrossoverNodeModel>
    implements ModernNodeDialogFactory {
  @Override
  public CrossoverNodeModel createNodeModel() {
    return new CrossoverNodeModel();
  }

  @Override
  protected int getNrNodeViews() {
    return 0;
  }

  @Override
  public NodeView<CrossoverNodeModel> createNodeView(
      final int viewIndex, final CrossoverNodeModel nodeModel) {
    return null;
  }

  @Override
  protected boolean hasDialog() {
    return true;
  }

  @Override
  protected NodeDialogPane createNodeDialogPane() {
    return new CrossoverNodeDialog();
  }

  @Override
  public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass() {
    return ModernNodeParameters.Crossover.class;
  }
}
