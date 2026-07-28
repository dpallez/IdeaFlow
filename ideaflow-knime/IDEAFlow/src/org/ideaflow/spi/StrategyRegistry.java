package org.ideaflow.spi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/** Discovers third-party strategies through Java's standard service-provider mechanism. */
public final class StrategyRegistry {
  private StrategyRegistry() {}

  public static <T extends Strategy> List<T> discover(final Class<T> strategyType) {
    final List<T> result = new ArrayList<>();
    ServiceLoader.load(strategyType, strategyType.getClassLoader()).forEach(result::add);
    result.sort(Comparator.comparing(Strategy::id));
    return List.copyOf(result);
  }

  public static <T extends Strategy> T require(final Class<T> strategyType, final String id) {
    return discover(strategyType).stream()
        .filter(strategy -> strategy.id().equals(id))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "No " + strategyType.getSimpleName() + " registered with ID '" + id + "'."));
  }
}
