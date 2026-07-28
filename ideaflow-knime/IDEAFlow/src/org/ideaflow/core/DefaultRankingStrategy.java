package org.ideaflow.core;

import java.util.List;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.ObjectiveDefinition;
import org.ideaflow.spi.CapabilityDescriptor;
import org.ideaflow.spi.RankingStrategy;

/** Service-provider adapter for fast nondominated sorting. */
public final class DefaultRankingStrategy implements RankingStrategy {
  @Override
  public String id() {
    return "ranking.fast-nondominated";
  }

  @Override
  public String displayName() {
    return "Fast nondominated sorting";
  }

  @Override
  public CapabilityDescriptor capabilities() {
    return CapabilityDescriptor.general();
  }

  @Override
  public List<List<Integer>> rank(
      final List<Candidate> population, final List<ObjectiveDefinition> objectives) {
    return FastNonDominatedSort.sort(population, objectives);
  }
}
