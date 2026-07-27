package org.ideaflow.nodes.variation;

import org.knime.core.node.defaultnodesettings.*;

final class RecombinationNodeDialog extends DefaultNodeSettingsPane {
    RecombinationNodeDialog() {
        final SettingsModelString strategy =
            new SettingsModelString(RecombinationNodeModel.CFG_STRATEGY, "SBX");
        final SettingsModelDoubleBounded probability =
            new SettingsModelDoubleBounded(RecombinationNodeModel.CFG_PROBABILITY, 0.9, 0, 1);
        final SettingsModelDoubleBounded eta =
            new SettingsModelDoubleBounded(RecombinationNodeModel.CFG_ETA, 20, 0.01, 10000);
        final Runnable visibility = () -> {
            final boolean differential = strategy.getStringValue().startsWith("DE_");
            probability.setEnabled(!differential);
            eta.setEnabled("SBX".equals(strategy.getStringValue()));
        };
        strategy.addChangeListener(event -> visibility.run());
        addDialogComponent(new DialogComponentStringSelection(strategy, "Strategy",
            "SBX", "UNIFORM", "ONE_POINT", "ARITHMETIC", "DE_BINOMIAL", "DE_EXPONENTIAL"));
        addDialogComponent(new DialogComponentNumber(probability, "Crossover probability", 0.05));
        addDialogComponent(new DialogComponentNumber(eta, "SBX distribution index", 1));
        visibility.run();
    }
}
