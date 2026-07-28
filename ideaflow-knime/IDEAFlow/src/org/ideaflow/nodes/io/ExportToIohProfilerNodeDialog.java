package org.ideaflow.nodes.io;

import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

final class ExportToIohProfilerNodeDialog extends DefaultNodeSettingsPane {
    ExportToIohProfilerNodeDialog() {
        addDialogComponent(new DialogComponentString(
            new SettingsModelString(ExportToIohProfilerNodeModel.CFG_OUTPUT, "ioh-output"), "Output directory"));
        addDialogComponent(new DialogComponentString(
            new SettingsModelString(ExportToIohProfilerNodeModel.CFG_FOLDER, "ioh_data"), "New folder name"));
        addDialogComponent(new DialogComponentString(
            new SettingsModelString(ExportToIohProfilerNodeModel.CFG_SUITE, "unknown_suite"), "Suite"));
        addDialogComponent(new DialogComponentString(
            new SettingsModelString(ExportToIohProfilerNodeModel.CFG_PROBLEM_ID, "1"), "Problem ID"));
        addDialogComponent(new DialogComponentString(
            new SettingsModelString(ExportToIohProfilerNodeModel.CFG_ALGORITHM, "IdeaFlow"), "Algorithm"));
        addDialogComponent(new DialogComponentString(
            new SettingsModelString(ExportToIohProfilerNodeModel.CFG_INFO, ""), "Algorithm information"));
        addDialogComponent(new DialogComponentString(
            new SettingsModelString(ExportToIohProfilerNodeModel.CFG_RAW_Y, ""),
            "Scalar performance column (blank selects a single objective automatically)"));
        addDialogComponent(new DialogComponentNumber(
            new SettingsModelIntegerBounded(ExportToIohProfilerNodeModel.CFG_INSTANCE, 1, 1, Integer.MAX_VALUE),
            "Instance", 1));
        addDialogComponent(new DialogComponentBoolean(
            new SettingsModelBoolean(ExportToIohProfilerNodeModel.CFG_COMPLETE, true),
            "Write .cdat complete log"));
        addDialogComponent(new DialogComponentString(
            new SettingsModelString(ExportToIohProfilerNodeModel.CFG_PROPERTIES, ""),
            "Additional sidecar columns"));
    }
}
