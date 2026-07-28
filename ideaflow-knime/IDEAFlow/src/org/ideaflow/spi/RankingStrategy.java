package org.ideaflow.spi;

import java.util.List;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.ObjectiveDefinition;

/** Orders a population into Pareto fronts. */
public interface RankingStrategy extends Strategy {
  List<List<Integer>> rank(List<Candidate> population, List<ObjectiveDefinition> objectives);
}
