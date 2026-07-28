package org.ideaflow.nodes.loop.end;

import org.ideaflow.knime.ModernNodeDialogFactory;
import org.ideaflow.knime.ModernNodeParameters;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/** Creates the loop end that applies stopping rules and stores iteration feedback. */
public final class OptimizationLoopEndNodeFactory extends NodeFactory<OptimizationLoopEndNodeModel>
    implements ModernNodeDialogFactory {
  @Override
  public OptimizationLoopEndNodeModel createNodeModel() {
    return new OptimizationLoopEndNodeModel();
  }

  @Override
  protected int getNrNodeViews() {
    return 0;
  }

  @Override
  public NodeView<OptimizationLoopEndNodeModel> createNodeView(
      final int index, final OptimizationLoopEndNodeModel model) {
    return null;
  }

  @Override
  protected boolean hasDialog() {
    return true;
  }

  @Override
  protected NodeDialogPane createNodeDialogPane() {
    return new OptimizationLoopEndNodeDialog();
  }

  @Override
  public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass() {
    return ModernNodeParameters.OptimizationLoopEnd.class;
  }
}
