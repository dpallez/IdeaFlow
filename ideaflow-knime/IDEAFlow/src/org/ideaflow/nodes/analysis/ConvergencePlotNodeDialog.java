package org.ideaflow.nodes.analysis;

import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

final class ConvergencePlotNodeDialog extends DefaultNodeSettingsPane {
    ConvergencePlotNodeDialog() {
        addDialogComponent(new DialogComponentString(new SettingsModelString(
            ConvergencePlotNodeModel.CFG_SERIES, "Series"), "Series column"));
        addDialogComponent(new DialogComponentString(new SettingsModelString(
            ConvergencePlotNodeModel.CFG_NFE, "NFE"), "Horizontal-axis column"));
        addDialogComponent(new DialogComponentString(new SettingsModelString(
            ConvergencePlotNodeModel.CFG_VALUE, "Median"), "Convergence value column"));
        addDialogComponent(new DialogComponentString(new SettingsModelString(
            ConvergencePlotNodeModel.CFG_LOWER, "Q1"), "Lower band column (blank for none)"));
        addDialogComponent(new DialogComponentString(new SettingsModelString(
            ConvergencePlotNodeModel.CFG_UPPER, "Q3"), "Upper band column (blank for none)"));
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
            ConvergencePlotNodeModel.CFG_LOG_X, false), "Logarithmic horizontal axis"));
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
            ConvergencePlotNodeModel.CFG_LOG_Y, false), "Logarithmic vertical axis"));
    }
}
