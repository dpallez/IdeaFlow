package org.ideaflow.knime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.ConstraintRelation;
import org.ideaflow.api.ObjectiveDefinition;
import org.ideaflow.api.OptimizationDirection;
import org.ideaflow.api.ReservedColumns;
import org.ideaflow.core.BinaryEncoding;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;

public final class KnimeTableSupport {
  private KnimeTableSupport() {}

  public static List<String> names(final String commaSeparated) {
    if (commaSeparated == null || commaSeparated.isBlank()) return List.of();
    return java.util.Arrays.stream(commaSeparated.split(","))
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .toList();
  }

  public static List<OptimizationDirection> directions(final String commaSeparated, final int count)
      throws InvalidSettingsException {
    final List<String> names = names(commaSeparated);
    if (names.size() != count) {
      throw new InvalidSettingsException("Provide exactly one direction per objective.");
    }
    try {
      return names.stream().map(OptimizationDirection::parse).toList();
    } catch (IllegalArgumentException e) {
      throw new InvalidSettingsException(e.getMessage(), e);
    }
  }

  public static int[] requireNumericColumns(final DataTableSpec spec, final List<String> names)
      throws InvalidSettingsException {
    if (names.isEmpty())
      throw new InvalidSettingsException("At least one column must be selected.");
    final int[] indices = new int[names.size()];
    for (int i = 0; i < names.size(); i++) {
      indices[i] = spec.findColumnIndex(names.get(i));
      if (indices[i] < 0) throw new InvalidSettingsException("Missing column: " + names.get(i));
      final Class<?> valueClass = spec.getColumnSpec(indices[i]).getType().getPreferredValueClass();
      if (!(DoubleValue.class.isAssignableFrom(valueClass)
          || IntValue.class.isAssignableFrom(valueClass)
          || LongValue.class.isAssignableFrom(valueClass))) {
        throw new InvalidSettingsException("Column must be numeric: " + names.get(i));
      }
    }
    return indices;
  }


  /** Requires the same ordered column names and types, while allowing different domain metadata. */
  public static void requireCompatibleSchema(
      final DataTableSpec expected, final DataTableSpec actual, final String label)
      throws InvalidSettingsException {
    if (expected == null || actual == null) return;
    if (expected.getNumColumns() != actual.getNumColumns()) {
      throw new InvalidSettingsException(
          label
              + " must have the population columns: expected "
              + expected.getNumColumns()
              + " columns but found "
              + actual.getNumColumns()
              + ".");
    }
    for (int index = 0; index < expected.getNumColumns(); index++) {
      final DataColumnSpec expectedColumn = expected.getColumnSpec(index);
      final DataColumnSpec actualColumn = actual.getColumnSpec(index);
      if (!expectedColumn.getName().equals(actualColumn.getName())
          || !expectedColumn.getType().equals(actualColumn.getType())) {
        throw new InvalidSettingsException(
            label
                + " must have the same ordered column names and types "
                + "as the population. The first mismatch is column "
                + index
                + " ('"
                + expectedColumn.getName()
                + "').");
      }
    }
  }

  public static double number(final DataCell cell, final DataRow row, final String column)
      throws InvalidSettingsException {
    if (cell.isMissing())
      throw new InvalidSettingsException("Missing value in '" + column + "' at " + row.getKey());
    final double value;
    if (cell instanceof DoubleValue typed) value = typed.getDoubleValue();
    else if (cell instanceof IntValue typed) value = typed.getIntValue();
    else if (cell instanceof LongValue typed) value = typed.getLongValue();
    else
      throw new InvalidSettingsException(
          "Non-numeric value in '" + column + "' at " + row.getKey());
    if (!Double.isFinite(value))
      throw new InvalidSettingsException("Non-finite value in '" + column + "' at " + row.getKey());
    return value;
  }

  public static List<ObjectiveDefinition> objectives(
      final List<String> columns,
      final List<OptimizationDirection> directions,
      final List<Double> references) {
    final List<ObjectiveDefinition> result = new ArrayList<>();
    for (int i = 0; i < columns.size(); i++) {
      result.add(
          new ObjectiveDefinition(
              columns.get(i),
              directions.get(i),
              null,
              references == null || references.isEmpty() ? null : references.get(i)));
    }
    return List.copyOf(result);
  }

  public static Candidate candidate(
      final DataRow row,
      final int[] variableIndices,
      final int[] objectiveIndices,
      final int violationIndex,
      final List<String> variableNames,
      final List<String> objectiveNames)
      throws InvalidSettingsException {
    final double[] variables = new double[variableIndices.length];
    final double[] objectives = new double[objectiveIndices.length];
    for (int i = 0; i < variables.length; i++)
      variables[i] = number(row.getCell(variableIndices[i]), row, variableNames.get(i));
    for (int i = 0; i < objectives.length; i++)
      objectives[i] = number(row.getCell(objectiveIndices[i]), row, objectiveNames.get(i));
    final double violation =
        violationIndex < 0
            ? 0.0
            : Math.max(
                0.0,
                number(row.getCell(violationIndex), row, ReservedColumns.CONSTRAINT_VIOLATION));
    return new Candidate(row.getKey().getString(), variables, objectives, violation);
  }

  public static DataTableSpec appendOrReplace(
      final DataTableSpec input, final DataColumnSpec... columns) {
    final List<DataColumnSpec> result = new ArrayList<>();
    for (int i = 0; i < input.getNumColumns(); i++) result.add(input.getColumnSpec(i));
    for (DataColumnSpec column : columns) {
      final int existing = input.findColumnIndex(column.getName());
      if (existing >= 0) result.set(existing, column);
      else result.add(column);
    }
    // Keep the single internal-state cell at the far right of population tables.
    // Newly appended objectives, constraints, or indicators therefore remain easy to inspect.
    final int stateIndex =
        java.util.stream.IntStream.range(0, result.size())
            .filter(index -> PopulationState.COLUMN.equals(result.get(index).getName()))
            .findFirst()
            .orElse(-1);
    if (stateIndex >= 0 && stateIndex != result.size() - 1) {
      result.add(result.remove(stateIndex));
    }
    return new DataTableSpec(input.getName(), result.toArray(DataColumnSpec[]::new));
  }

  public static DataCell[] copyToSpec(
      final DataRow row, final DataTableSpec input, final DataTableSpec output) {
    final DataCell[] cells = new DataCell[output.getNumColumns()];
    for (int i = 0; i < output.getNumColumns(); i++) {
      final int source = input.findColumnIndex(output.getColumnSpec(i).getName());
      cells[i] = source >= 0 ? row.getCell(source) : org.knime.core.data.DataType.getMissingCell();
    }
    return cells;
  }

  public static DataColumnSpec stringColumn(final String name) {
    return new DataColumnSpecCreator(name, StringCell.TYPE).createSpec();
  }

  public static DataColumnSpec longColumn(final String name) {
    return new DataColumnSpecCreator(name, LongCell.TYPE).createSpec();
  }

  public static DataColumnSpec intColumn(final String name) {
    return new DataColumnSpecCreator(name, IntCell.TYPE).createSpec();
  }

  public static DataColumnSpec doubleColumn(final String name) {
    return new DataColumnSpecCreator(name, DoubleCell.TYPE).createSpec();
  }

  /**
   * Configuration-time problem information carried in a KNIME table specification.
   *
   * <p>This is nested in an established Eclipse source unit so PDE incremental builds discover it
   * reliably when an existing workspace is refreshed.
   */
  public static final class ProblemMetadata {
    public static final String PROPERTY_KEY = "ideaflow.problem.metadata.v1";
    private static final int FORMAT_VERSION = 2;

    public record Variable(
        String name,
        String type,
        double lower,
        double upper,
        String representation,
        String encoding,
        int bits) {
      public Variable {
        name = required(name, "Variable name");
        type = required(type, "Variable type");
        representation = required(representation, "Variable representation");
        encoding = encoding == null ? "" : encoding;
        if (!Double.isFinite(lower) || !Double.isFinite(upper) || lower > upper) {
          throw new IllegalArgumentException("Invalid bounds for " + name);
        }
        if (bits < 1) throw new IllegalArgumentException("Invalid bit count for " + name);
      }

      public boolean encoded() {
        return "BINARY_ENCODED".equalsIgnoreCase(representation);
      }

      public List<String> populationColumns() {
        if (!encoded()) return List.of(name);
        final List<String> result = new ArrayList<>(bits);
        for (int bit = 0; bit < bits; bit++) result.add(BinaryEncoding.geneName(name, bit));
        return List.copyOf(result);
      }
    }

    public record Objective(String column, OptimizationDirection direction, Double referencePoint) {
      public Objective {
        column = required(column, "Objective column");
        if (direction == null)
          throw new IllegalArgumentException("Objective direction is required.");
        if (referencePoint != null && !Double.isFinite(referencePoint)) {
          throw new IllegalArgumentException("Objective reference point must be finite.");
        }
      }
    }

    public record Constraint(
        String column, ConstraintRelation relation, double threshold, double tolerance) {
      public Constraint {
        column = required(column, "Constraint column");
        if (relation == null)
          throw new IllegalArgumentException("Constraint relation is required.");
        if (!Double.isFinite(threshold) || !Double.isFinite(tolerance) || tolerance < 0.0) {
          throw new IllegalArgumentException("Invalid constraint values for " + column);
        }
      }
    }

    public record Schema(
        String problemId,
        long maxEvaluations,
        List<Variable> variables,
        List<Objective> objectives,
        List<Constraint> constraints) {
      public Schema {
        problemId = required(problemId, "Problem ID");
        if (maxEvaluations < 1) {
          throw new IllegalArgumentException("Maximum evaluations must be positive.");
        }
        variables = List.copyOf(variables == null ? List.of() : variables);
        objectives = List.copyOf(objectives == null ? List.of() : objectives);
        constraints = List.copyOf(constraints == null ? List.of() : constraints);
        if (variables.isEmpty())
          throw new IllegalArgumentException("At least one variable is required.");
        if (objectives.isEmpty())
          throw new IllegalArgumentException("At least one objective is required.");
      }

      public List<String> objectiveNames() {
        return objectives.stream().map(Objective::column).toList();
      }

      public List<String> objectiveDirections() {
        return objectives.stream().map(objective -> objective.direction().name()).toList();
      }

      public List<String> evaluatorVariableNames() {
        return variables.stream().map(Variable::name).toList();
      }
    }

    private ProblemMetadata() {}

    public static DataTableSpec attach(
        final DataTableSpec input, final String markerColumn, final Schema schema) {
      final int marker = input.findColumnIndex(markerColumn);
      if (marker < 0)
        throw new IllegalArgumentException("Metadata marker column is missing: " + markerColumn);
      final DataColumnSpec[] columns = new DataColumnSpec[input.getNumColumns()];
      for (int index = 0; index < columns.length; index++)
        columns[index] = input.getColumnSpec(index);
      final DataColumnSpec original = columns[marker];
      final DataColumnSpecCreator creator = new DataColumnSpecCreator(original);
      creator.setProperties(
          original.getProperties().cloneAndOverwrite(Map.of(PROPERTY_KEY, encode(schema))));
      columns[marker] = creator.createSpec();
      return new DataTableSpec(input.getName(), columns);
    }

    public static Optional<Schema> read(final DataTableSpec spec) throws InvalidSettingsException {
      if (spec == null) return Optional.empty();
      for (DataColumnSpec column : spec) {
        final String encoded = column.getProperties().getProperty(PROPERTY_KEY);
        if (encoded != null && !encoded.isBlank()) return Optional.of(decode(encoded));
      }
      return Optional.empty();
    }

    public static Schema require(final DataTableSpec spec) throws InvalidSettingsException {
      return read(spec)
          .orElseThrow(
              () ->
                  new InvalidSettingsException(
                      "Problem metadata is unavailable. Reconfigure Problem Setup."));
    }

    private static String encode(final Schema schema) {
      try {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
          output.writeInt(FORMAT_VERSION);
          output.writeUTF(schema.problemId());
          output.writeLong(schema.maxEvaluations());
          output.writeInt(schema.variables().size());
          for (Variable variable : schema.variables()) {
            output.writeUTF(variable.name());
            output.writeUTF(variable.type());
            output.writeDouble(variable.lower());
            output.writeDouble(variable.upper());
            output.writeUTF(variable.representation());
            output.writeUTF(variable.encoding());
            output.writeInt(variable.bits());
          }
          output.writeInt(schema.objectives().size());
          for (Objective objective : schema.objectives()) {
            output.writeUTF(objective.column());
            output.writeUTF(objective.direction().name());
            output.writeBoolean(objective.referencePoint() != null);
            if (objective.referencePoint() != null) output.writeDouble(objective.referencePoint());
          }
          output.writeInt(schema.constraints().size());
          for (Constraint constraint : schema.constraints()) {
            output.writeUTF(constraint.column());
            output.writeUTF(constraint.relation().name());
            output.writeDouble(constraint.threshold());
            output.writeDouble(constraint.tolerance());
          }
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
      } catch (IOException exception) {
        throw new IllegalStateException("Could not encode problem metadata.", exception);
      }
    }

    private static Schema decode(final String encoded) throws InvalidSettingsException {
      try {
        final byte[] bytes = Base64.getUrlDecoder().decode(encoded);
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
          final int version = input.readInt();
          if (version != FORMAT_VERSION) {
            throw new InvalidSettingsException(
                "Unsupported IdeaFlow problem metadata version: " + version);
          }
          final String problemId = input.readUTF();
          final long maxEvaluations = input.readLong();
          final List<Variable> variables = new ArrayList<>();
          final int variableCount = checkedCount(input.readInt());
          for (int index = 0; index < variableCount; index++) {
            variables.add(
                new Variable(
                    input.readUTF(),
                    input.readUTF(),
                    input.readDouble(),
                    input.readDouble(),
                    input.readUTF(),
                    input.readUTF(),
                    input.readInt()));
          }
          final List<Objective> objectives = new ArrayList<>();
          final int objectiveCount = checkedCount(input.readInt());
          for (int index = 0; index < objectiveCount; index++) {
            final String column = input.readUTF();
            final OptimizationDirection direction = OptimizationDirection.parse(input.readUTF());
            final Double reference = input.readBoolean() ? input.readDouble() : null;
            objectives.add(new Objective(column, direction, reference));
          }
          final List<Constraint> constraints = new ArrayList<>();
          final int constraintCount = checkedCount(input.readInt());
          for (int index = 0; index < constraintCount; index++) {
            constraints.add(
                new Constraint(
                    input.readUTF(),
                    ConstraintRelation.valueOf(input.readUTF()),
                    input.readDouble(),
                    input.readDouble()));
          }
          return new Schema(problemId, maxEvaluations, variables, objectives, constraints);
        }
      } catch (IOException | IllegalArgumentException exception) {
        throw new InvalidSettingsException("Invalid IdeaFlow problem metadata.", exception);
      }
    }

    private static int checkedCount(final int count) throws InvalidSettingsException {
      if (count < 0 || count > 1_000_000) {
        throw new InvalidSettingsException("Invalid metadata item count.");
      }
      return count;
    }

    private static String required(final String value, final String label) {
      if (value == null || value.isBlank())
        throw new IllegalArgumentException(label + " is required.");
      return value.trim();
    }
  }
}
