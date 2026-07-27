package org.ideaflow.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** A versioned, declarative composition of generic optimization stages. */
public record AlgorithmRecipe(
        String id,
        String version,
        String family,
        List<String> stages,
        Map<String, String> strategies,
        List<String> requirements) {

    public AlgorithmRecipe {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(family, "family");
        stages = List.copyOf(stages);
        strategies = Map.copyOf(strategies);
        requirements = List.copyOf(requirements);
        if (id.isBlank() || version.isBlank() || stages.isEmpty()) {
            throw new IllegalArgumentException("A recipe needs an ID, version, and at least one stage.");
        }
    }
}
