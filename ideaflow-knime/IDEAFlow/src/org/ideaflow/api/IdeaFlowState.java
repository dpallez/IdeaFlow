package org.ideaflow.api;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable, versioned per-individual optimizer state carried by one KNIME cell. */
public final class IdeaFlowState {
  public static final int VERSION = 1;

  public static final String RUN = "run";
  public static final String POPULATION = "population";
  public static final String INDIVIDUAL = "individual";
  public static final String GENERATION = "generation";
  public static final String SEED = "seed";
  public static final String INITIAL_POPULATION_SIZE = "initialPopulationSize";
  public static final String EVALUATED = "evaluated";
  public static final String EVALUATION = "evaluation";
  public static final String PARENTS = "parents";
  public static final String DE_F = "deF";
  public static final String DE_CR = "deCR";
  public static final String DE_SUCCESS = "deSuccess";
  public static final String DE_IMPROVEMENT = "deImprovement";
  public static final String SHADE_MEMORY_F = "shadeMemoryF";
  public static final String SHADE_MEMORY_CR = "shadeMemoryCR";
  public static final String SHADE_MEMORY_INDEX = "shadeMemoryIndex";
  public static final String TARGET_POPULATION_SIZE = "targetPopulationSize";
  public static final String EVALUATION_SOURCE = "evaluationSource";
  public static final String DE_TARGET_VECTOR = "deTarget";
  public static final String DE_RANDOM_BASE_VECTOR = "deRandomBase";
  public static final String DE_DIFFERENCE_1_VECTOR = "deDifference1";
  public static final String DE_DIFFERENCE_2_VECTOR = "deDifference2";
  public static final String DE_BEST_VECTOR = "deBest";
  public static final String DE_PBEST_VECTOR = "dePbest";
  public static final String DE_PBEST_DIFFERENCE_2_VECTOR = "dePbestDifference2";

  private final Map<String, String> m_values;

  public IdeaFlowState(final Map<String, String> values) {
    m_values = Map.copyOf(values == null ? Map.of() : values);
  }

  public static IdeaFlowState empty() {
    return new IdeaFlowState(Map.of());
  }

  public Map<String, String> values() {
    return m_values;
  }

  public String text(final String key, final String fallback) {
    return m_values.getOrDefault(key, fallback);
  }

  public long longValue(final String key, final long fallback) {
    final String value = m_values.get(key);
    if (value == null) return fallback;
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException exception) {
      return fallback;
    }
  }

  public int intValue(final String key, final int fallback) {
    final long value = longValue(key, fallback);
    return value < Integer.MIN_VALUE || value > Integer.MAX_VALUE ? fallback : (int) value;
  }

  public double doubleValue(final String key, final double fallback) {
    final String value = m_values.get(key);
    if (value == null) return fallback;
    try {
      final double parsed = Double.parseDouble(value);
      return Double.isFinite(parsed) ? parsed : fallback;
    } catch (NumberFormatException exception) {
      return fallback;
    }
  }

  public boolean booleanValue(final String key, final boolean fallback) {
    final String value = m_values.get(key);
    return value == null ? fallback : Boolean.parseBoolean(value);
  }

  public double[] vector(final String key) {
    final String encoded = m_values.get(key);
    if (encoded == null || encoded.isBlank()) return new double[0];
    try {
      final byte[] decoded = Base64.getUrlDecoder().decode(encoded);
      if (decoded.length < Integer.BYTES) return new double[0];
      final ByteBuffer input = ByteBuffer.wrap(decoded);
      final int length = input.getInt();
      if (length < 0 || input.remaining() != (long) length * Double.BYTES) return new double[0];
      final double[] result = new double[length];
      for (int index = 0; index < length; index++) result[index] = input.getDouble();
      return result;
    } catch (IllegalArgumentException exception) {
      return new double[0];
    }
  }

  public IdeaFlowState with(final String key, final String value) {
    final Map<String, String> updated = new LinkedHashMap<>(m_values);
    if (value == null) updated.remove(key);
    else updated.put(key, value);
    return new IdeaFlowState(updated);
  }

  public IdeaFlowState with(final String key, final long value) {
    return with(key, Long.toString(value));
  }

  public IdeaFlowState with(final String key, final double value) {
    return with(key, Double.toString(value));
  }

  public IdeaFlowState with(final String key, final boolean value) {
    return with(key, Boolean.toString(value));
  }

  public IdeaFlowState withVector(final String key, final double[] vector) {
    final ByteBuffer bytes = ByteBuffer.allocate(Integer.BYTES + Double.BYTES * vector.length);
    bytes.putInt(vector.length);
    for (double value : vector) bytes.putDouble(value);
    return with(key, Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.array()));
  }

  public IdeaFlowState without(final String... keys) {
    final Map<String, String> updated = new LinkedHashMap<>(m_values);
    for (String key : keys) updated.remove(key);
    return new IdeaFlowState(updated);
  }

  @Override
  public boolean equals(final Object other) {
    return other instanceof IdeaFlowState state && m_values.equals(state.m_values);
  }

  @Override
  public int hashCode() {
    return m_values.hashCode();
  }
}
