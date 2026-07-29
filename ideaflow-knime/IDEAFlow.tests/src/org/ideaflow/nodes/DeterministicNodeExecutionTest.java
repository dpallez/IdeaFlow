package org.ideaflow.nodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.ideaflow.api.ReservedColumns;
import org.ideaflow.knime.KnimeTableSupport;
import org.ideaflow.knime.PopulationState;
import org.ideaflow.nodes.metrics.CompareParetoFrontsNodeFactory;
import org.ideaflow.nodes.multiobjective.RankParetoSolutionsNodeFactory;
import org.ideaflow.nodes.reference.ReferenceDirectionsNodeFactory;
import org.ideaflow.nodes.trace.RecordPopulationNodeFactory;
import org.ideaflow.testing.NodeTestHarness;
import org.ideaflow.testing.TestPopulation;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;

final class DeterministicNodeExecutionTest {
  @Test
  void referenceDirectionsConfigureAndExecuteFromProblemMetadata() throws Exception {
    try (NodeTestHarness harness = new NodeTestHarness(new ReferenceDirectionsNodeFactory())) {
      final DataTableSpec inputSpec = TestPopulation.spec();
      final DataTableSpec outputSpec = (DataTableSpec) harness.configure(inputSpec)[0];
      final BufferedDataTable input = harness.table(inputSpec, List.of());
      final BufferedDataTable output = (BufferedDataTable) harness.execute(input)[0];

      assertEquals(3, outputSpec.getNumColumns());
      assertEquals(13, output.size());
      for (DataRow row : output) {
        assertEquals(
            1.0,
            ((DoubleValue) row.getCell(1)).getDoubleValue()
                + ((DoubleValue) row.getCell(2)).getDoubleValue(),
            1.0e-12);
      }
    }
  }

  @Test
  void referenceDirectionsRejectMissingProblemMetadata() {
    try (NodeTestHarness harness = new NodeTestHarness(new ReferenceDirectionsNodeFactory())) {
      assertThrows(
          InvalidSettingsException.class,
          () -> harness.configure(new DataTableSpec(KnimeTableSupport.doubleColumn("x"))));
    }
  }

  @Test
  void paretoRankingAddsRanksAndCrowdingWithoutDroppingPopulationColumns() throws Exception {
    final List<DataCell[]> rows = populationRows();
    try (NodeTestHarness harness = new NodeTestHarness(new RankParetoSolutionsNodeFactory())) {
      final DataTableSpec inputSpec = TestPopulation.spec();
      final DataTableSpec configured = (DataTableSpec) harness.configure(inputSpec)[0];
      final BufferedDataTable input = harness.table(inputSpec, rows);
      final BufferedDataTable output = (BufferedDataTable) harness.execute(input)[0];

      assertEquals(inputSpec.getNumColumns() + 2, configured.getNumColumns());
      assertEquals(inputSpec.getNumColumns() + 2, output.getDataTableSpec().getNumColumns());
      final int rank = output.getDataTableSpec().findColumnIndex(ReservedColumns.PARETO_RANK);
      final int crowding =
          output.getDataTableSpec().findColumnIndex(ReservedColumns.CROWDING_DISTANCE);
      final List<DataRow> outputRows = rows(output);
      assertEquals(List.of(0, 0, 0, 1), outputRows.stream().map(row -> ((IntValue) row.getCell(rank)).getIntValue()).toList());
      assertTrue(((DoubleValue) outputRows.get(0).getCell(crowding)).getDoubleValue() > 0.0);
    }
  }

  @Test
  void populationRecorderPassesThroughInputAndCreatesANumericTrace() throws Exception {
    try (NodeTestHarness harness = new NodeTestHarness(new RecordPopulationNodeFactory())) {
      final DataTableSpec inputSpec = TestPopulation.spec();
      final BufferedDataTable input = harness.table(inputSpec, populationRows());
      final PortObject[] output = harness.execute(input);
      final BufferedDataTable trace = (BufferedDataTable) output[1];

      assertEquals(input.size(), ((BufferedDataTable) output[0]).size());
      final DataTableSpec outputSpec = ((BufferedDataTable) output[0]).getDataTableSpec();
      assertEquals(inputSpec.getNumColumns(), outputSpec.getNumColumns());
      for (int column = 0; column < inputSpec.getNumColumns(); column++) {
        assertEquals(inputSpec.getColumnSpec(column).getName(), outputSpec.getColumnSpec(column).getName());
        assertEquals(inputSpec.getColumnSpec(column).getType(), outputSpec.getColumnSpec(column).getType());
      }
      assertEquals(input.size(), trace.size());
      assertEquals("Run", trace.getDataTableSpec().getColumnSpec(0).getName());
      assertEquals("Stage", trace.getDataTableSpec().getColumnSpec(4).getName());
      assertTrue(trace.getDataTableSpec().findColumnIndex("f1") >= 0);
      assertTrue(trace.getDataTableSpec().findColumnIndex("f2") >= 0);
      assertTrue(
          trace.getDataTableSpec().findColumnIndex(PopulationState.CONSTRAINT_VIOLATION) >= 0);
    }
  }

  @Test
  void comparingAFrontWithItselfProducesZeroDistanceIndicators() throws Exception {
    try (NodeTestHarness harness = new NodeTestHarness(new CompareParetoFrontsNodeFactory())) {
      final DataTableSpec approximationSpec = TestPopulation.spec();
      final BufferedDataTable approximation = harness.table(approximationSpec, populationRows().subList(0, 3));
      final DataTableSpec referenceSpec =
          new DataTableSpec(
              TestPopulation.spec().getColumnSpec("f1"), TestPopulation.spec().getColumnSpec("f2"));
      final BufferedDataTable reference =
          harness.table(
              referenceSpec,
              List.of(
                  new DataCell[] {new DoubleCell(0), new DoubleCell(2)},
                  new DataCell[] {new DoubleCell(1), new DoubleCell(1)},
                  new DataCell[] {new DoubleCell(2), new DoubleCell(0)}));

      final BufferedDataTable output =
          (BufferedDataTable) harness.execute(approximation, reference)[0];
      final DataRow result = output.iterator().next();

      assertEquals(1, output.size());
      for (int column = 3; column <= 6; column++) {
        assertEquals(0.0, ((DoubleValue) result.getCell(column)).getDoubleValue(), 1.0e-12);
      }
    }
  }

  private static List<DataCell[]> populationRows() {
    return List.of(
        TestPopulation.row("a", 0, 0, 2, 4, 0),
        TestPopulation.row("b", 1, 1, 1, 4, 0),
        TestPopulation.row("c", 2, 2, 0, 4, 0),
        TestPopulation.row("d", 3, 2, 2, 4, 0));
  }

  private static List<DataRow> rows(final BufferedDataTable table) {
    final List<DataRow> result = new ArrayList<>();
    table.forEach(result::add);
    return result;
  }

}
