package org.ideaflow.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.ObjectiveDefinition;

/** Computes NSGA-II crowding distances within each Pareto front. */
public final class CrowdingDistance {
  private CrowdingDistance() {}

  public static double[] compute(
      final List<Candidate> population,
      final List<List<Integer>> fronts,
      final List<ObjectiveDefinition> objectives) {
    final double[] result = new double[population.size()];
    for (List<Integer> front : fronts) {
      if (front.size() <= 2) {
        front.forEach(index -> result[index] = Double.POSITIVE_INFINITY);
        continue;
      }
      for (int objective = 0; objective < objectives.size(); objective++) {
        final int objectiveIndex = objective;
        final List<Integer> ordered = new ArrayList<>(front);
        ordered.sort(
            Comparator.comparingDouble(
                index ->
                    objectives
                        .get(objectiveIndex)
                        .direction()
                        .normalize(population.get(index).objectives()[objectiveIndex])));
        result[ordered.get(0)] = Double.POSITIVE_INFINITY;
        result[ordered.get(ordered.size() - 1)] = Double.POSITIVE_INFINITY;
        final double min =
            objectives
                .get(objective)
                .direction()
                .normalize(population.get(ordered.get(0)).objectives()[objective]);
        final double max =
            objectives
                .get(objective)
                .direction()
                .normalize(population.get(ordered.get(ordered.size() - 1)).objectives()[objective]);
        if (max == min) continue;
        for (int i = 1; i < ordered.size() - 1; i++) {
          if (Double.isInfinite(result[ordered.get(i)])) continue;
          final double previous =
              objectives
                  .get(objective)
                  .direction()
                  .normalize(population.get(ordered.get(i - 1)).objectives()[objective]);
          final double next =
              objectives
                  .get(objective)
                  .direction()
                  .normalize(population.get(ordered.get(i + 1)).objectives()[objective]);
          result[ordered.get(i)] += (next - previous) / (max - min);
        }
      }
    }
    return result;
  }
}
