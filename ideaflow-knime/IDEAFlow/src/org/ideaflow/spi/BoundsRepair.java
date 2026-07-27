package org.ideaflow.spi;

import java.util.random.RandomGenerator;

public interface BoundsRepair extends Strategy {
    double repair(double value, double lowerBound, double upperBound, RandomGenerator random);
}
