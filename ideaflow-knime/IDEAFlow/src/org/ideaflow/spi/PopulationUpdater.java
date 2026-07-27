package org.ideaflow.spi;

import java.util.List;
import java.util.random.RandomGenerator;
import org.ideaflow.api.Candidate;

/** Tell side of the family-neutral optimizer protocol. */
public interface PopulationUpdater extends Strategy {
    UpdateResult update(OptimizationContext context, List<Candidate> evaluated, RandomGenerator random);

    record UpdateResult(List<Candidate> population, org.ideaflow.api.RunState state) {
        public UpdateResult { population = List.copyOf(population); }
    }
}
