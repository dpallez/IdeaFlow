package org.ideaflow.nodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.ideaflow.api.IdeaFlowState;
import org.ideaflow.api.IdeaFlowStateCell;
import org.ideaflow.knime.PopulationState;
import org.ideaflow.nodes.analysis.convergence.ConvergencePlotNodeFactory;
import org.ideaflow.nodes.analysis.ecdf.EcdfPlotNodeFactory;
import org.ideaflow.nodes.analysis.run.OptimizationRunAnalysisNodeFactory;
import org.ideaflow.nodes.migration.PopulationMigrationNodeFactory;
import org.ideaflow.nodes.monitor.TrackProgressNodeFactory;
import org.ideaflow.nodes.update.ElitismNodeFactory;
import org.ideaflow.testing.NodeTestHarness;
import org.ideaflow.testing.TestPopulation;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObject;

final class PopulationPipelineNodeTest {
  @Test
  void elitismReturnsACompleteSurvivorPopulationAndTheDiscardedCandidates()
      throws Exception {
    try (NodeTestHarness elitism = new NodeTestHarness(new ElitismNodeFactory())) {
      final DataTableSpec spec = TestPopulation.spec();
      final BufferedDataTable parents = elitism.table(spec, parentRows());
      final BufferedDataTable trials = elitism.table(spec, trialRows());

      final PortObject[] output = elitism.execute(parents, trials);

      assertEquals(parents.size(), ((BufferedDataTable) output[0]).size());
      assertEquals(parents.size() + trials.size(),
          ((BufferedDataTable) output[0]).size() + ((BufferedDataTable) output[1]).size());
      for (DataRow survivor : (BufferedDataTable) output[0]) {
        assertEquals(2, PopulationState.get(survivor, spec).longValue(IdeaFlowState.GENERATION, -1));
      }
    }
  }

  @Test
  void progressAnalysisAndPlotNodesConsumeEachOthersPublishedTables() throws Exception {
    try (NodeTestHarness progress = new NodeTestHarness(new TrackProgressNodeFactory());
        NodeTestHarness analysis = new NodeTestHarness(new OptimizationRunAnalysisNodeFactory());
        NodeTestHarness convergence = new NodeTestHarness(new ConvergencePlotNodeFactory());
        NodeTestHarness ecdf = new NodeTestHarness(new EcdfPlotNodeFactory())) {
      final DataTableSpec spec = TestPopulation.spec();
      final BufferedDataTable population = progress.table(spec, parentRows());

      final PortObject[] tracked = progress.execute(population);
      final BufferedDataTable summary = (BufferedDataTable) tracked[1];
      final BufferedDataTable events = (BufferedDataTable) tracked[2];
      assertEquals(1, summary.size());
      assertEquals(population.size(), events.size());

      final PortObject[] analyzed = analysis.execute(summary);
      assertEquals(1, ((BufferedDataTable) analyzed[0]).size());
      assertTrue(((BufferedDataTable) analyzed[1]).size() >= 1);
      assertEquals(1, ((BufferedDataTable) analyzed[2]).size());

      assertEquals(0, convergence.execute(analyzed[0]).length);
      assertEquals(0, ecdf.execute(analyzed[1]).length);
    }
  }

  @Test
  void migrationRejectsOneIslandAndMigratesBetweenTwoIslands() throws Exception {
    try (NodeTestHarness migration = new NodeTestHarness(new PopulationMigrationNodeFactory())) {
      final DataTableSpec spec = TestPopulation.spec();
      final BufferedDataTable oneIsland = migration.table(spec, parentRows());
      assertThrows(InvalidSettingsException.class, () -> migration.execute(oneIsland));

      final NodeSettings settings = migration.settings();
      settings.addInt("migration_interval", 1);
      migration.loadSettings(settings);
      final List<DataCell[]> islands = new ArrayList<>();
      islands.add(withPopulation(TestPopulation.row("a1", -2.0, 4.0, 9.0, 4, 0.0), "A"));
      islands.add(withPopulation(TestPopulation.row("a2", -1.0, 1.0, 8.0, 4, 0.0), "A"));
      islands.add(withPopulation(TestPopulation.row("b1", 1.0, 8.0, 1.0, 4, 0.0), "B"));
      islands.add(withPopulation(TestPopulation.row("b2", 2.0, 9.0, 4.0, 4, 0.0), "B"));

      final BufferedDataTable output =
          (BufferedDataTable) migration.execute(migration.table(spec, islands))[0];

      assertEquals(4, output.size());
      int migrated = 0;
      for (DataRow row : output) {
        if (PopulationState.individual(row, spec).contains(":from:")) migrated++;
      }
      assertEquals(2, migrated);
    }
  }

  private static List<DataCell[]> parentRows() {
    return List.of(
        TestPopulation.row("parent-1", -2.0, 4.0, 9.0, 2, 0.0),
        TestPopulation.row("parent-2", 2.0, 9.0, 4.0, 2, 0.0));
  }

  private static List<DataCell[]> trialRows() {
    return List.of(
        TestPopulation.row("trial-1", -1.0, 1.0, 8.0, 4, 0.0),
        TestPopulation.row("trial-2", 1.0, 8.0, 1.0, 4, 0.0));
  }

  private static DataCell[] withPopulation(final DataCell[] cells, final String population) {
    final DataCell[] copy = cells.clone();
    final IdeaFlowState state = ((IdeaFlowStateCell) copy[copy.length - 1]).state();
    copy[copy.length - 1] =
        new IdeaFlowStateCell(state.with(IdeaFlowState.POPULATION, population));
    return copy;
  }
}
