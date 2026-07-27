package org.ideaflow.nodes.adaptation;

import org.knime.core.node.defaultnodesettings.*;

final class AdaptiveParameterControllerNodeDialog extends DefaultNodeSettingsPane {
    AdaptiveParameterControllerNodeDialog() {
        final SettingsModelString mode =
            new SettingsModelString(AdaptiveParameterControllerNodeModel.CFG_MODE, "JDE");
        final SettingsModelDoubleBounded f = new SettingsModelDoubleBounded(
            AdaptiveParameterControllerNodeModel.CFG_F, 0.5, 0.000001, 2);
        final SettingsModelDoubleBounded cr = new SettingsModelDoubleBounded(
            AdaptiveParameterControllerNodeModel.CFG_CR, 0.9, 0, 1);
        final SettingsModelDoubleBounded tauF = new SettingsModelDoubleBounded(
            AdaptiveParameterControllerNodeModel.CFG_TAU_F, 0.1, 0, 1);
        final SettingsModelDoubleBounded tauCr = new SettingsModelDoubleBounded(
            AdaptiveParameterControllerNodeModel.CFG_TAU_CR, 0.1, 0, 1);
        final SettingsModelIntegerBounded memory = new SettingsModelIntegerBounded(
            AdaptiveParameterControllerNodeModel.CFG_MEMORY_SIZE, 6, 1, 1000);
        final Runnable visibility = () -> {
            tauF.setEnabled("JDE".equals(mode.getStringValue()));
            tauCr.setEnabled("JDE".equals(mode.getStringValue()));
            memory.setEnabled("SHADE".equals(mode.getStringValue()));
        };
        mode.addChangeListener(event -> visibility.run());
        addDialogComponent(new DialogComponentStringSelection(
            mode, "Adaptation mode", "FIXED", "JDE", "SHADE"));
        addDialogComponent(new DialogComponentNumber(f, "Initial/mean F", 0.05));
        addDialogComponent(new DialogComponentNumber(cr, "Initial/mean CR", 0.05));
        addDialogComponent(new DialogComponentNumber(tauF, "jDE F adaptation probability", 0.01));
        addDialogComponent(new DialogComponentNumber(tauCr, "jDE CR adaptation probability", 0.01));
        addDialogComponent(new DialogComponentNumber(memory, "SHADE memory size", 1));
        visibility.run();
    }
}
