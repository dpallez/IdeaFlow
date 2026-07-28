package org.ideaflow.nodes.trace;

import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

final class RecordPopulationNodeDialog extends DefaultNodeSettingsPane {
    RecordPopulationNodeDialog() {
        addDialogComponent(new DialogComponentString(
            new SettingsModelString(RecordPopulationNodeModel.CFG_STAGE, "operator-stage"), "Stage"));
        addDialogComponent(new DialogComponentString(
            new SettingsModelString(RecordPopulationNodeModel.CFG_OPERATOR, "custom"), "Operator"));
    }
}
