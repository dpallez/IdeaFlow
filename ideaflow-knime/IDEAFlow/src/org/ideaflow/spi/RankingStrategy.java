package org.ideaflow.spi;

import java.util.List;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.ObjectiveDefinition;

public interface RankingStrategy extends Strategy {
    List<List<Integer>> rank(List<Candidate> population, List<ObjectiveDefinition> objectives);
}
