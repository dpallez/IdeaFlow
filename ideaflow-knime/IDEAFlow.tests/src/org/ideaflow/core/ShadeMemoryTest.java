package org.ideaflow.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Checks the success-history update rules used by SHADE and L-SHADE. */
final class ShadeMemoryTest {
  @Test
  void usesImprovementWeightedLehmerAndArithmeticMeans() {
    final ShadeMemory.State initial = ShadeMemory.initial(2, 0.5, 0.5);
    final ShadeMemory.State updated =
        ShadeMemory.update(
            initial,
            List.of(
                new ShadeMemory.Success(0.5, 0.2, 1.0), new ShadeMemory.Success(1.0, 0.8, 3.0)));

    assertArrayEquals(new double[] {3.25 / 3.5, 0.5}, updated.f(), 1.0e-12);
    assertArrayEquals(new double[] {0.65, 0.5}, updated.cr(), 1.0e-12);
    assertEquals(1, updated.index());
  }

  @Test
  void noSuccessLeavesMemorySlotAndIndexUntouched() {
    final ShadeMemory.State initial = ShadeMemory.initial(3, 0.6, 0.4);
    final ShadeMemory.State updated = ShadeMemory.update(initial, List.of());

    assertArrayEquals(initial.f(), updated.f());
    assertArrayEquals(initial.cr(), updated.cr());
    assertEquals(initial.index(), updated.index());
  }
}
