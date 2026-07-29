package org.ideaflow.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.ideaflow.api.RunState;
import org.junit.jupiter.api.Test;

final class RunStateCodecTest {
  @Test
  void roundTripsEveryRunStateField() {
    final Map<String, String> algorithm = new LinkedHashMap<>();
    algorithm.put("memory", "0.1,0.2");
    algorithm.put("unicode", "évolution");
    final RunState expected =
        new RunState("run", "island", 7, 100, 1234, true, "target", algorithm);
    final RunStateCodec codec = new RunStateCodec();

    assertEquals(expected, codec.decode(codec.encode(expected)));
  }

  @Test
  void encodingIsDeterministicForStableMapOrder() {
    final RunState state =
        new RunState("run", "population", 0, 10, 1, false, "continue", Map.of("a", "b"));
    final RunStateCodec codec = new RunStateCodec();

    assertArrayEquals(codec.encode(state), codec.encode(state));
  }

  @Test
  void rejectsUnknownVersionsAndTruncatedPayloads() {
    final RunStateCodec codec = new RunStateCodec();

    assertThrows(
        IllegalArgumentException.class,
        () -> codec.decode(ByteBuffer.allocate(4).putInt(99).array()));
    assertThrows(IllegalArgumentException.class, () -> codec.decode(new byte[] {0, 0, 0, 2}));
  }
}
