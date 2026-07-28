package org.ideaflow.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class EvolutionScheduleTest {
    @Test void migratesOnlyOnPositiveIntervalBoundaries() {
        assertFalse(EvolutionSchedule.shouldMigrate(0, 10));
        assertFalse(EvolutionSchedule.shouldMigrate(9, 10));
        assertTrue(EvolutionSchedule.shouldMigrate(10, 10));
        assertFalse(EvolutionSchedule.shouldMigrate(11, 10));
    }

    @Test void reservesACompleteGenerationWithoutOvershooting() {
        assertTrue(EvolutionSchedule.canEvaluateBatch(99_960, 40, 100_000));
        assertFalse(EvolutionSchedule.canEvaluateBatch(99_961, 40, 100_000));
        assertFalse(EvolutionSchedule.canEvaluateBatch(100_001, 0, 100_000));
    }
}