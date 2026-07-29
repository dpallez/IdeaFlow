package org.ideaflow.nodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.ideaflow.nodes.analysis.convergence.ConvergencePlotNodeFactory;
import org.ideaflow.nodes.analysis.ecdf.EcdfPlotNodeFactory;
import org.ideaflow.nodes.analysis.run.OptimizationRunAnalysisNodeFactory;
import org.ideaflow.nodes.benchmark.EvaluationNodeFactory;
import org.ideaflow.nodes.initialization.InitialPopulationNodeFactory;
import org.ideaflow.nodes.io.ExportToIohProfilerNodeFactory;
import org.ideaflow.nodes.loop.end.OptimizationLoopEndNodeFactory;
import org.ideaflow.nodes.loop.start.OptimizationLoopStartNodeFactory;
import org.ideaflow.nodes.metrics.CompareParetoFrontsNodeFactory;
import org.ideaflow.nodes.migration.PopulationMigrationNodeFactory;
import org.ideaflow.nodes.monitor.TrackProgressNodeFactory;
import org.ideaflow.nodes.multiobjective.RankParetoSolutionsNodeFactory;
import org.ideaflow.nodes.problem.ProblemSetupNodeFactory;
import org.ideaflow.nodes.reference.ReferenceDirectionsNodeFactory;
import org.ideaflow.nodes.selection.SelectionNodeFactory;
import org.ideaflow.nodes.trace.RecordPopulationNodeFactory;
import org.ideaflow.nodes.update.ElitismNodeFactory;
import org.ideaflow.nodes.variation.mutation.MutationNodeFactory;
import org.ideaflow.nodes.variation.recombination.CrossoverNodeFactory;
import org.ideaflow.testing.NodeTestHarness;
import org.junit.jupiter.api.Test;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.Node;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;

final class RegisteredNodeFactoryTest {
  private record Contract(
      NodeFactory<? extends NodeModel> factory, int inputPorts, int outputPorts) {}

  @Test
  void everyRegisteredFactoryCreatesANodeWithItsPublishedPortContract() {
    final List<Contract> contracts =
        List.of(
            new Contract(new ProblemSetupNodeFactory(), 0, 1),
            new Contract(new InitialPopulationNodeFactory(), 1, 1),
            new Contract(new OptimizationLoopStartNodeFactory(), 2, 2),
            new Contract(new OptimizationLoopEndNodeFactory(), 3, 4),
            new Contract(new SelectionNodeFactory(), 2, 1),
            new Contract(new CrossoverNodeFactory(), 1, 1),
            new Contract(new MutationNodeFactory(), 1, 1),
            new Contract(new ElitismNodeFactory(), 2, 2),
            new Contract(new EvaluationNodeFactory(), 2, 2),
            new Contract(new RankParetoSolutionsNodeFactory(), 1, 1),
            new Contract(new ReferenceDirectionsNodeFactory(), 1, 1),
            new Contract(new CompareParetoFrontsNodeFactory(), 2, 1),
            new Contract(new PopulationMigrationNodeFactory(), 1, 1),
            new Contract(new TrackProgressNodeFactory(), 1, 3),
            new Contract(new OptimizationRunAnalysisNodeFactory(), 1, 3),
            new Contract(new ConvergencePlotNodeFactory(), 1, 0),
            new Contract(new EcdfPlotNodeFactory(), 1, 0),
            new Contract(new RecordPopulationNodeFactory(), 1, 2),
            new Contract(new ExportToIohProfilerNodeFactory(), 1, 1));

    assertEquals(19, contracts.size());
    for (Contract contract : contracts) {
      try (NodeTestHarness harness = new NodeTestHarness(contract.factory())) {
        assertEquals(contract.inputPorts(), tableInputPorts(harness.node()), contract.factory().getClass().getName());
        assertEquals(contract.outputPorts(), tableOutputPorts(harness.node()), contract.factory().getClass().getName());
        assertFalse(harness.node().getName().isBlank());
        assertNotNull(contract.factory().getIcon());
      }
    }
  }

  private static int tableInputPorts(final Node node) {
    int count = 0;
    for (int port = 0; port < node.getNrInPorts(); port++) {
      if (!FlowVariablePortObject.TYPE.equals(node.getInputType(port))
          && !FlowVariablePortObject.TYPE_OPTIONAL.equals(node.getInputType(port))) count++;
    }
    return count;
  }

  private static int tableOutputPorts(final Node node) {
    int count = 0;
    for (int port = 0; port < node.getNrOutPorts(); port++) {
      if (!FlowVariablePortObject.TYPE.equals(node.getOutputType(port))
          && !FlowVariablePortObject.TYPE_OPTIONAL.equals(node.getOutputType(port))) count++;
    }
    return count;
  }

  @Test
  void archiveAndProgressPortsRemainOptional() {
    try (NodeTestHarness selection = new NodeTestHarness(new SelectionNodeFactory());
        NodeTestHarness loopEnd = new NodeTestHarness(new OptimizationLoopEndNodeFactory())) {
      assertEquals(BufferedDataTable.TYPE_OPTIONAL, selection.node().getInputType(1));
      try (NodeTestHarness loopStart = new NodeTestHarness(new OptimizationLoopStartNodeFactory())) {
        assertEquals(BufferedDataTable.TYPE_OPTIONAL, loopStart.node().getInputType(1));
      }
      assertEquals(BufferedDataTable.TYPE_OPTIONAL, loopEnd.node().getInputType(1));
      assertEquals(BufferedDataTable.TYPE_OPTIONAL, loopEnd.node().getInputType(2));
    }
  }
}
