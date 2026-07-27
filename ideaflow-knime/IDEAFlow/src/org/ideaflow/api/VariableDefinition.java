package org.ideaflow.api;

import java.util.List;
import java.util.Objects;

public record VariableDefinition(
        String name,
        VariableType type,
        Double lowerBound,
        Double upperBound,
        List<String> categories,
        String encodingGroup) {

    public VariableDefinition {
        if (name == null || name.isBlank() || ReservedColumns.isReserved(name)) {
            throw new IllegalArgumentException("Variable name is missing or reserved: " + name);
        }
        Objects.requireNonNull(type, "Variable type is required.");
        categories = categories == null ? List.of() : List.copyOf(categories);
        encodingGroup = encodingGroup == null ? "" : encodingGroup;
        if ((type == VariableType.REAL || type == VariableType.INTEGER)
                && (lowerBound == null || upperBound == null || lowerBound > upperBound)) {
            throw new IllegalArgumentException("Numeric variable requires valid bounds: " + name);
        }
        if (type == VariableType.CATEGORICAL && categories.isEmpty()) {
            throw new IllegalArgumentException("Categorical variable requires categories: " + name);
        }
    }
}
