package org.ideaflow.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.ObjectiveDefinition;

/** Exact axis-aligned dominated hypervolume for arbitrary objective counts. */
public final class Hypervolume {
  private Hypervolume() {}

  public static double compute(
      final List<Candidate> approximation,
      final List<ObjectiveDefinition> objectives,
      final double[] referencePoint) {
    if (objectives.isEmpty() || referencePoint.length != objectives.size()) {
      throw new IllegalArgumentException("Reference point must match the objective count.");
    }
    final double[] reference = new double[referencePoint.length];
    for (int d = 0; d < reference.length; d++) {
      reference[d] = objectives.get(d).direction().normalize(referencePoint[d]);
      if (!Double.isFinite(reference[d]))
        throw new IllegalArgumentException("Reference point must be finite.");
    }
    final List<double[]> points = new ArrayList<>();
    for (Candidate candidate : approximation) {
      if (candidate.constraintViolation() > 0.0) continue;
      final double[] raw = candidate.objectives();
      if (raw.length != objectives.size())
        throw new IllegalArgumentException("Objective count mismatch.");
      final double[] point = new double[raw.length];
      boolean inside = true;
      for (int d = 0; d < raw.length; d++) {
        point[d] = objectives.get(d).direction().normalize(raw[d]);
        if (!Double.isFinite(point[d]) || point[d] >= reference[d]) inside = false;
      }
      if (inside) points.add(point);
    }
    return unionVolume(removeDominated(points), reference, reference.length);
  }

  public static double[] contributions(
      final List<Candidate> approximation,
      final List<ObjectiveDefinition> objectives,
      final double[] referencePoint) {
    final double total = compute(approximation, objectives, referencePoint);
    final double[] result = new double[approximation.size()];
    for (int i = 0; i < approximation.size(); i++) {
      final List<Candidate> reduced = new ArrayList<>(approximation);
      reduced.remove(i);
      result[i] = Math.max(0.0, total - compute(reduced, objectives, referencePoint));
    }
    return result;
  }

  private static double unionVolume(
      final List<double[]> points, final double[] reference, final int dimensions) {
    if (points.isEmpty()) return 0.0;
    if (dimensions == 1) {
      double minimum = reference[0];
      for (double[] point : points) minimum = Math.min(minimum, point[0]);
      return Math.max(0.0, reference[0] - minimum);
    }
    final int axis = dimensions - 1;
    final List<Double> boundaries =
        points.stream().map(point -> point[axis]).distinct().sorted().toList();
    double volume = 0.0;
    for (int i = 0; i < boundaries.size(); i++) {
      final double lower = boundaries.get(i);
      final double upper = i + 1 < boundaries.size() ? boundaries.get(i + 1) : reference[axis];
      if (upper <= lower) continue;
      final List<double[]> active = new ArrayList<>();
      for (double[] point : points) {
        if (point[axis] <= lower) active.add(Arrays.copyOf(point, axis));
      }
      volume +=
          (upper - lower)
              * unionVolume(removeDominated(active), Arrays.copyOf(reference, axis), axis);
    }
    return volume;
  }

  private static List<double[]> removeDominated(final List<double[]> points) {
    final List<double[]> result = new ArrayList<>();
    outer:
    for (int i = 0; i < points.size(); i++) {
      for (int j = 0; j < points.size(); j++) {
        if (i != j && dominates(points.get(j), points.get(i))) continue outer;
      }
      final double[] point = points.get(i);
      if (result.stream().noneMatch(existing -> Arrays.equals(existing, point))) result.add(point);
    }
    result.sort(Comparator.comparingDouble(point -> point[0]));
    return result;
  }

  private static boolean dominates(final double[] left, final double[] right) {
    boolean strict = false;
    for (int i = 0; i < left.length; i++) {
      if (left[i] > right[i]) return false;
      strict |= left[i] < right[i];
    }
    return strict;
  }
}
