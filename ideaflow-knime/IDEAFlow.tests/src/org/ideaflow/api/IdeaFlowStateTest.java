package org.ideaflow.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

final class IdeaFlowStateTest {
  @Test
  void typedValuesUseFallbacksForMalformedContent() {
    final IdeaFlowState state =
        new IdeaFlowState(
            Map.of(
                "long", "invalid",
                "large", Long.toString((long) Integer.MAX_VALUE + 1),
                "double", "NaN",
                "boolean", "true"));

    assertEquals(7L, state.longValue("long", 7L));
    assertEquals(8, state.intValue("large", 8));
    assertEquals(9.0, state.doubleValue("double", 9.0));
    assertTrue(state.booleanValue("boolean", false));
    assertFalse(state.booleanValue("missing", false));
  }

  @Test
  void vectorsRoundTripWithoutSharingArrays() {
    final double[] source = {-1.5, 0.0, 4.25};
    final IdeaFlowState state = IdeaFlowState.empty().withVector("vector", source);
    source[0] = 99.0;

    final double[] decoded = state.vector("vector");
    decoded[1] = 99.0;

    assertArrayEquals(new double[] {-1.5, 0.0, 4.25}, state.vector("vector"));
  }

  @Test
  void malformedVectorsAreIgnored() {
    assertArrayEquals(new double[0], new IdeaFlowState(Map.of("vector", "bad" )).vector("vector"));
  }

  @Test
  void updatesDoNotMutatePreviousStates() {
    final IdeaFlowState initial = IdeaFlowState.empty().with("a", 1L);
    final IdeaFlowState updated = initial.with("b", 2.0).without("a");

    assertEquals("1", initial.text("a", "missing"));
    assertEquals("missing", updated.text("a", "missing"));
    assertEquals(2.0, updated.doubleValue("b", 0.0));
  }
}
