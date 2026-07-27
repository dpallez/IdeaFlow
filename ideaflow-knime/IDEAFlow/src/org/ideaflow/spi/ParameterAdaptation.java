package org.ideaflow.spi;

import java.util.Map;

public interface ParameterAdaptation extends Strategy {
    Map<String, Double> update(Map<String, Double> current, Map<String, double[]> successfulValues);
}
