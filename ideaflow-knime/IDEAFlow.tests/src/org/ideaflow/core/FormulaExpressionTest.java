package org.ideaflow.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class FormulaExpressionTest {
  @Test
  void respectsOperatorPrecedenceAndRightAssociativePowers() {
    assertEquals(14.0, FormulaExpression.compile("2 + 3 * 4").evaluate(Map.of()), 1.0e-12);
    assertEquals(-4.0, FormulaExpression.compile("-2^2").evaluate(Map.of()), 1.0e-12);
    assertEquals(512.0, FormulaExpression.compile("2^3^2").evaluate(Map.of()), 1.0e-12);
  }

  @Test
  void supportsConstantsFunctionsAndNamedColumns() {
    final FormulaExpression expression =
        FormulaExpression.compile("max(abs([x value]), sqrt($other$)) + cos(pi)");

    assertEquals(3.0, expression.evaluate(Map.of("x value", -2.0, "other", 16.0)), 1.0e-12);
    assertEquals(Set.of("x value", "other"), expression.variables());
  }

  @Test
  void rejectsUnknownFunctionsAndWrongArity() {
    assertThrows(IllegalArgumentException.class, () -> FormulaExpression.compile("unknown(1)"));
    assertThrows(IllegalArgumentException.class, () -> FormulaExpression.compile("min(1)"));
  }

  @Test
  void rejectsMalformedAndMissingInputs() {
    assertThrows(IllegalArgumentException.class, () -> FormulaExpression.compile(""));
    assertThrows(IllegalArgumentException.class, () -> FormulaExpression.compile("(1 + 2"));
    final IllegalArgumentException missing =
        assertThrows(
            IllegalArgumentException.class,
            () -> FormulaExpression.compile("x + 1").evaluate(Map.of()));
    assertTrue(missing.getMessage().contains("x"));
  }

  @Test
  void rejectsNonFiniteResults() {
    assertThrows(
        IllegalArgumentException.class,
        () -> FormulaExpression.compile("1 / 0").evaluate(Map.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> FormulaExpression.compile("sqrt(-1)").evaluate(Map.of()));
  }
}
