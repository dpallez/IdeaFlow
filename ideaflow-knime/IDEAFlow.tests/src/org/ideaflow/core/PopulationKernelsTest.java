package org.ideaflow.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.ideaflow.api.Candidate;
import org.junit.jupiter.api.Test;

final class PopulationKernelsTest {
  @Test
  void linearScheduleReachesBothEndpointsAndRoundsIntermediateSizes() {
    assertEquals(100, LinearPopulationSchedule.sizeAt(100, 4, 0, 1000));
    assertEquals(52, LinearPopulationSchedule.sizeAt(100, 4, 500, 1000));
    assertEquals(4, LinearPopulationSchedule.sizeAt(100, 4, 1000, 1000));
    assertEquals(4, LinearPopulationSchedule.sizeAt(100, 4, 2000, 1000));
  }

  @Test
  void linearScheduleRejectsInvalidConfigurations() {
    assertThrows(IllegalArgumentException.class, () -> LinearPopulationSchedule.sizeAt(3, 4, 0, 10));
    assertThrows(IllegalArgumentException.class, () -> LinearPopulationSchedule.sizeAt(4, 1, 0, 10));
    assertThrows(IllegalArgumentException.class, () -> LinearPopulationSchedule.sizeAt(4, 2, -1, 10));
  }

  @Test
  void ringMigrationUsesSortedIslandOrderAndReplacesTailCandidates() {
    final Candidate a1 = candidate("a1");
    final Candidate a2 = candidate("a2");
    final Candidate b1 = candidate("b1");
    final Candidate b2 = candidate("b2");
    final Map<String, List<Candidate>> populations = new LinkedHashMap<>();
    populations.put("b", List.of(b1, b2));
    populations.put("a", List.of(a1, a2));

    final Map<String, List<Candidate>> migrated = IslandMigration.ring(populations, 1);

    assertSame(b1, migrated.get("a").get(1));
    assertSame(a1, migrated.get("b").get(1));
    assertSame(a1, populations.get("a").get(0));
  }

  @Test
  void ringMigrationCapsTheMigrantCountAtThePopulationSize() {
    final Map<String, List<Candidate>> populations =
        Map.of("a", List.of(candidate("a")), "b", List.of(candidate("b")));

    assertEquals(1, IslandMigration.ring(populations, 10).get("a").size());
  }

  @Test
  void ringMigrationRequiresAtLeastTwoIslands() {
    assertThrows(
        IllegalArgumentException.class,
        () -> IslandMigration.ring(Map.of("a", List.of(candidate("a"))), 1));
  }

  private static Candidate candidate(final String id) {
    return new Candidate(id, new double[] {1}, new double[] {1}, 0);
  }
}
