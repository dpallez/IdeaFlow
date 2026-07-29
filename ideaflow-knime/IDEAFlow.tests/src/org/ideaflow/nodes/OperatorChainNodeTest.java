package org.ideaflow.nodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.ideaflow.api.IdeaFlowState;
import org.ideaflow.knime.PopulationState;
import org.ideaflow.nodes.selection.SelectionNodeFactory;
import org.ideaflow.nodes.variation.mutation.MutationNodeFactory;
import org.ideaflow.nodes.variation.recombination.CrossoverNodeFactory;
import org.ideaflow.testing.NodeTestHarness;
import org.ideaflow.testing.TestPopulation;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeSettings;

final class OperatorChainNodeTest {
  @Test
  void seededSelectionCrossoverAndMutationProduceBoundedUnevaluatedCandidates()
      throws Exception {
    try (NodeTestHarness selection = new NodeTestHarness(new SelectionNodeFactory());
        NodeTestHarness crossover = new NodeTestHarness(new CrossoverNodeFactory());
        NodeTestHarness mutation = new NodeTestHarness(new MutationNodeFactory())) {
      final NodeSettings selectionSettings = selection.settings();
      selectionSettings.addInt("parent_count", 4);
      selection.loadSettings(selectionSettings);

      final DataTableSpec spec = TestPopulation.spec();
      selection.configure(spec, null);
      crossover.configure(spec);
      mutation.configure(spec);
      final BufferedDataTable population = selection.table(spec, populationRows());
      final BufferedDataTable parents =
          (BufferedDataTable) selection.execute(population, null)[0];
      final BufferedDataTable children = (BufferedDataTable) crossover.execute(parents)[0];
      final BufferedDataTable mutants = (BufferedDataTable) mutation.execute(children)[0];

      assertEquals(4, parents.size());
      assertEquals(4, children.size());
      assertEquals(4, mutants.size());
      final int variable = spec.findColumnIndex("x");
      final int firstObjective = spec.findColumnIndex("f1");
      for (DataRow row : mutants) {
        final double value = ((DoubleValue) row.getCell(variable)).getDoubleValue();
        assertTrue(value >= -5.0 && value <= 5.0);
        assertTrue(row.getCell(firstObjective).isMissing());
        assertFalse(
            PopulationState.get(row, spec).booleanValue(IdeaFlowState.EVALUATED, true));
      }
    }
  }

  @Test
  void seededOperatorChainIsRepeatable() throws Exception {
    assertEquals(runOperatorChain(), runOperatorChain());
  }

  private static List<Double> runOperatorChain() throws Exception {
    try (NodeTestHarness selection = new NodeTestHarness(new SelectionNodeFactory());
        NodeTestHarness crossover = new NodeTestHarness(new CrossoverNodeFactory());
        NodeTestHarness mutation = new NodeTestHarness(new MutationNodeFactory())) {
      final NodeSettings selectionSettings = selection.settings();
      selectionSettings.addInt("parent_count", 4);
      selection.loadSettings(selectionSettings);
      final DataTableSpec spec = TestPopulation.spec();
      final BufferedDataTable population = selection.table(spec, populationRows());
      final BufferedDataTable parents =
          (BufferedDataTable) selection.execute(population, null)[0];
      final BufferedDataTable children = (BufferedDataTable) crossover.execute(parents)[0];
      final BufferedDataTable mutants = (BufferedDataTable) mutation.execute(children)[0];
      final int variable = spec.findColumnIndex("x");
      final List<Double> values = new ArrayList<>();
      for (DataRow row : mutants) {
        values.add(((DoubleValue) row.getCell(variable)).getDoubleValue());
      }
      return List.copyOf(values);
    }
  }

  private static List<DataCell[]> populationRows() {
    return List.of(
        TestPopulation.row("a", -4, 0, 3, 4, 0),
        TestPopulation.row("b", -1, 1, 2, 4, 0),
        TestPopulation.row("c", 1, 2, 1, 4, 0),
        TestPopulation.row("d", 4, 3, 0, 4, 0));
  }
}
