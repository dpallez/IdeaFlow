package org.ideaflow.spi;

/** Common identity and capability contract for discoverable optimization strategies. */
public interface Strategy {
  String id();

  String displayName();

  CapabilityDescriptor capabilities();
}
