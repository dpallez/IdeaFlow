package org.ideaflow.nodes.io;

import org.ideaflow.knime.ModernNodeDialogFactory;
import org.ideaflow.knime.ModernNodeParameters;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/** Creates IOHprofiler export nodes and exposes their classic and modern dialogs. */
public final class ExportToIohProfilerNodeFactory extends NodeFactory<ExportToIohProfilerNodeModel>
    implements ModernNodeDialogFactory {
  @Override
  public ExportToIohProfilerNodeModel createNodeModel() {
    return new ExportToIohProfilerNodeModel();
  }

  @Override
  protected int getNrNodeViews() {
    return 0;
  }

  @Override
  public NodeView<ExportToIohProfilerNodeModel> createNodeView(
      int i, ExportToIohProfilerNodeModel m) {
    return null;
  }

  @Override
  protected boolean hasDialog() {
    return true;
  }

  @Override
  protected NodeDialogPane createNodeDialogPane() {
    return new ExportToIohProfilerNodeDialog();
  }

  @Override
  public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass() {
    return ModernNodeParameters.ExportToIohProfiler.class;
  }
}
