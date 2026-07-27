package org.ideaflow.api;

import java.util.Objects;

public record ObjectiveDefinition(
        String column,
        OptimizationDirection direction,
        Double target,
        Double referencePoint) {

    public ObjectiveDefinition {
        if (column == null || column.isBlank()) {
            throw new IllegalArgumentException("Objective column is required.");
        }
        Objects.requireNonNull(direction, "Objective direction is required.");
    }
}
