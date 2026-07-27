package org.ideaflow.spi;

public interface StoppingCriterion extends Strategy {
    StopDecision evaluate(OptimizationContext context);
    record StopDecision(boolean stop, String reason) { }
}
