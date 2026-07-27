package org.ideaflow.spi;

import org.ideaflow.api.Candidate;

public interface TraceProperty extends Strategy {
    double value(Candidate candidate, OptimizationContext context);
}
