package org.ideaflow.nodes.loop.end;

import org.knime.core.node.defaultnodesettings.*;

final class OptimizationLoopEndNodeDialog extends DefaultNodeSettingsPane {
    OptimizationLoopEndNodeDialog() {
        final SettingsModelString targets =
            new SettingsModelString(OptimizationLoopEndNodeModel.CFG_TARGET_CONDITIONS, "");
        final SettingsModelString rule =
            new SettingsModelString(OptimizationLoopEndNodeModel.CFG_TARGET_RULE, "ALL");
        final Runnable visibility = () -> {
            final String encoded = targets.getStringValue();
            rule.setEnabled(encoded != null && encoded.indexOf(',') >= 0);
        };
        targets.addChangeListener(event -> visibility.run());
        addDialogComponent(new DialogComponentString(
            targets, "Objective targets (encoded objective:value list)"));
        addDialogComponent(new DialogComponentStringSelection(rule,
            "When several targets are configured", "ALL", "ANY"));
        visibility.run();
    }
}
