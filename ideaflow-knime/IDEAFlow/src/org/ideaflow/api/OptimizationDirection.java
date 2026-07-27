package org.ideaflow.api;

public enum OptimizationDirection {
    MINIMIZE,
    MAXIMIZE;

    public double normalize(final double value) {
        return this == MINIMIZE ? value : -value;
    }

    public static OptimizationDirection parse(final String value) {
        if (value == null) {
            throw new IllegalArgumentException("Objective direction is required.");
        }
        final String normalized = value.trim().toLowerCase();
        if (normalized.equals("min") || normalized.equals("minimize") || normalized.equals("minimise")) {
            return MINIMIZE;
        }
        if (normalized.equals("max") || normalized.equals("maximize") || normalized.equals("maximise")) {
            return MAXIMIZE;
        }
        throw new IllegalArgumentException("Unsupported objective direction: " + value);
    }
}
