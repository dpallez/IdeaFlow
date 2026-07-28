package org.ideaflow.core;

/** Computes the L-SHADE population size at a given evaluation count. */
public final class LinearPopulationSchedule {
  private LinearPopulationSchedule() {}

  public static int sizeAt(
      final int initialSize,
      final int minimumSize,
      final long evaluations,
      final long maximumEvaluations) {
    if (initialSize < minimumSize
        || minimumSize < 2
        || maximumEvaluations <= 0
        || evaluations < 0) {
      throw new IllegalArgumentException("Invalid population schedule.");
    }
    final double progress = Math.min(1.0, (double) evaluations / maximumEvaluations);
    return Math.max(
        minimumSize, (int) Math.round(initialSize + progress * (minimumSize - initialSize)));
  }
}
