package org.ideaflow.spi;

import java.util.List;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.ObjectiveDefinition;

public interface ArchiveStrategy extends Strategy {
    List<Candidate> update(List<Candidate> archive, List<Candidate> candidates,
            List<ObjectiveDefinition> objectives, int capacity);
}
