package org.ideaflow.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.ObjectiveDefinition;

/** Deterministic reference-direction environmental selection for NSGA-III recipes. */
public final class Nsga3Selection {
  private Nsga3Selection() {}

  public static List<Integer> select(
      final List<Candidate> population,
      final List<ObjectiveDefinition> objectives,
      final int targetSize,
      final int divisions) {
    if (targetSize < 1 || targetSize > population.size())
      throw new IllegalArgumentException("Invalid target size.");
    final List<List<Integer>> fronts = FastNonDominatedSort.sort(population, objectives);
    final List<Integer> selected = new ArrayList<>();
    List<Integer> splitFront = List.of();
    for (List<Integer> front : fronts) {
      if (selected.size() + front.size() <= targetSize) selected.addAll(front);
      else {
        splitFront = front;
        break;
      }
    }
    if (selected.size() == targetSize) return List.copyOf(selected);
    final List<double[]> directions = ReferenceDirections.dasDennis(objectives.size(), divisions);
    final double[][] normalized = normalize(population, objectives);
    final int[] association = new int[population.size()];
    final double[] distance = new double[population.size()];
    for (int index = 0; index < population.size(); index++) {
      double best = Double.POSITIVE_INFINITY;
      for (int direction = 0; direction < directions.size(); direction++) {
        final double candidateDistance =
            perpendicular(normalized[index], directions.get(direction));
        if (candidateDistance < best) {
          best = candidateDistance;
          association[index] = direction;
        }
      }
      distance[index] = best;
    }
    final int[] nicheCount = new int[directions.size()];
    for (int index : selected) nicheCount[association[index]]++;
    final Map<Integer, List<Integer>> candidates = new LinkedHashMap<>();
    for (int index : splitFront)
      candidates.computeIfAbsent(association[index], ignored -> new ArrayList<>()).add(index);
    for (List<Integer> indices : candidates.values())
      indices.sort(
          Comparator.comparingDouble((Integer index) -> distance[index])
              .thenComparing(index -> population.get(index).id()));
    while (selected.size() < targetSize) {
      int chosenDirection = -1;
      for (int direction : candidates.keySet())
        if (!candidates.get(direction).isEmpty()
            && (chosenDirection < 0
                || nicheCount[direction] < nicheCount[chosenDirection]
                || nicheCount[direction] == nicheCount[chosenDirection]
                    && direction < chosenDirection)) chosenDirection = direction;
      if (chosenDirection < 0) break;
      final List<Integer> available = candidates.get(chosenDirection);
      final int chosen =
          nicheCount[chosenDirection] == 0
              ? available.remove(0)
              : available.remove(
                  available.size() == 1
                      ? 0
                      : Math.floorMod(
                          population.get(available.get(0)).id().hashCode(), available.size()));
      selected.add(chosen);
      nicheCount[chosenDirection]++;
    }
    return List.copyOf(selected);
  }

  private static double[][] normalize(
      final List<Candidate> population, final List<ObjectiveDefinition> objectives) {
    final int count = objectives.size();
    final double[] ideal = new double[count], nadir = new double[count];
    Arrays.fill(ideal, Double.POSITIVE_INFINITY);
    Arrays.fill(nadir, Double.NEGATIVE_INFINITY);
    final double[][] values = new double[population.size()][count];
    for (int row = 0; row < population.size(); row++)
      for (int objective = 0; objective < count; objective++) {
        final double value =
            objectives
                .get(objective)
                .direction()
                .normalize(population.get(row).objectives()[objective]);
        values[row][objective] = value;
        ideal[objective] = Math.min(ideal[objective], value);
        nadir[objective] = Math.max(nadir[objective], value);
      }
    for (double[] row : values)
      for (int objective = 0; objective < count; objective++) {
        final double range = nadir[objective] - ideal[objective];
        row[objective] = range > 0 ? (row[objective] - ideal[objective]) / range : 0;
      }
    return values;
  }

  private static double perpendicular(final double[] point, final double[] direction) {
    double dot = 0, norm = 0;
    for (int index = 0; index < point.length; index++) {
      dot += point[index] * direction[index];
      norm += direction[index] * direction[index];
    }
    final double scale = norm == 0 ? 0 : dot / norm;
    double squared = 0;
    for (int index = 0; index < point.length; index++)
      squared += Math.pow(point[index] - scale * direction[index], 2);
    return Math.sqrt(squared);
  }
}
