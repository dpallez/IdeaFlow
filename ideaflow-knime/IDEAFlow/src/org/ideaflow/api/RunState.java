package org.ideaflow.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Serializable logical run state. Algorithm data is namespaced and not GA-specific. */
public record RunState(
    String runId,
    String populationId,
    long nfe,
    long maxEvaluations,
    long startedAtMillis,
    boolean stopped,
    String stopReason,
    Map<String, String> algorithmState) {

  public RunState {
    if (runId == null || runId.isBlank() || populationId == null || populationId.isBlank()) {
      throw new IllegalArgumentException("Run and population IDs are required.");
    }
    if (nfe < 0 || maxEvaluations <= 0 || nfe > maxEvaluations) {
      throw new IllegalArgumentException("Invalid NFE state.");
    }
    stopReason = stopReason == null ? "continue" : stopReason;
    algorithmState =
        Collections.unmodifiableMap(
            new LinkedHashMap<>(algorithmState == null ? Map.of() : algorithmState));
  }

  public RunState account(final long count) {
    if (count < 0) throw new IllegalArgumentException("Evaluation count cannot be negative.");
    final long next = Math.min(maxEvaluations, Math.addExact(nfe, count));
    final boolean budgetReached = next >= maxEvaluations;
    return new RunState(
        runId,
        populationId,
        next,
        maxEvaluations,
        startedAtMillis,
        stopped || budgetReached,
        budgetReached ? "max_evaluations" : stopReason,
        algorithmState);
  }
}
