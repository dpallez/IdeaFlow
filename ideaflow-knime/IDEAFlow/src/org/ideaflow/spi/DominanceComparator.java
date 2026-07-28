package org.ideaflow.spi;

import java.util.List;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.ObjectiveDefinition;

public interface DominanceComparator extends Strategy {
  /** Returns -1 if left dominates, 1 if right dominates, and 0 otherwise. */
  int compare(Candidate left, Candidate right, List<ObjectiveDefinition> objectives);
}
