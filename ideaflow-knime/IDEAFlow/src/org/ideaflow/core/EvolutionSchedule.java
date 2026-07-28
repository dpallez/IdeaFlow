package org.ideaflow.core;

/** Shared generation, migration, and strict evaluation-budget scheduling. */
public final class EvolutionSchedule {
  private EvolutionSchedule() {}

  public static boolean shouldMigrate(final long generation, final int interval) {
    if (interval < 1) throw new IllegalArgumentException("Migration interval must be positive.");
    return generation > 0L && generation % interval == 0L;
  }

  public static boolean canEvaluateBatch(
      final long currentNfe, final long batchSize, final long maximumNfe) {
    if (currentNfe < 0L || batchSize < 0L || maximumNfe < 0L) {
      throw new IllegalArgumentException("Evaluation counts must be non-negative.");
    }
    return currentNfe <= maximumNfe && batchSize <= maximumNfe - currentNfe;
  }
}
