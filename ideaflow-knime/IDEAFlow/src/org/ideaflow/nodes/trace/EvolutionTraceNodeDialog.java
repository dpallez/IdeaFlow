package org.ideaflow.nodes.trace;

import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

final class EvolutionTraceNodeDialog extends DefaultNodeSettingsPane {
    EvolutionTraceNodeDialog() {
        addDialogComponent(new DialogComponentString(
            new SettingsModelString(EvolutionTraceNodeModel.CFG_STAGE, "operator-stage"), "Stage"));
        addDialogComponent(new DialogComponentString(
            new SettingsModelString(EvolutionTraceNodeModel.CFG_OPERATOR, "custom"), "Operator"));
    }
}
