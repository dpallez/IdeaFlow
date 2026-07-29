package org.ideaflow.nodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.ideaflow.knime.KnimeTableSupport;
import org.ideaflow.nodes.loop.end.OptimizationLoopEndNodeFactory;
import org.ideaflow.nodes.loop.end.OptimizationLoopEndNodeModel;
import org.ideaflow.nodes.loop.start.OptimizationLoopStartNodeFactory;
import org.ideaflow.testing.NodeTestHarness;
import org.ideaflow.testing.TestPopulation;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.LongValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;

final class LoopArchiveFeedbackNodeTest {
  @Test
  void loopStartCreatesACompatibleEmptyArchiveWhenNoneIsConnected() throws Exception {
    try (NodeTestHarness start = new NodeTestHarness(new OptimizationLoopStartNodeFactory())) {
      final DataTableSpec spec = TestPopulation.spec();
      start.configure(spec, null);
      final BufferedDataTable population = start.table(spec, populationRows(1));

      final PortObject[] output = start.execute(population, null);

      assertEquals(population.size(), ((BufferedDataTable) output[0]).size());
      assertEquals(0, ((BufferedDataTable) output[1]).size());
      assertSameColumns(spec, ((BufferedDataTable) output[1]).getDataTableSpec());
    }
  }

  @Test
  void loopStartReturnsTheConnectedInitialArchive() throws Exception {
    try (NodeTestHarness start = new NodeTestHarness(new OptimizationLoopStartNodeFactory())) {
      final DataTableSpec spec = TestPopulation.spec();
      final BufferedDataTable population = start.table(spec, populationRows(1));
      final BufferedDataTable archive =
          start.table(spec, List.<DataCell[]>of(TestPopulation.row("archive", 4.0, 16.0, 1.0, 1, 0.0)));

      final PortObject[] output = start.execute(population, archive);

      assertEquals(1, ((BufferedDataTable) output[1]).size());
      assertSameColumns(spec, ((BufferedDataTable) output[1]).getDataTableSpec());
    }
  }

  @Test
  void loopStartRejectsAnInitialArchiveWithAnotherSchema() {
    try (NodeTestHarness start = new NodeTestHarness(new OptimizationLoopStartNodeFactory())) {
      final DataTableSpec incompatible =
          new DataTableSpec(KnimeTableSupport.doubleColumn("another-column"));

      assertThrows(
          InvalidSettingsException.class,
          () -> start.configure(TestPopulation.spec(), incompatible));
    }
  }

  @Test
  void loopEndReturnsAndResetsTheFinalArchive() throws Exception {
    try (NodeTestHarness start = new NodeTestHarness(new OptimizationLoopStartNodeFactory());
        NodeTestHarness end = new NodeTestHarness(new OptimizationLoopEndNodeFactory())) {
      start.node().setLoopEndNode(end.node());
      end.node().setLoopStartNode(start.node());
      final DataTableSpec spec = TestPopulation.spec();
      final BufferedDataTable population = start.table(spec, populationRows(100));
      final BufferedDataTable archive =
          start.table(spec, List.<DataCell[]>of(TestPopulation.row("archive", 4.0, 16.0, 1.0, 1_000, 0.0)));
      start.execute(population, null);

      final PortObject[] output = end.execute(population, null, archive);

      assertEquals(population.size(), ((BufferedDataTable) output[0]).size());
      assertEquals(1, ((BufferedDataTable) output[1]).size());
      assertEquals(1, ((BufferedDataTable) output[3]).size());
      final DataRow summary = ((BufferedDataTable) output[1]).iterator().next();
      assertEquals(100, ((LongValue) summary.getCell(2)).getLongValue());
      assertSameColumns(spec, ((BufferedDataTable) output[3]).getDataTableSpec());

      final OptimizationLoopEndNodeModel model =
          (OptimizationLoopEndNodeModel) end.node().getNodeModel();
      assertEquals(1, model.feedbackArchive().size());
      end.node().reset();
      assertThrows(IllegalStateException.class, model::feedbackPopulation);
      assertThrows(IllegalStateException.class, model::feedbackArchive);
    }
  }

  private static List<DataCell[]> populationRows(final long nfe) {
    return List.of(
        TestPopulation.row("one", -2.0, 4.0, 9.0, nfe, 0.0),
        TestPopulation.row("two", 1.0, 1.0, 4.0, nfe, 0.0));
  }

  private static void assertSameColumns(
      final DataTableSpec expected, final DataTableSpec actual) {
    assertEquals(expected.getNumColumns(), actual.getNumColumns());
    for (int column = 0; column < expected.getNumColumns(); column++) {
      assertEquals(expected.getColumnSpec(column).getName(), actual.getColumnSpec(column).getName());
      assertEquals(expected.getColumnSpec(column).getType(), actual.getColumnSpec(column).getType());
    }
  }
}
