package org.ideaflow.api;

import java.util.Arrays;

/** Language-neutral numerical view used by optimization strategies. */
public final class Candidate {
  private final String id;
  private final double[] variables;
  private final double[] objectives;
  private final double constraintViolation;

  public Candidate(
      final String id,
      final double[] variables,
      final double[] objectives,
      final double constraintViolation) {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("Candidate ID is required.");
    }
    if (!Double.isFinite(constraintViolation) || constraintViolation < 0.0) {
      throw new IllegalArgumentException("Constraint violation must be finite and non-negative.");
    }
    this.id = id;
    this.variables = variables == null ? new double[0] : variables.clone();
    this.objectives = objectives == null ? new double[0] : objectives.clone();
    this.constraintViolation = constraintViolation;
  }

  public String id() {
    return id;
  }

  public double[] variables() {
    return variables.clone();
  }

  public double[] objectives() {
    return objectives.clone();
  }

  public double constraintViolation() {
    return constraintViolation;
  }

  @Override
  public String toString() {
    return id + Arrays.toString(objectives);
  }
}
