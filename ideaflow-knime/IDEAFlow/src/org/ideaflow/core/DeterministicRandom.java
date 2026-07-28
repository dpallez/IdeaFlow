package org.ideaflow.core;

import java.nio.charset.StandardCharsets;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

/** Creates reproducible random streams from stable run and operator identifiers. */
public final class DeterministicRandom {
  private DeterministicRandom() {}

  public static RandomGenerator forScope(final long masterSeed, final Object... scope) {
    long hash = mix64(masterSeed ^ 0xcbf29ce484222325L);
    if (scope != null) {
      for (Object item : scope) {
        final byte[] bytes = String.valueOf(item).getBytes(StandardCharsets.UTF_8);
        for (byte value : bytes) {
          hash ^= value & 0xffL;
          hash *= 0x100000001b3L;
        }
        hash = mix64(hash);
      }
    }
    return new SplittableRandom(hash);
  }

  static long mix64(long value) {
    value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
    value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
    return value ^ (value >>> 31);
  }
}
