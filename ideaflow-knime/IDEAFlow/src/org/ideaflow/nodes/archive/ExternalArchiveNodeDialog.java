package org.ideaflow.nodes.archive;

import org.knime.core.node.defaultnodesettings.*;

final class ExternalArchiveNodeDialog extends DefaultNodeSettingsPane {
    ExternalArchiveNodeDialog() {
        addDialogComponent(new DialogComponentStringSelection(
            new SettingsModelString(ExternalArchiveNodeModel.CFG_MODE, "PARETO"),
            "What should the archive remember?", "PARETO", "FIFO_UNIQUE"));
        addDialogComponent(new DialogComponentNumber(
            new SettingsModelIntegerBounded(ExternalArchiveNodeModel.CFG_MAX_SIZE, 100, 0, Integer.MAX_VALUE),
            "Maximum stored candidates (0 = unbounded)", 1));
        addDialogComponent(new DialogComponentStringSelection(
            new SettingsModelString(ExternalArchiveNodeModel.CFG_GROUPING, "RUN"),
            "How should populations share the archive?", "RUN", "RUN_AND_POPULATION"));
    }
}
