package org.ideaflow.spi;

import java.util.List;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.ObjectiveDefinition;

/** Computes one scalar quality measure for a population. */
public interface QualityIndicator extends Strategy {
  double compute(
      List<Candidate> approximation, List<ObjectiveDefinition> objectives, double[] reference);
}
