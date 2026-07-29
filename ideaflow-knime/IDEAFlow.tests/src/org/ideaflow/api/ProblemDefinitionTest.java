package org.ideaflow.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ProblemDefinitionTest {
  @Test
  void copiesAllDefinitionLists() {
    final List<VariableDefinition> variables = new ArrayList<>();
    variables.add(new VariableDefinition("x", VariableType.REAL, -1.0, 1.0, null, null));
    final List<ObjectiveDefinition> objectives =
        new ArrayList<>(
            List.of(new ObjectiveDefinition("fitness", OptimizationDirection.MINIMIZE, null, null)));

    final ProblemDefinition problem = new ProblemDefinition("problem", variables, objectives, null);
    variables.clear();
    objectives.clear();

    assertEquals(1, problem.variables().size());
    assertEquals(1, problem.objectives().size());
    assertThrows(
        UnsupportedOperationException.class,
        () -> problem.variables().add(new VariableDefinition("y", VariableType.BINARY, null, null, null, null)));
  }

  @Test
  void rejectsDuplicateColumnsAcrossRoles() {
    final VariableDefinition variable =
        new VariableDefinition("value", VariableType.REAL, 0.0, 1.0, null, null);
    final ObjectiveDefinition objective =
        new ObjectiveDefinition("value", OptimizationDirection.MINIMIZE, null, null);

    assertThrows(
        IllegalArgumentException.class,
        () -> new ProblemDefinition("problem", List.of(variable), List.of(objective), List.of()));
  }

  @Test
  void validatesVariableRepresentations() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new VariableDefinition("x", VariableType.REAL, 2.0, 1.0, null, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new VariableDefinition("category", VariableType.CATEGORICAL, null, null, List.of(), null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new VariableDefinition(ReservedColumns.NFE, VariableType.INTEGER, 0.0, 1.0, null, null));
  }

  @Test
  void calculatesConstraintViolationForEveryRelation() {
    final ConstraintDefinition lessOrEqual =
        new ConstraintDefinition("le", ConstraintRelation.LE, 2.0, 0.0);
    final ConstraintDefinition greaterOrEqual =
        new ConstraintDefinition("ge", ConstraintRelation.GE, 2.0, 0.0);
    final ConstraintDefinition equality =
        new ConstraintDefinition("eq", ConstraintRelation.EQ, 2.0, 0.25);

    assertEquals(1.0, lessOrEqual.violation(3.0), 1.0e-12);
    assertEquals(1.0, greaterOrEqual.violation(1.0), 1.0e-12);
    assertEquals(0.0, equality.violation(2.2), 1.0e-12);
    assertEquals(0.75, equality.violation(3.0), 1.0e-12);
  }

  @Test
  void restrictsToleranceToEqualityConstraints() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ConstraintDefinition("constraint", ConstraintRelation.LE, 0.0, 0.1));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ConstraintDefinition("constraint", ConstraintRelation.EQ, 0.0, -0.1));
  }

  @Test
  void requiresAtLeastOneObjective() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ProblemDefinition("problem", List.of(), List.of(), List.of()));
  }
}
