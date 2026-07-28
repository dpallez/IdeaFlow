package org.ideaflow.nodes.variation.mutation;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/** Classic-workbench configuration for mutation and DE parameter adaptation. */
final class MutationNodeDialog extends DefaultNodeSettingsPane {
  MutationNodeDialog() {
    final SettingsModelString strategy =
        new SettingsModelString(MutationNodeModel.CFG_STRATEGY, "POLYNOMIAL");
    final SettingsModelBoolean auto =
        new SettingsModelBoolean(MutationNodeModel.CFG_AUTO_RATE, true);
    final SettingsModelDoubleBounded rate =
        new SettingsModelDoubleBounded(MutationNodeModel.CFG_RATE, 0.1, 0, 1);
    final SettingsModelDoubleBounded scale =
        new SettingsModelDoubleBounded(MutationNodeModel.CFG_SCALE, 0.1, 0, Double.MAX_VALUE);
    final SettingsModelDoubleBounded eta =
        new SettingsModelDoubleBounded(MutationNodeModel.CFG_ETA, 20, 0.01, 10000);
    final SettingsModelString adaptation =
        new SettingsModelString(MutationNodeModel.CFG_ADAPTATION, "FIXED");
    final SettingsModelDoubleBounded f =
        new SettingsModelDoubleBounded(MutationNodeModel.CFG_F, 0.5, Double.MIN_NORMAL, 2.0);
    final SettingsModelDoubleBounded cr =
        new SettingsModelDoubleBounded(MutationNodeModel.CFG_INITIAL_CR, 0.9, 0, 1);
    final SettingsModelDoubleBounded tauF =
        new SettingsModelDoubleBounded(MutationNodeModel.CFG_TAU_F, 0.1, 0, 1);
    final SettingsModelDoubleBounded tauCr =
        new SettingsModelDoubleBounded(MutationNodeModel.CFG_TAU_CR, 0.1, 0, 1);
    final SettingsModelIntegerBounded memory =
        new SettingsModelIntegerBounded(MutationNodeModel.CFG_MEMORY_SIZE, 6, 1, 1000);
    final SettingsModelString repair =
        new SettingsModelString(MutationNodeModel.CFG_REPAIR, "REFLECT");
    final Runnable visibility =
        () -> {
          final boolean differential = strategy.getStringValue().startsWith("DE_");
          auto.setEnabled(!differential);
          rate.setEnabled(!differential && !auto.getBooleanValue());
          scale.setEnabled("GAUSSIAN".equals(strategy.getStringValue()));
          eta.setEnabled("POLYNOMIAL".equals(strategy.getStringValue()));
          adaptation.setEnabled(differential);
          f.setEnabled(differential);
          cr.setEnabled(differential);
          tauF.setEnabled(differential && "JDE".equals(adaptation.getStringValue()));
          tauCr.setEnabled(differential && "JDE".equals(adaptation.getStringValue()));
          memory.setEnabled(differential && "SHADE".equals(adaptation.getStringValue()));
          repair.setEnabled(differential);
        };
    strategy.addChangeListener(event -> visibility.run());
    auto.addChangeListener(event -> visibility.run());
    adaptation.addChangeListener(event -> visibility.run());
    addDialogComponent(
        new DialogComponentStringSelection(
            strategy,
            "Strategy",
            "POLYNOMIAL",
            "GAUSSIAN",
            "BIT_FLIP",
            "RANDOM_RESET",
            "DE_RAND_1",
            "DE_BEST_1",
            "DE_CURRENT_TO_BEST_1",
            "DE_CURRENT_TO_PBEST_1"));
    addDialogComponent(new DialogComponentBoolean(auto, "Use 1 / variable count mutation rate"));
    addDialogComponent(new DialogComponentNumber(rate, "Mutation probability", 0.01));
    addDialogComponent(
        new DialogComponentNumber(scale, "Gaussian scale (fraction of range)", 0.01));
    addDialogComponent(new DialogComponentNumber(eta, "Polynomial distribution index", 1));
    addDialogComponent(
        new DialogComponentStringSelection(
            adaptation, "DE parameter control", "FIXED", "JDE", "SHADE"));
    addDialogComponent(new DialogComponentNumber(f, "Initial or fixed F", 0.05));
    addDialogComponent(new DialogComponentNumber(cr, "Initial or fixed CR", 0.05));
    addDialogComponent(new DialogComponentNumber(tauF, "jDE F adaptation probability", 0.01));
    addDialogComponent(new DialogComponentNumber(tauCr, "jDE CR adaptation probability", 0.01));
    addDialogComponent(new DialogComponentNumber(memory, "SHADE memory size", 1));
    addDialogComponent(
        new DialogComponentStringSelection(
            repair, "DE bounds repair", "REFLECT", "CLAMP", "RANDOM"));
    visibility.run();
  }
}
