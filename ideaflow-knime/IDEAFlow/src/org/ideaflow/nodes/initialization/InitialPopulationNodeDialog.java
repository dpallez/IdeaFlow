package org.ideaflow.nodes.initialization;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/** Classic-workbench configuration for initial population generation. */
final class InitialPopulationNodeDialog extends DefaultNodeSettingsPane {
  InitialPopulationNodeDialog() {
    addDialogComponent(
        new DialogComponentNumber(
            new SettingsModelIntegerBounded(
                InitialPopulationNodeModel.CFG_POPULATION_SIZE, 50, 1, 1000000),
            "Population size",
            1));
    addDialogComponent(
        new DialogComponentString(
            new SettingsModelString(InitialPopulationNodeModel.CFG_POPULATION_ID, "population-0"),
            "Population ID"));
  }
}
