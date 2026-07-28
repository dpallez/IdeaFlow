package org.ideaflow.nodes.monitor;

import org.ideaflow.knime.ModernNodeDialogFactory;
import org.ideaflow.knime.ModernNodeParameters;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.node.view.NodeViewFactory;
import org.knime.core.webui.node.view.table.TableNodeView;

/** Creates Track Progress nodes and exposes their classic and modern dialogs. */
public final class TrackProgressNodeFactory extends NodeFactory<TrackProgressNodeModel>
    implements ModernNodeDialogFactory, NodeViewFactory<TrackProgressNodeModel> {
  @Override
  public TrackProgressNodeModel createNodeModel() {
    return new TrackProgressNodeModel();
  }

  @Override
  protected int getNrNodeViews() {
    return 0;
  }

  @Override
  public NodeView<TrackProgressNodeModel> createNodeView(
      final int index, final TrackProgressNodeModel model) {
    return null;
  }

  @Override
  protected boolean hasDialog() {
    return true;
  }

  @Override
  protected NodeDialogPane createNodeDialogPane() {
    return new TrackProgressNodeDialog();
  }

  @Override
  public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass() {
    return ModernNodeParameters.TrackProgress.class;
  }

  @Override
  public org.knime.core.webui.node.view.NodeView createNodeView(
      final TrackProgressNodeModel model) {
    return new TableNodeView(model::summaryTable, NodeContext.getContext().getNodeContainer(), 0);
  }
}
