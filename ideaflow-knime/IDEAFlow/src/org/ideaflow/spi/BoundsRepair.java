package org.ideaflow.spi;

import java.util.random.RandomGenerator;

/** Returns an out-of-range value to the variable bounds. */
public interface BoundsRepair extends Strategy {
  double repair(double value, double lowerBound, double upperBound, RandomGenerator random);
}
