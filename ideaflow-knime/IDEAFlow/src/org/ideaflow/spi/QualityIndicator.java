package org.ideaflow.spi;

import java.util.List;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.ObjectiveDefinition;

public interface QualityIndicator extends Strategy {
    double compute(List<Candidate> approximation, List<ObjectiveDefinition> objectives, double[] reference);
}
