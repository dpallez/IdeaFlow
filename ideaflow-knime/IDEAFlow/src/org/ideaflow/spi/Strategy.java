package org.ideaflow.spi;

public interface Strategy {
    String id();
    String displayName();
    CapabilityDescriptor capabilities();
}
