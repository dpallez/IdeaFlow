package org.ideaflow.spi;

import java.util.List;
import java.util.random.RandomGenerator;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.ProblemDefinition;

public interface Initializer extends Strategy {
    List<Candidate> initialize(ProblemDefinition problem, int populationSize, RandomGenerator random);
}
