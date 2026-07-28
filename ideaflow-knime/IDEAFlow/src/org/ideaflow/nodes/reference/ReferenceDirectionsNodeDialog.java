package org.ideaflow.nodes.reference;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/** Classic-workbench configuration for reference-direction generation. */
final class ReferenceDirectionsNodeDialog extends DefaultNodeSettingsPane {
  ReferenceDirectionsNodeDialog() {
    addDialogComponent(
        new DialogComponentNumber(
            new SettingsModelIntegerBounded(
                ReferenceDirectionsNodeModel.CFG_DIVISIONS, 12, 1, 1000),
            "Das-Dennis divisions",
            1));
  }
}
