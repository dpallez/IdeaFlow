package org.ideaflow.core;

import java.util.Set;
import java.util.random.RandomGenerator;
import org.ideaflow.api.VariableType;
import org.ideaflow.spi.BoundsRepair;
import org.ideaflow.spi.CapabilityDescriptor;

/** Repairs a value by clamping it to the nearest bound. */
public final class ClampBoundsRepair implements BoundsRepair {
  @Override
  public String id() {
    return "bounds.clamp";
  }

  @Override
  public String displayName() {
    return "Clamp to bounds";
  }

  @Override
  public CapabilityDescriptor capabilities() {
    return new CapabilityDescriptor(
        Set.of(VariableType.REAL, VariableType.INTEGER), 1, true, Set.of());
  }

  @Override
  public double repair(
      final double value,
      final double lowerBound,
      final double upperBound,
      final RandomGenerator random) {
    return Math.max(lowerBound, Math.min(upperBound, value));
  }
}
