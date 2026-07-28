package org.ideaflow.core;

import java.util.List;
import java.util.Map;
import org.ideaflow.api.Candidate;
import org.ideaflow.spi.CapabilityDescriptor;
import org.ideaflow.spi.MigrationTopology;

/** Service-provider adapter for deterministic ring migration. */
public final class RingMigrationTopology implements MigrationTopology {
  @Override
  public String id() {
    return "migration.ring";
  }

  @Override
  public String displayName() {
    return "Ring migration";
  }

  @Override
  public CapabilityDescriptor capabilities() {
    return CapabilityDescriptor.general();
  }

  @Override
  public Map<String, List<Candidate>> migrate(
      final Map<String, List<Candidate>> populations, final int migrantCount) {
    return IslandMigration.ring(populations, migrantCount);
  }
}
