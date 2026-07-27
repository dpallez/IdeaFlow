package org.ideaflow.core;

import java.util.List;
import java.util.random.RandomGenerator;
import org.ideaflow.api.Candidate;
import org.ideaflow.spi.CandidateProposer;
import org.ideaflow.spi.OptimizationContext;
import org.ideaflow.spi.PopulationUpdater;

public final class AskTellEngine {
    private final CandidateProposer proposer;
    private final PopulationUpdater updater;

    public AskTellEngine(final CandidateProposer proposer, final PopulationUpdater updater) {
        this.proposer = java.util.Objects.requireNonNull(proposer);
        this.updater = java.util.Objects.requireNonNull(updater);
    }

    public List<Candidate> ask(final OptimizationContext context, final RandomGenerator random) {
        return List.copyOf(proposer.propose(context, random));
    }

    public PopulationUpdater.UpdateResult tell(final OptimizationContext context,
            final List<Candidate> evaluated, final RandomGenerator random) {
        return updater.update(context, List.copyOf(evaluated), random);
    }
}
