package org.ideaflow.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

final class EncodingAndRandomTest {
  @Test
  void decodesEndpointsAndMidpoints() {
    assertEquals(-5.0, BinaryEncoding.decode(0, 2, -5.0, 5.0), 1.0e-12);
    assertEquals(5.0, BinaryEncoding.decode(3, 2, -5.0, 5.0), 1.0e-12);
    assertEquals(-5.0 + 20.0 / 3.0, BinaryEncoding.decode(2, 2, -5.0, 5.0), 1.0e-12);
    assertEquals("x_bit7", BinaryEncoding.geneName("x", 7));
  }

  @Test
  void rejectsInvalidEncodingInputs() {
    assertThrows(IllegalArgumentException.class, () -> BinaryEncoding.decode(0, 0, 0, 1));
    assertThrows(IllegalArgumentException.class, () -> BinaryEncoding.decode(0, 53, 0, 1));
    assertThrows(IllegalArgumentException.class, () -> BinaryEncoding.decode(4, 2, 0, 1));
    assertThrows(IllegalArgumentException.class, () -> BinaryEncoding.decode(0, 2, 1, 1));
  }

  @Test
  void createsRepeatableIndependentRandomScopes() {
    final RandomGenerator first = DeterministicRandom.forScope(42, "mutation", 3);
    final RandomGenerator second = DeterministicRandom.forScope(42, "mutation", 3);
    final RandomGenerator different = DeterministicRandom.forScope(42, "mutation", 4);

    for (int index = 0; index < 10; index++) assertEquals(first.nextLong(), second.nextLong());
    assertNotEquals(
        DeterministicRandom.forScope(42, "mutation", 3).nextLong(), different.nextLong());
  }

  @Test
  void clampsValuesAtBothBounds() {
    final ClampBoundsRepair repair = new ClampBoundsRepair();
    final RandomGenerator random = DeterministicRandom.forScope(1, "repair");

    assertEquals(-1.0, repair.repair(-3.0, -1.0, 2.0, random));
    assertEquals(2.0, repair.repair(3.0, -1.0, 2.0, random));
    assertEquals(0.5, repair.repair(0.5, -1.0, 2.0, random));
  }
}
