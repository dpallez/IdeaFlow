package org.ideaflow.nodes.selection;

import org.knime.core.node.defaultnodesettings.*;

/** Classic-workbench fallback; the modern dialog hides these fields instead of merely disabling them. */
final class ParentSelectionNodeDialog extends DefaultNodeSettingsPane {
    ParentSelectionNodeDialog() {
        final SettingsModelString strategy =
            new SettingsModelString(ParentSelectionNodeModel.CFG_MODE, "TOURNAMENT");
        final SettingsModelIntegerBounded tournament = new SettingsModelIntegerBounded(
            ParentSelectionNodeModel.CFG_TOURNAMENT_SIZE, 2, 1, Integer.MAX_VALUE);
        final SettingsModelIntegerBounded count = new SettingsModelIntegerBounded(
            ParentSelectionNodeModel.CFG_PARENT_COUNT, 100, 1, Integer.MAX_VALUE);
        final SettingsModelBoolean replacement =
            new SettingsModelBoolean(ParentSelectionNodeModel.CFG_WITH_REPLACEMENT, true);
        final SettingsModelDoubleBounded pbest = new SettingsModelDoubleBounded(
            ParentSelectionNodeModel.CFG_PBEST, 0.2, Double.MIN_NORMAL, 1.0);
        final Runnable visibility = () -> {
            final boolean ordinary = !"DE_DONORS".equals(strategy.getStringValue());
            tournament.setEnabled("TOURNAMENT".equals(strategy.getStringValue()));
            count.setEnabled(ordinary);
            replacement.setEnabled(ordinary);
            pbest.setEnabled(!ordinary);
        };
        strategy.addChangeListener(event -> visibility.run());
        addDialogComponent(new DialogComponentStringSelection(strategy, "Selection strategy",
            "TOURNAMENT", "RANDOM", "DE_DONORS"));
        addDialogComponent(new DialogComponentNumber(tournament, "Tournament size", 1));
        addDialogComponent(new DialogComponentNumber(count, "Parents per population", 1));
        addDialogComponent(new DialogComponentBoolean(replacement, "Allow duplicate selections"));
        addDialogComponent(new DialogComponentNumber(pbest, "DE p-best rate", 0.05));
        visibility.run();
    }
}
