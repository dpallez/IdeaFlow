package org.ideaflow.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class CandidateTest {
  @Test
  void defensivelyCopiesNumericalArrays() {
    final double[] variables = {1.0, 2.0};
    final double[] objectives = {3.0};
    final Candidate candidate = new Candidate("candidate", variables, objectives, 0.0);

    variables[0] = 99.0;
    objectives[0] = 99.0;
    final double[] returnedVariables = candidate.variables();
    returnedVariables[1] = 99.0;

    assertArrayEquals(new double[] {1.0, 2.0}, candidate.variables());
    assertArrayEquals(new double[] {3.0}, candidate.objectives());
  }

  @Test
  void nullArraysBecomeEmptyArrays() {
    final Candidate candidate = new Candidate("candidate", null, null, 0.0);

    assertEquals(0, candidate.variables().length);
    assertEquals(0, candidate.objectives().length);
  }

  @Test
  void rejectsMissingIdentifiers() {
    assertThrows(IllegalArgumentException.class, () -> new Candidate(" ", null, null, 0.0));
  }

  @Test
  void rejectsInvalidConstraintViolations() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Candidate("candidate", null, null, -1.0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new Candidate("candidate", null, null, Double.NaN));
  }
}
