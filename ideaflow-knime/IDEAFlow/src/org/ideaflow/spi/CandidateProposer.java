package org.ideaflow.spi;

import java.util.List;
import java.util.random.RandomGenerator;
import org.ideaflow.api.Candidate;

/** Ask side of the family-neutral optimizer protocol. */
public interface CandidateProposer extends Strategy {
    List<Candidate> propose(OptimizationContext context, RandomGenerator random);
}
