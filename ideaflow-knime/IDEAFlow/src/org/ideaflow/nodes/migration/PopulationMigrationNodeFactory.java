package org.ideaflow.nodes.migration;

import org.ideaflow.knime.ModernNodeDialogFactory;
import org.ideaflow.knime.ModernNodeParameters;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/** Creates Population Migration nodes and exposes their classic and modern dialogs. */
public final class PopulationMigrationNodeFactory extends NodeFactory<PopulationMigrationNodeModel>
    implements ModernNodeDialogFactory {
  @Override
  public PopulationMigrationNodeModel createNodeModel() {
    return new PopulationMigrationNodeModel();
  }

  @Override
  protected int getNrNodeViews() {
    return 0;
  }

  @Override
  public NodeView<PopulationMigrationNodeModel> createNodeView(
      int i, PopulationMigrationNodeModel m) {
    return null;
  }

  @Override
  protected boolean hasDialog() {
    return true;
  }

  @Override
  protected NodeDialogPane createNodeDialogPane() {
    return new PopulationMigrationNodeDialog();
  }

  @Override
  public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass() {
    return ModernNodeParameters.PopulationMigration.class;
  }
}
