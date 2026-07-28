package org.ideaflow.nodes.monitor;

import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

final class TrackProgressNodeDialog extends DefaultNodeSettingsPane {
    TrackProgressNodeDialog() {
        addDialogComponent(new DialogComponentString(new SettingsModelString(
            TrackProgressNodeModel.CFG_STAGE, "population"), "Recorded stage name"));
    }
}
