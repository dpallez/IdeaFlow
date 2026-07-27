package org.ideaflow.nodes.surrogate;

import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

final class SurrogateCoordinatorNodeDialog extends DefaultNodeSettingsPane {
    SurrogateCoordinatorNodeDialog() {
        addDialogComponent(new DialogComponentString(
            new SettingsModelString(SurrogateCoordinatorNodeModel.CFG_ACQUISITION, "acquisition"),
            "Acquisition/score column"));
        addDialogComponent(new DialogComponentStringSelection(
            new SettingsModelString(SurrogateCoordinatorNodeModel.CFG_DIRECTION, "MAXIMIZE"),
            "Preferred score direction", "MINIMIZE", "MAXIMIZE"));
        addDialogComponent(new DialogComponentNumber(
            new SettingsModelIntegerBounded(SurrogateCoordinatorNodeModel.CFG_EXACT_COUNT,
                10, 1, Integer.MAX_VALUE),
            "Exact evaluations per population", 1));
    }
}
