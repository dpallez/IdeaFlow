package org.ideaflow.nodes.monitor;

import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

final class OptimizationMonitorNodeDialog extends DefaultNodeSettingsPane {
    OptimizationMonitorNodeDialog() {
        addDialogComponent(new DialogComponentString(new SettingsModelString(
            OptimizationMonitorNodeModel.CFG_STAGE, "population"), "Recorded stage name"));
    }
}
