package org.ideaflow.core;

import java.util.ArrayList;
import java.util.List;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.ObjectiveDefinition;

/** Assigns candidates to Pareto fronts using constraint-aware dominance. */
public final class FastNonDominatedSort {
  private FastNonDominatedSort() {}

  public static List<List<Integer>> sort(
      final List<Candidate> population, final List<ObjectiveDefinition> objectives) {
    final int size = population.size();
    final List<List<Integer>> dominates = new ArrayList<>(size);
    final int[] dominationCount = new int[size];
    final List<Integer> first = new ArrayList<>();
    for (int p = 0; p < size; p++) {
      final List<Integer> pDominates = new ArrayList<>();
      for (int q = 0; q < size; q++) {
        if (p == q) continue;
        final int relation =
            ParetoDominance.compare(population.get(p), population.get(q), objectives);
        if (relation < 0) pDominates.add(q);
        else if (relation > 0) dominationCount[p]++;
      }
      dominates.add(pDominates);
      if (dominationCount[p] == 0) first.add(p);
    }

    final List<List<Integer>> fronts = new ArrayList<>();
    List<Integer> current = first;
    while (!current.isEmpty()) {
      fronts.add(List.copyOf(current));
      final List<Integer> next = new ArrayList<>();
      for (int p : current) {
        for (int q : dominates.get(p)) {
          if (--dominationCount[q] == 0) next.add(q);
        }
      }
      current = next;
    }
    return List.copyOf(fronts);
  }

  public static int[] ranks(
      final List<Candidate> population, final List<ObjectiveDefinition> objectives) {
    final int[] ranks = new int[population.size()];
    final List<List<Integer>> fronts = sort(population, objectives);
    for (int rank = 0; rank < fronts.size(); rank++) {
      for (int index : fronts.get(rank)) ranks[index] = rank;
    }
    return ranks;
  }
}
