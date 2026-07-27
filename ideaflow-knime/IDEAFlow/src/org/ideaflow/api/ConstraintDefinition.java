package org.ideaflow.api;

public record ConstraintDefinition(
        String column,
        ConstraintRelation relation,
        double threshold,
        double tolerance) {

    public ConstraintDefinition {
        if (column == null || column.isBlank()) {
            throw new IllegalArgumentException("Constraint column is required.");
        }
        if (relation == null) {
            throw new IllegalArgumentException("Constraint relation is required.");
        }
        if (!Double.isFinite(threshold)) {
            throw new IllegalArgumentException("Constraint threshold must be finite.");
        }
        if (!Double.isFinite(tolerance) || tolerance < 0.0) {
            throw new IllegalArgumentException("Constraint tolerance must be finite and non-negative.");
        }
        if (relation != ConstraintRelation.EQ && tolerance != 0.0) {
            throw new IllegalArgumentException("Tolerance is only supported for equality constraints.");
        }
        column = column.trim();
    }

    /** Returns zero for a feasible value and a positive distance for a violation. */
    public double violation(final double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Constraint value must be finite: " + column);
        }
        return switch (relation) {
            case LE -> Math.max(0.0, value - threshold);
            case GE -> Math.max(0.0, threshold - value);
            case EQ -> Math.max(0.0, Math.abs(value - threshold) - tolerance);
        };
    }
}
