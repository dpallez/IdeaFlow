package org.ideaflow.spi;

import java.util.List;
import java.util.random.RandomGenerator;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.ProblemDefinition;

public interface VariationOperator extends Strategy {
    List<Candidate> vary(List<Candidate> parents, ProblemDefinition problem, RandomGenerator random);
}
