package org.ideaflow.spi;

import java.util.List;
import java.util.Map;
import org.ideaflow.api.Candidate;

public interface MigrationTopology extends Strategy {
    Map<String, List<Candidate>> migrate(Map<String, List<Candidate>> populations, int migrantCount);
}
