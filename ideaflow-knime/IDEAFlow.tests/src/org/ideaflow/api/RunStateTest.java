package org.ideaflow.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class RunStateTest {
  @Test
  void accountsEvaluationsAndStopsExactlyAtTheBudget() {
    final RunState initial = state(7, 10);
    final RunState continuing = initial.account(2);
    final RunState stopped = continuing.account(1);

    assertEquals(9, continuing.nfe());
    assertFalse(continuing.stopped());
    assertEquals(10, stopped.nfe());
    assertTrue(stopped.stopped());
    assertEquals("max_evaluations", stopped.stopReason());
  }

  @Test
  void clampsAnEvaluationBatchToTheBudget() {
    assertEquals(10, state(7, 10).account(20).nfe());
  }

  @Test
  void rejectsInvalidEvaluationStateAndCounts() {
    assertThrows(IllegalArgumentException.class, () -> state(11, 10));
    assertThrows(IllegalArgumentException.class, () -> state(0, 0));
    assertThrows(IllegalArgumentException.class, () -> state(0, 10).account(-1));
  }

  @Test
  void copiesAndFreezesAlgorithmState() {
    final Map<String, String> algorithm = new LinkedHashMap<>(Map.of("key", "value"));
    final RunState state = new RunState("run", "population", 0, 10, 1, false, null, algorithm);
    algorithm.clear();

    assertEquals("value", state.algorithmState().get("key"));
    assertEquals("continue", state.stopReason());
    assertThrows(UnsupportedOperationException.class, () -> state.algorithmState().put("x", "y"));
  }

  private static RunState state(final long nfe, final long maximum) {
    return new RunState("run", "population", nfe, maximum, 1, false, "continue", Map.of());
  }
}
