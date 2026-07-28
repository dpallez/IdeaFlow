package org.ideaflow.nodes.analysis.run;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/** Classic-workbench configuration for repeated-run analysis. */
final class OptimizationRunAnalysisNodeDialog extends DefaultNodeSettingsPane {
  OptimizationRunAnalysisNodeDialog() {
    addDialogComponent(
        new DialogComponentString(
            new SettingsModelString(OptimizationRunAnalysisNodeModel.CFG_SERIES, "Series"),
            "Series column"));
    addDialogComponent(
        new DialogComponentString(
            new SettingsModelString(OptimizationRunAnalysisNodeModel.CFG_RUN, "Run"),
            "Run column"));
    addDialogComponent(
        new DialogComponentString(
            new SettingsModelString(OptimizationRunAnalysisNodeModel.CFG_NFE, "NFE"),
            "Evaluation-budget column"));
    addDialogComponent(
        new DialogComponentString(
            new SettingsModelString(OptimizationRunAnalysisNodeModel.CFG_VALUE, "Best"),
            "Performance column"));
    addDialogComponent(
        new DialogComponentStringSelection(
            new SettingsModelString(OptimizationRunAnalysisNodeModel.CFG_DIRECTION, "AUTO"),
            "Preferred direction",
            "AUTO",
            "MINIMIZE",
            "MAXIMIZE"));
    addDialogComponent(
        new DialogComponentNumber(
            new SettingsModelDouble(OptimizationRunAnalysisNodeModel.CFG_TARGET, 0.1),
            "Per-run success target",
            0.01));
    addDialogComponent(
        new DialogComponentBoolean(
            new SettingsModelBoolean(OptimizationRunAnalysisNodeModel.CFG_CARRY_FORWARD, true),
            "Carry the latest observation forward across unequal checkpoints"));
  }
}
