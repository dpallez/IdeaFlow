package org.ideaflow.nodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.ideaflow.api.IdeaFlowState;
import org.ideaflow.knime.PopulationState;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.ideaflow.nodes.benchmark.EvaluationNodeFactory;
import org.ideaflow.nodes.initialization.InitialPopulationNodeFactory;
import org.ideaflow.nodes.problem.ProblemSetupNodeFactory;
import org.ideaflow.testing.NodeTestHarness;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObject;

final class ProblemInitializationNodeTest {
  @Test
  void defaultProblemSetupCreatesAReproducibleBoundedPopulation() throws Exception {
    try (NodeTestHarness setup = new NodeTestHarness(new ProblemSetupNodeFactory());
        NodeTestHarness initialization = new NodeTestHarness(new InitialPopulationNodeFactory());
        NodeTestHarness evaluation = new NodeTestHarness(new EvaluationNodeFactory())) {
      setup.configure();
      final PortObject[] setupOutput = setup.execute();
      final BufferedDataTable problem = (BufferedDataTable) setupOutput[0];
      assertTrue(problem.size() >= 2);

      final NodeSettings settings = initialization.settings();
      settings.addInt("population_size", 8);
      initialization.loadSettings(settings);
      final BufferedDataTable first =
          (BufferedDataTable)
              initialization.execute(problem)[0];
      initialization.node().reset();
      final BufferedDataTable second =
          (BufferedDataTable)
              initialization.execute(problem)[0];

      assertEquals(8, first.size());
      assertEquals(values(first), values(second));
      final PortObject[] evaluatedOutput = evaluation.execute(first, problem);
      final BufferedDataTable evaluated = (BufferedDataTable) evaluatedOutput[0];
      final BufferedDataTable events = (BufferedDataTable) evaluatedOutput[1];
      assertEquals(8, evaluated.size());
      assertEquals(8, events.size());
      final String objective =
          ProblemMetadata.require(evaluated.getDataTableSpec()).objectiveNames().get(0);
      final int objectiveColumn = evaluated.getDataTableSpec().findColumnIndex(objective);
      for (DataRow row : evaluated) {
        assertFalse(row.getCell(objectiveColumn).isMissing());
        assertEquals(8, PopulationState.nfe(row, evaluated.getDataTableSpec()));
        assertTrue(
            PopulationState.get(row, evaluated.getDataTableSpec())
                .booleanValue(IdeaFlowState.EVALUATED, false));
      }
      final int firstVariable = first.getDataTableSpec().findColumnIndex("x0");
      assertTrue(firstVariable >= 0);
      for (DataRow row : first) {
        final double value = ((DoubleValue) row.getCell(firstVariable)).getDoubleValue();
        assertTrue(value >= -5.0 && value <= 5.0);
        assertEquals(0, PopulationState.nfe(row, first.getDataTableSpec()));
        assertFalse(
            PopulationState.get(row, first.getDataTableSpec())
                .booleanValue(IdeaFlowState.EVALUATED, true));
      }
    }
  }

  private static List<String> values(final BufferedDataTable table) {
    final List<String> result = new ArrayList<>();
    for (DataRow row : table) result.add(row.toString());
    return result;
  }
}
