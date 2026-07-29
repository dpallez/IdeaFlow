package org.ideaflow.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.ObjectiveDefinition;
import org.ideaflow.api.OptimizationDirection;
import org.junit.jupiter.api.Test;

final class MultiObjectiveKernelsTest {
  private static final List<ObjectiveDefinition> MINIMIZE_TWO =
      List.of(
          new ObjectiveDefinition("f1", OptimizationDirection.MINIMIZE, null, null),
          new ObjectiveDefinition("f2", OptimizationDirection.MINIMIZE, null, null));

  @Test
  void feasibilityPrecedesObjectiveDominance() {
    final Candidate feasible = candidate("feasible", 100, 100, 0);
    final Candidate infeasible = candidate("infeasible", 0, 0, 1);

    assertEquals(-1, ParetoDominance.compare(feasible, infeasible, MINIMIZE_TWO));
    assertEquals(1, ParetoDominance.compare(infeasible, feasible, MINIMIZE_TWO));
  }

  @Test
  void mixedDirectionsAreNormalizedBeforeDominance() {
    final List<ObjectiveDefinition> objectives =
        List.of(
            new ObjectiveDefinition("cost", OptimizationDirection.MINIMIZE, null, null),
            new ObjectiveDefinition("quality", OptimizationDirection.MAXIMIZE, null, null));

    assertEquals(
        -1,
        ParetoDominance.compare(
            candidate("better", 1, 10, 0), candidate("worse", 2, 9, 0), objectives));
  }

  @Test
  void assignsExpectedFrontsAndRanks() {
    final List<Candidate> population =
        List.of(
            candidate("a", 0, 2, 0),
            candidate("b", 1, 1, 0),
            candidate("c", 2, 0, 0),
            candidate("d", 2, 2, 0));

    assertEquals(List.of(List.of(0, 1, 2), List.of(3)), FastNonDominatedSort.sort(population, MINIMIZE_TWO));
    assertArrayEquals(new int[] {0, 0, 0, 1}, FastNonDominatedSort.ranks(population, MINIMIZE_TWO));
  }

  @Test
  void crowdingKeepsFrontBoundariesAndScoresInteriorPoints() {
    final List<Candidate> population =
        List.of(
            candidate("a", 0, 2, 0),
            candidate("b", 1, 1, 0),
            candidate("c", 2, 0, 0));
    final double[] distances =
        CrowdingDistance.compute(population, List.of(List.of(0, 1, 2)), MINIMIZE_TWO);

    assertTrue(Double.isInfinite(distances[0]));
    assertEquals(2.0, distances[1], 1.0e-12);
    assertTrue(Double.isInfinite(distances[2]));
  }

  @Test
  void generatesTheExpectedSimplexLattice() {
    final List<double[]> directions = ReferenceDirections.dasDennis(3, 2);

    assertEquals(6, directions.size());
    for (double[] direction : directions) {
      assertEquals(1.0, java.util.Arrays.stream(direction).sum(), 1.0e-12);
    }
    assertThrows(IllegalArgumentException.class, () -> ReferenceDirections.dasDennis(1, 2));
  }

  @Test
  void computesExactTwoDimensionalHypervolumeAndContributions() {
    final List<Candidate> front =
        List.of(candidate("a", 1, 3, 0), candidate("b", 2, 2, 0), candidate("c", 3, 1, 0));

    assertEquals(6.0, Hypervolume.compute(front, MINIMIZE_TWO, new double[] {4, 4}), 1.0e-12);
    assertArrayEquals(
        new double[] {1.0, 1.0, 1.0},
        Hypervolume.contributions(front, MINIMIZE_TWO, new double[] {4, 4}),
        1.0e-12);
  }

  @Test
  void ignoresInfeasibleAndOutOfReferenceHypervolumePoints() {
    final List<Candidate> front =
        List.of(candidate("valid", 1, 1, 0), candidate("infeasible", 0, 0, 1), candidate("outside", 5, 1, 0));

    assertEquals(9.0, Hypervolume.compute(front, MINIMIZE_TWO, new double[] {4, 4}), 1.0e-12);
  }

  @Test
  void qualityIndicatorsMatchSimpleReferenceSets() {
    final List<Candidate> approximation =
        List.of(candidate("a", 1, 1, 0), candidate("b", 3, 3, 0));
    final List<Candidate> reference = List.of(candidate("r", 1, 1, 0));

    assertEquals(Math.sqrt(8.0) / 2.0, QualityIndicators.generationalDistance(approximation, reference, MINIMIZE_TWO), 1.0e-12);
    assertEquals(0.0, QualityIndicators.invertedGenerationalDistance(approximation, reference, MINIMIZE_TWO), 1.0e-12);
    assertEquals(0.0, QualityIndicators.invertedGenerationalDistancePlus(approximation, reference, MINIMIZE_TWO), 1.0e-12);
    assertEquals(0.0, QualityIndicators.additiveEpsilon(approximation, reference, MINIMIZE_TWO), 1.0e-12);
    assertEquals(0.0, QualityIndicators.spacing(approximation, MINIMIZE_TWO), 1.0e-12);
    assertThrows(
        IllegalArgumentException.class,
        () -> QualityIndicators.generationalDistance(List.of(), reference, MINIMIZE_TWO));
  }

  @Test
  void nsgaThreeSelectionIsDeterministicAndReturnsRequestedSize() {
    final List<Candidate> population =
        List.of(
            candidate("a", 0, 4, 0),
            candidate("b", 1, 3, 0),
            candidate("c", 2, 2, 0),
            candidate("d", 3, 1, 0),
            candidate("e", 4, 0, 0));

    final List<Integer> first = Nsga3Selection.select(population, MINIMIZE_TWO, 3, 2);
    final List<Integer> second = Nsga3Selection.select(population, MINIMIZE_TWO, 3, 2);

    assertEquals(3, first.size());
    assertEquals(first, second);
  }

  private static Candidate candidate(
      final String id, final double first, final double second, final double violation) {
    return new Candidate(id, new double[0], new double[] {first, second}, violation);
  }
}
