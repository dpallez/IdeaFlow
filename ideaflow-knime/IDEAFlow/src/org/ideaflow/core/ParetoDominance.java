package org.ideaflow.core;

import java.util.List;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.ObjectiveDefinition;

/** Deb constraint dominance followed by Pareto dominance. */
public final class ParetoDominance {
  private ParetoDominance() {}

  public static int compare(
      final Candidate left, final Candidate right, final List<ObjectiveDefinition> objectives) {
    final double lv = left.constraintViolation();
    final double rv = right.constraintViolation();
    if (lv == 0.0 && rv > 0.0) return -1;
    if (lv > 0.0 && rv == 0.0) return 1;
    if (lv > 0.0 && rv > 0.0) return Double.compare(lv, rv);

    final double[] a = left.objectives();
    final double[] b = right.objectives();
    if (a.length != objectives.size() || b.length != objectives.size()) {
      throw new IllegalArgumentException(
          "Candidate objective count does not match problem definition.");
    }
    boolean leftBetter = false;
    boolean rightBetter = false;
    for (int i = 0; i < objectives.size(); i++) {
      requireFinite(a[i]);
      requireFinite(b[i]);
      final int comparison =
          Double.compare(
              objectives.get(i).direction().normalize(a[i]),
              objectives.get(i).direction().normalize(b[i]));
      leftBetter |= comparison < 0;
      rightBetter |= comparison > 0;
    }
    if (leftBetter == rightBetter) return 0;
    return leftBetter ? -1 : 1;
  }

  private static void requireFinite(final double value) {
    if (!Double.isFinite(value)) throw new IllegalArgumentException("Objectives must be finite.");
  }
}
