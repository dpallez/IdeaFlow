package org.ideaflow.spi;

import java.util.Set;
import org.ideaflow.api.VariableType;

public record CapabilityDescriptor(
        Set<VariableType> variableTypes,
        int minimumObjectives,
        boolean supportsConstraints,
        Set<String> requiredStateKeys) {

    public CapabilityDescriptor {
        variableTypes = Set.copyOf(variableTypes == null ? Set.of() : variableTypes);
        requiredStateKeys = Set.copyOf(requiredStateKeys == null ? Set.of() : requiredStateKeys);
        if (minimumObjectives < 1) throw new IllegalArgumentException("Minimum objectives must be positive.");
    }

    public static CapabilityDescriptor general() {
        return new CapabilityDescriptor(Set.of(), 1, true, Set.of());
    }
}
