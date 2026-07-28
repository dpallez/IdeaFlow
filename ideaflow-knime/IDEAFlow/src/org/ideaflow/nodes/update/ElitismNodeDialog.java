package org.ideaflow.nodes.update;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/** Classic-workbench configuration for survivor selection. */
final class ElitismNodeDialog extends DefaultNodeSettingsPane {
  ElitismNodeDialog() {
    final SettingsModelString mode = new SettingsModelString(ElitismNodeModel.CFG_MODE, "NSGA_II");
    final SettingsModelIntegerBounded divisions =
        new SettingsModelIntegerBounded(ElitismNodeModel.CFG_REFERENCE_DIVISIONS, 12, 1, 1000);
    final SettingsModelString policy =
        new SettingsModelString(ElitismNodeModel.CFG_SIZE_POLICY, "FIXED");
    final SettingsModelIntegerBounded minimum =
        new SettingsModelIntegerBounded(ElitismNodeModel.CFG_MINIMUM_SIZE, 4, 4, Integer.MAX_VALUE);
    final Runnable visibility =
        () -> {
          divisions.setEnabled("NSGA_III".equals(mode.getStringValue()));
          minimum.setEnabled("LINEAR_NFE".equals(policy.getStringValue()));
        };
    mode.addChangeListener(event -> visibility.run());
    policy.addChangeListener(event -> visibility.run());
    addDialogComponent(
        new DialogComponentStringSelection(
            mode, "Update mode", "SINGLE_OBJECTIVE", "DE_PAIRWISE", "NSGA_II", "NSGA_III", "GDE3"));
    addDialogComponent(new DialogComponentNumber(divisions, "NSGA-III reference divisions", 1));
    addDialogComponent(
        new DialogComponentStringSelection(
            policy, "Population size policy", "FIXED", "LINEAR_NFE"));
    addDialogComponent(new DialogComponentNumber(minimum, "Final population size", 1));
    visibility.run();
  }
}
