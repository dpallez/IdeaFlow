package org.ideaflow.core;

import org.ideaflow.api.OptimizationDirection;

/** Constraint-aware single-objective parent/trial competition and improvement measurement. */
public final class DeCompetition {
    public record Outcome(boolean trialWins, double improvement) { }

    private DeCompetition() { }

    public static Outcome compare(final double parentViolation, final double trialViolation,
            final double parentObjective, final double trialObjective,
            final OptimizationDirection direction) {
        if (parentViolation > 0.0 || trialViolation > 0.0) {
            final boolean wins = trialViolation < parentViolation;
            return new Outcome(wins, wins ? parentViolation - trialViolation : 0.0);
        }
        final double parent = direction.normalize(parentObjective);
        final double trial = direction.normalize(trialObjective);
        final boolean wins = trial <= parent;
        return new Outcome(wins, wins ? Math.max(0.0, parent - trial) : 0.0);
    }
}
