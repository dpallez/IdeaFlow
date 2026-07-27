package org.ideaflow.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Writes IOHprofiler 0.3.3+ metadata/data plus a lossless IdeaFlow event sidecar. */
public final class IohProfilerWriter {
    private IohProfilerWriter() { }

    public record Metadata(String suite, String problemId, String problemName, int dimension,
            String algorithmName, String algorithmInfo, boolean maximization) {
        public Metadata {
            if (problemId == null || problemId.isBlank() || dimension < 1 || algorithmName == null || algorithmName.isBlank()) {
                throw new IllegalArgumentException("Problem, dimension, and algorithm metadata are required.");
            }
            suite = suite == null || suite.isBlank() ? "unknown_suite" : suite;
            problemName = problemName == null ? problemId : problemName;
            algorithmInfo = algorithmInfo == null ? "" : algorithmInfo;
        }
    }

    public record Record(String runId, int instance, long evaluations, double rawY,
            Map<String, String> properties) {
        public Record {
            if (runId == null || runId.isBlank() || instance < 1 || evaluations < 1 || !Double.isFinite(rawY)) {
                throw new IllegalArgumentException("Invalid IOH event record.");
            }
            properties = Map.copyOf(properties == null ? Map.of() : properties);
        }
    }

    public static Path write(final Path root, final String folderName, final Metadata metadata,
            final List<Record> records, final boolean writeComplete) throws IOException {
        if (records.isEmpty()) throw new IllegalArgumentException("At least one event is required.");
        final Path folder = root.resolve(safe(folderName));
        if (Files.exists(folder)) {
            try (var children = Files.list(folder)) {
                if (children.findAny().isPresent()) {
                    throw new IOException("Refusing to overwrite non-empty IOH output folder: " + folder);
                }
            }
        }
        Files.createDirectories(folder);
        final String token = safe(metadata.problemId());
        final String rawName = "IOHprofiler_f" + token + "_DIM" + metadata.dimension() + ".dat";
        final Path raw = folder.resolve(rawName);
        final Map<String, List<Record>> runs = group(records);
        writeTriggered(raw, runs, metadata.maximization(), true);
        if (writeComplete) writeTriggered(folder.resolve(rawName.replace(".dat", ".cdat")), runs, metadata.maximization(), false);
        writeSidecar(folder.resolve("ideaflow-events.tsv"), records);
        writeInfo(folder.resolve("IOHprofiler_f" + token + ".info"), metadata, rawName, runs);
        return folder;
    }

    private static Map<String, List<Record>> group(final List<Record> records) {
        final Map<String, List<Record>> result = new LinkedHashMap<>();
        records.stream().sorted(Comparator.comparing(Record::runId).thenComparingLong(Record::evaluations))
                .forEach(record -> result.computeIfAbsent(record.runId(), key -> new ArrayList<>()).add(record));
        return result;
    }

    private static void writeTriggered(final Path path, final Map<String, List<Record>> runs,
            final boolean maximization, final boolean improvementsOnly) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW)) {
            for (List<Record> run : runs.values()) {
                writer.write("evaluations raw_y run_id");
                writer.newLine();
                double best = maximization ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
                Record lastWritten = null;
                for (Record record : run) {
                    final boolean improved = maximization ? record.rawY() > best : record.rawY() < best;
                    if (!improvementsOnly || improved) {
                        line(writer, record);
                        lastWritten = record;
                    }
                    if (improved) best = record.rawY();
                }
                final Record finalRecord = run.get(run.size() - 1);
                if (lastWritten == null || lastWritten.evaluations() != finalRecord.evaluations()) line(writer, finalRecord);
            }
        }
    }

    private static void line(final BufferedWriter writer, final Record record) throws IOException {
        writer.write(Long.toString(record.evaluations()));
        writer.write(' ');
        writer.write(Double.toString(record.rawY()));
        writer.write(' ');
        writer.write(safe(record.runId()));
        writer.newLine();
    }

    private static void writeSidecar(final Path path, final List<Record> records) throws IOException {
        final List<String> properties = records.stream().flatMap(record -> record.properties().keySet().stream())
                .distinct().sorted().toList();
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
            writer.write("run_id\tinstance\tevaluations\traw_y");
            for (String property : properties) writer.write("\t" + property.replace('\t', '_'));
            writer.newLine();
            for (Record record : records) {
                writer.write(record.runId() + "\t" + record.instance() + "\t" + record.evaluations() + "\t" + record.rawY());
                for (String property : properties) writer.write("\t" + record.properties().getOrDefault(property, ""));
                writer.newLine();
            }
        }
    }

    private static void writeInfo(final Path path, final Metadata metadata, final String rawName,
            final Map<String, List<Record>> runs) throws IOException {
        final StringBuilder json = new StringBuilder();
        json.append("{\n  \"version\": \"0.3.14\",")
                .append("\n  \"suite\": \"").append(json(metadata.suite())).append("\",")
                .append("\n  \"function_id\": \"").append(json(metadata.problemId())).append("\",")
                .append("\n  \"function_name\": \"").append(json(metadata.problemName())).append("\",")
                .append("\n  \"maximization\": ").append(metadata.maximization()).append(',')
                .append("\n  \"algorithm\": {\"name\": \"").append(json(metadata.algorithmName()))
                .append("\", \"info\": \"").append(json(metadata.algorithmInfo())).append("\"},")
                .append("\n  \"attributes\": [\"evaluations\", \"raw_y\"],")
                .append("\n  \"scenarios\": [{\"dimension\": ").append(metadata.dimension())
                .append(", \"path\": \"").append(json(rawName)).append("\", \"runs\": [");
        boolean first = true;
        for (List<Record> run : runs.values()) {
            final Record last = run.get(run.size() - 1);
            Record best = run.get(0);
            for (Record record : run) if (metadata.maximization() ? record.rawY() > best.rawY() : record.rawY() < best.rawY()) best = record;
            if (!first) json.append(',');
            first = false;
            json.append("{\"instance\": ").append(last.instance()).append(", \"evals\": ").append(last.evaluations())
                    .append(", \"run_id\": \"").append(json(last.runId())).append("\", \"best\": {\"evals\": ")
                    .append(best.evaluations()).append(", \"y\": ").append(best.rawY()).append("}}");
        }
        json.append("]}]\n}\n");
        Files.writeString(path, json.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }

    private static String safe(final String value) {
        final String result = String.valueOf(value).replaceAll("[^A-Za-z0-9._-]", "_");
        return result.isBlank() ? "data" : result;
    }

    private static String json(final String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
