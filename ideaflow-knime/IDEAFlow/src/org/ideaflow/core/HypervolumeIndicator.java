package org.ideaflow.core;

import java.util.List;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.ObjectiveDefinition;
import org.ideaflow.spi.CapabilityDescriptor;
import org.ideaflow.spi.QualityIndicator;

/** Service-provider adapter for dominated hypervolume. */
public final class HypervolumeIndicator implements QualityIndicator {
  @Override
  public String id() {
    return "indicator.hypervolume";
  }

  @Override
  public String displayName() {
    return "Dominated hypervolume";
  }

  @Override
  public CapabilityDescriptor capabilities() {
    return CapabilityDescriptor.general();
  }

  @Override
  public double compute(
      final List<Candidate> population,
      final List<ObjectiveDefinition> objectives,
      final double[] referencePoint) {
    return Hypervolume.compute(population, objectives, referencePoint);
  }
}
