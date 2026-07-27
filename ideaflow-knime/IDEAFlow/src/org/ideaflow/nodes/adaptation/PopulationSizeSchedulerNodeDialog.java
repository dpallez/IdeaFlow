package org.ideaflow.nodes.adaptation;

import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

final class PopulationSizeSchedulerNodeDialog extends DefaultNodeSettingsPane {
    PopulationSizeSchedulerNodeDialog() {
        addDialogComponent(new DialogComponentNumber(
            new SettingsModelIntegerBounded(PopulationSizeSchedulerNodeModel.CFG_MINIMUM,
                4, 4, Integer.MAX_VALUE),
            "Minimum population size", 1));
    }
}
