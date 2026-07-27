package org.ideaflow.api;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record ProblemDefinition(
        String problemId,
        List<VariableDefinition> variables,
        List<ObjectiveDefinition> objectives,
        List<ConstraintDefinition> constraints) {

    public ProblemDefinition {
        if (problemId == null || problemId.isBlank()) {
            throw new IllegalArgumentException("Problem ID is required.");
        }
        variables = List.copyOf(variables == null ? List.of() : variables);
        objectives = List.copyOf(objectives == null ? List.of() : objectives);
        constraints = List.copyOf(constraints == null ? List.of() : constraints);
        if (objectives.isEmpty()) {
            throw new IllegalArgumentException("At least one objective is required.");
        }
        final Set<String> names = new HashSet<>();
        variables.forEach(v -> requireUnique(names, v.name()));
        objectives.forEach(o -> requireUnique(names, o.column()));
        constraints.forEach(c -> requireUnique(names, c.column()));
    }

    private static void requireUnique(final Set<String> names, final String name) {
        if (!names.add(name)) {
            throw new IllegalArgumentException("Duplicate problem column: " + name);
        }
    }
}
