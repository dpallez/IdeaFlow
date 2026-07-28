package org.ideaflow.core;

import java.util.List;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.ObjectiveDefinition;
import org.ideaflow.spi.CapabilityDescriptor;
import org.ideaflow.spi.DominanceComparator;

/** Service-provider adapter for Deb constraint handling and Pareto dominance. */
public final class DefaultDominanceStrategy implements DominanceComparator {
  @Override
  public String id() {
    return "pareto.deb-constraints";
  }

  @Override
  public String displayName() {
    return "Pareto dominance with Deb constraints";
  }

  @Override
  public CapabilityDescriptor capabilities() {
    return CapabilityDescriptor.general();
  }

  @Override
  public int compare(
      final Candidate left, final Candidate right, final List<ObjectiveDefinition> objectives) {
    return ParetoDominance.compare(left, right, objectives);
  }
}
