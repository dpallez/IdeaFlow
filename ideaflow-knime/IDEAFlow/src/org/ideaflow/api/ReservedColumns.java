package org.ideaflow.api;

/** Small set of meaningful columns that may be visible in IdeaFlow tables. */
public final class ReservedColumns {
  public static final String CONSTRAINT_VIOLATION = "Constraint violation";
  public static final String PARETO_RANK = "Pareto rank";
  public static final String CROWDING_DISTANCE = "Crowding distance";
  public static final String HYPERVOLUME = "Hypervolume";
  public static final String NFE = "NFE";

  private ReservedColumns() {}

  public static boolean isReserved(final String columnName) {
    return columnName != null
        && java.util.Set.of(
                CONSTRAINT_VIOLATION,
                PARETO_RANK,
                CROWDING_DISTANCE,
                HYPERVOLUME,
                NFE,
                "IdeaFlow state",
                "Feasible")
            .contains(columnName);
  }
}
