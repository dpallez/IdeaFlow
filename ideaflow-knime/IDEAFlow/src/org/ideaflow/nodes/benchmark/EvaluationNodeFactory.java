package org.ideaflow.nodes.benchmark;

import org.ideaflow.knime.ModernNodeDialogFactory;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/** Creates Evaluation nodes and exposes their classic and modern dialogs. */
public final class EvaluationNodeFactory extends NodeFactory<EvaluationNodeModel>
    implements ModernNodeDialogFactory {
  @Override
  public EvaluationNodeModel createNodeModel() {
    return new EvaluationNodeModel();
  }

  @Override
  protected int getNrNodeViews() {
    return 0;
  }

  @Override
  public NodeView<EvaluationNodeModel> createNodeView(
      final int viewIndex, final EvaluationNodeModel nodeModel) {
    return null;
  }

  @Override
  protected boolean hasDialog() {
    return true;
  }

  @Override
  protected NodeDialogPane createNodeDialogPane() {
    return new EvaluationNodeDialog();
  }

  @Override
  public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass() {
    return EvaluationNodeParameters.class;
  }
}
