package org.ideaflow.core;

import java.util.List;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.ObjectiveDefinition;

public final class QualityIndicators {
    private QualityIndicators() { }

    public static double generationalDistance(final List<Candidate> approximation,
            final List<Candidate> reference, final List<ObjectiveDefinition> objectives) {
        return averageNearestDistance(approximation, reference, objectives);
    }

    public static double invertedGenerationalDistance(final List<Candidate> approximation,
            final List<Candidate> reference, final List<ObjectiveDefinition> objectives) {
        return averageNearestDistance(reference, approximation, objectives);
    }

    public static double invertedGenerationalDistancePlus(final List<Candidate> approximation,
            final List<Candidate> reference, final List<ObjectiveDefinition> objectives) {
        requireNonEmpty(approximation, reference);
        double sum = 0.0;
        for (Candidate ref : reference) {
            double nearest = Double.POSITIVE_INFINITY;
            for (Candidate candidate : approximation) {
                double squared = 0.0;
                for (int dimension = 0; dimension < objectives.size(); dimension++) {
                    final double candidateValue = objectives.get(dimension).direction().normalize(candidate.objectives()[dimension]);
                    final double referenceValue = objectives.get(dimension).direction().normalize(ref.objectives()[dimension]);
                    final double difference = Math.max(candidateValue - referenceValue, 0.0);
                    squared += difference * difference;
                }
                nearest = Math.min(nearest, Math.sqrt(squared));
            }
            sum += nearest;
        }
        return sum / reference.size();
    }

    /** Coefficient of variation of nearest-neighbour spacing; zero is perfectly even. */
    public static double spacing(final List<Candidate> approximation, final List<ObjectiveDefinition> objectives) {
        if (approximation.size() < 2) return 0.0;
        final double[] nearest = new double[approximation.size()];
        for (int first = 0; first < approximation.size(); first++) {
            nearest[first] = Double.POSITIVE_INFINITY;
            for (int second = 0; second < approximation.size(); second++) if (first != second) {
                double distance = 0.0;
                for (int dimension = 0; dimension < objectives.size(); dimension++) {
                    distance += Math.abs(objectives.get(dimension).direction().normalize(approximation.get(first).objectives()[dimension])
                            - objectives.get(dimension).direction().normalize(approximation.get(second).objectives()[dimension]));
                }
                nearest[first] = Math.min(nearest[first], distance);
            }
        }
        final double mean = java.util.Arrays.stream(nearest).average().orElse(0.0);
        if (mean == 0.0) return 0.0;
        double variance = 0.0; for (double value : nearest) variance += (value - mean) * (value - mean);
        return Math.sqrt(variance / (nearest.length - 1)) / mean;
    }

    public static double additiveEpsilon(final List<Candidate> approximation,
            final List<Candidate> reference, final List<ObjectiveDefinition> objectives) {
        requireNonEmpty(approximation, reference);
        double epsilon = Double.NEGATIVE_INFINITY;
        for (Candidate ref : reference) {
            double bestForRef = Double.POSITIVE_INFINITY;
            for (Candidate candidate : approximation) {
                double worstObjective = Double.NEGATIVE_INFINITY;
                for (int d = 0; d < objectives.size(); d++) {
                    final double a = objectives.get(d).direction().normalize(candidate.objectives()[d]);
                    final double r = objectives.get(d).direction().normalize(ref.objectives()[d]);
                    worstObjective = Math.max(worstObjective, a - r);
                }
                bestForRef = Math.min(bestForRef, worstObjective);
            }
            epsilon = Math.max(epsilon, bestForRef);
        }
        return epsilon;
    }

    private static double averageNearestDistance(final List<Candidate> source,
            final List<Candidate> target, final List<ObjectiveDefinition> objectives) {
        requireNonEmpty(source, target);
        double sum = 0.0;
        for (Candidate point : source) {
            double nearest = Double.POSITIVE_INFINITY;
            for (Candidate candidate : target) {
                double squared = 0.0;
                for (int d = 0; d < objectives.size(); d++) {
                    final double difference = objectives.get(d).direction().normalize(point.objectives()[d])
                            - objectives.get(d).direction().normalize(candidate.objectives()[d]);
                    squared += difference * difference;
                }
                nearest = Math.min(nearest, Math.sqrt(squared));
            }
            sum += nearest;
        }
        return sum / source.size();
    }

    private static void requireNonEmpty(final List<?> first, final List<?> second) {
        if (first.isEmpty() || second.isEmpty()) throw new IllegalArgumentException("Indicator sets cannot be empty.");
    }
}
