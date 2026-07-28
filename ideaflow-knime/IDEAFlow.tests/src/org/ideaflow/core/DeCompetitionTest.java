package org.ideaflow.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ideaflow.api.OptimizationDirection;
import org.junit.jupiter.api.Test;

final class DeCompetitionTest {
    @Test void minimizingTrialRecordsItsActualImprovement() {
        final DeCompetition.Outcome outcome = DeCompetition.compare(
            0.0, 0.0, 10.0, 7.5, OptimizationDirection.MINIMIZE);
        assertTrue(outcome.trialWins());
        assertEquals(2.5, outcome.improvement(), 1.0e-12);
    }

    @Test void maximizingTrialRecordsItsActualImprovement() {
        final DeCompetition.Outcome outcome = DeCompetition.compare(
            0.0, 0.0, 2.0, 3.25, OptimizationDirection.MAXIMIZE);
        assertTrue(outcome.trialWins());
        assertEquals(1.25, outcome.improvement(), 1.0e-12);
    }

    @Test void constraintReductionWinsBeforeObjectiveQuality() {
        final DeCompetition.Outcome outcome = DeCompetition.compare(
            4.0, 1.5, 1.0, 100.0, OptimizationDirection.MINIMIZE);
        assertTrue(outcome.trialWins());
        assertEquals(2.5, outcome.improvement(), 1.0e-12);
    }

    @Test void worseTrialCannotLeaveAStaleSuccess() {
        final DeCompetition.Outcome outcome = DeCompetition.compare(
            0.0, 0.0, 1.0, 2.0, OptimizationDirection.MINIMIZE);
        assertFalse(outcome.trialWins());
        assertEquals(0.0, outcome.improvement(), 1.0e-12);
    }
}