package org.ideaflow.nodes.analysis;

import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

final class EcdfPlotNodeDialog extends DefaultNodeSettingsPane {
    EcdfPlotNodeDialog() {
        addDialogComponent(new DialogComponentString(new SettingsModelString(
            EcdfPlotNodeModel.CFG_SERIES, "Series"), "Series column"));
        addDialogComponent(new DialogComponentString(new SettingsModelString(
            EcdfPlotNodeModel.CFG_FITNESS, "Fitness"), "Fitness column"));
        addDialogComponent(new DialogComponentString(new SettingsModelString(
            EcdfPlotNodeModel.CFG_ECDF, "ECDF"), "ECDF column"));
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
            EcdfPlotNodeModel.CFG_LOG_X, false), "Logarithmic horizontal axis"));
    }
}
