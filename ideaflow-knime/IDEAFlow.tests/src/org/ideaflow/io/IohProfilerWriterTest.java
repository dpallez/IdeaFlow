package org.ideaflow.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class IohProfilerWriterTest {
  @TempDir Path temporaryDirectory;

  @Test
  void improvementLogEndsWithBestSoFarAtFinalEvaluation() throws Exception {
    final Path folder =
        IohProfilerWriter.write(
            temporaryDirectory,
            "experiment",
            metadata(false),
            List.of(record(1, 10.0), record(2, 5.0), record(3, 12.0)),
            true);

    final List<String> triggered =
        Files.readAllLines(folder.resolve("IOHprofiler_f1_DIM2.dat"));
    assertEquals(
        List.of("evaluations raw_y", "1 10.0", "2 5.0", "3 5.0"), triggered);

    final List<String> complete =
        Files.readAllLines(folder.resolve("IOHprofiler_f1_DIM2.cdat"));
    assertEquals(
        List.of("evaluations raw_y", "1 10.0", "2 5.0", "3 12.0"), complete);

    final String info = Files.readString(folder.resolve("IOHprofiler_f1.info"));
    assertTrue(info.contains("\"version\": \"0.3.3\""));
    assertTrue(info.contains("\"y\": 5.0"));
    assertFalse(info.contains("\"run_id\""));
  }

  @Test
  void maximizationUsesTheLargestObservedValue() throws Exception {
    final Path folder =
        IohProfilerWriter.write(
            temporaryDirectory,
            "maximization",
            metadata(true),
            List.of(record(1, 2.0), record(2, 7.0), record(3, 4.0)),
            false);

    assertEquals(
        List.of("evaluations raw_y", "1 2.0", "2 7.0", "3 7.0"),
        Files.readAllLines(folder.resolve("IOHprofiler_f1_DIM2.dat")));
    assertFalse(Files.exists(folder.resolve("IOHprofiler_f1_DIM2.cdat")));
  }

  @Test
  void rejectsRepeatedEvaluationsWithinARun() {
    final List<IohProfilerWriter.Record> records =
        List.of(record(1, 2.0), record(1, 1.0));

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                IohProfilerWriter.write(
                    temporaryDirectory, "invalid", metadata(false), records, false));

    assertTrue(exception.getMessage().contains("increase strictly"));
    assertFalse(Files.exists(temporaryDirectory.resolve("invalid")));
  }

  @Test
  void sidecarQuotesTabsNewlinesAndQuotes() throws Exception {
    final IohProfilerWriter.Record record =
        new IohProfilerWriter.Record(
            "run\t1", 1, 1, 3.0, Map.of("note", "line one\n\"line two\""));

    final Path folder =
        IohProfilerWriter.write(
            temporaryDirectory, "escaped", metadata(false), List.of(record), false);
    final String sidecar = Files.readString(folder.resolve("ideaflow-events.tsv"));

    assertTrue(sidecar.startsWith("run_id\tinstance\tevaluations\traw_y\tproperty.note"));
    assertTrue(sidecar.contains("\"run\t1\""));
    assertTrue(sidecar.contains("\"line one\n\"\"line two\"\"\""));
  }

  @Test
  void dotDotCannotEscapeTheOutputDirectory() throws Exception {
    final Path folder =
        IohProfilerWriter.write(
            temporaryDirectory, "..", metadata(false), List.of(record(1, 1.0)), false);

    assertEquals(temporaryDirectory.resolve("data"), folder);
    assertTrue(folder.startsWith(temporaryDirectory));
  }

  private static IohProfilerWriter.Metadata metadata(final boolean maximization) {
    return new IohProfilerWriter.Metadata(
        "suite", "1", "problem", 2, "algorithm", "test", maximization);
  }

  private static IohProfilerWriter.Record record(final long evaluations, final double value) {
    return new IohProfilerWriter.Record("run-1", 1, evaluations, value, Map.of());
  }
}
