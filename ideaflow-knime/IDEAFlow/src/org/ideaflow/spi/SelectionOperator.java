package org.ideaflow.spi;

import java.util.List;
import java.util.random.RandomGenerator;
import org.ideaflow.api.Candidate;

public interface SelectionOperator extends Strategy {
    List<Candidate> select(List<Candidate> population, int count, RandomGenerator random);
}
