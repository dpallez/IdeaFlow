package org.ideaflow.nodes.trace;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/** Classic-workbench configuration for population trace labels. */
final class RecordPopulationNodeDialog extends DefaultNodeSettingsPane {
  RecordPopulationNodeDialog() {
    addDialogComponent(
        new DialogComponentString(
            new SettingsModelString(RecordPopulationNodeModel.CFG_STAGE, "operator-stage"),
            "Stage"));
    addDialogComponent(
        new DialogComponentString(
            new SettingsModelString(RecordPopulationNodeModel.CFG_OPERATOR, "custom"), "Operator"));
  }
}
