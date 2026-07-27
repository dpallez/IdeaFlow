package org.ideaflow.api;

import java.util.LinkedHashMap;
import java.util.Map;

public record OptimizationEvent(
        String runId,
        String populationId,
        long nfe,
        String stage,
        String operator,
        Map<String, Double> values,
        long elapsedMillis) {

    public OptimizationEvent {
        if (runId == null || runId.isBlank() || populationId == null || populationId.isBlank()) {
            throw new IllegalArgumentException("Run and population IDs are required.");
        }
        if (nfe < 0 || elapsedMillis < 0) {
            throw new IllegalArgumentException("Event counters cannot be negative.");
        }
        stage = stage == null ? "unknown" : stage;
        operator = operator == null ? "unknown" : operator;
        values = Map.copyOf(new LinkedHashMap<>(values == null ? Map.of() : values));
    }
}
