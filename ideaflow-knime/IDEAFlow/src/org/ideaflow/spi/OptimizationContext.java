package org.ideaflow.spi;

import java.util.List;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.ProblemDefinition;
import org.ideaflow.api.RunState;

public record OptimizationContext(ProblemDefinition problem, RunState state, List<Candidate> population) {
    public OptimizationContext {
        if (problem == null || state == null) throw new IllegalArgumentException("Problem and state are required.");
        population = List.copyOf(population == null ? List.of() : population);
    }
}
