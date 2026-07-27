package org.ideaflow.nodes.migration;

import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

final class PopulationMigrationNodeDialog extends DefaultNodeSettingsPane {
    PopulationMigrationNodeDialog() {
        addDialogComponent(new DialogComponentNumber(
            new SettingsModelIntegerBounded(PopulationMigrationNodeModel.CFG_COUNT, 1, 1, Integer.MAX_VALUE),
            "Migrants per population", 1));
        addDialogComponent(new DialogComponentStringSelection(
            new SettingsModelString(PopulationMigrationNodeModel.CFG_TOPOLOGY, "RING"),
            "Migration topology", "RING", "RANDOM", "ALL_TO_ALL"));
        addDialogComponent(new DialogComponentStringSelection(
            new SettingsModelString(PopulationMigrationNodeModel.CFG_REPLACEMENT, "REPLACE_WORST"),
            "When migrants arrive", "REPLACE_WORST", "ADD"));
    }
}
