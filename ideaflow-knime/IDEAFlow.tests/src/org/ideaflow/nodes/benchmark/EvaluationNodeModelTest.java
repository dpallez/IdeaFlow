package org.ideaflow.nodes.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

final class EvaluationNodeModelTest {

  @Test
  void generatedFormulaCardsFollowProblemSetupOrderAndPreserveExpressions() {
    final List<EvaluationNodeModel.FormulaDefinition> existing =
        List.of(
            new EvaluationNodeModel.FormulaDefinition("obsolete", "x"),
            new EvaluationNodeModel.FormulaDefinition("f2", "x^2"));

    assertEquals(
        List.of(
            new EvaluationNodeModel.FormulaDefinition("f1", ""),
            new EvaluationNodeModel.FormulaDefinition("f2", "x^2")),
        EvaluationNodeParameters.synchronizeFormulaDefinitions(List.of("f1", "f2"), existing));
  }

  @Test
  void objectiveAndConstraintCardsAreSynchronizedIndependently() {
    final List<EvaluationNodeModel.FormulaDefinition> existing =
        List.of(
            new EvaluationNodeModel.FormulaDefinition("objective", "x^2"),
            new EvaluationNodeModel.FormulaDefinition("constraint", "x - 1"));

    assertEquals(
        List.of(new EvaluationNodeModel.FormulaDefinition("constraint", "x - 1")),
        EvaluationNodeParameters.synchronizeFormulaDefinitions(
            List.of("constraint"), existing));
  }
}