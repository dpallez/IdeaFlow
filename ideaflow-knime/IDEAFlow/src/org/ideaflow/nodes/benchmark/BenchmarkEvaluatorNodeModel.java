package org.ideaflow.nodes.benchmark;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ideaflow.core.BinaryEncoding;
import org.ideaflow.core.FormulaExpression;
import org.ideaflow.knime.EvaluationFinalizer;
import org.ideaflow.knime.KnimeTableSupport;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/** Built-in benchmark calculation plus exact-evaluation finalization and accounting. */
public final class BenchmarkEvaluatorNodeModel extends NodeModel {
    static final String CFG_VARIABLES = "variable_columns";
    static final String CFG_OBJECTIVES = "objective_columns";
    static final String CFG_FUNCTION = "benchmark";
    static final String CFG_METHOD = "evaluation_method";
    static final String CFG_FORMULAS = "formula_definitions";

    record FormulaDefinition(String result, String expression) { }
    private record CompiledFormula(String result, FormulaExpression expression) { }

    // The legacy column settings remain persisted so old saved settings load, but
    // current workflows derive columns from the connected Problem Definition.
    private final SettingsModelString m_legacyVariables =
        new SettingsModelString(CFG_VARIABLES, "x0,x1");
    private final SettingsModelString m_legacyObjectives =
        new SettingsModelString(CFG_OBJECTIVES, "fitness");
    private final SettingsModelString m_function =
        new SettingsModelString(CFG_FUNCTION, "ACKLEY");
    private final SettingsModelString m_method =
        new SettingsModelString(CFG_METHOD, "BUILT_IN");
    private final SettingsModelString m_formulas =
        new SettingsModelString(CFG_FORMULAS, "");

    BenchmarkEvaluatorNodeModel() {
        super(2, 2);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] input) throws InvalidSettingsException {
        EvaluationFinalizer.requirePopulationStructure(input[0]);
        EvaluationFinalizer.requireProblemDefinitionStructure(input[1]);
        final ProblemMetadata.Schema metadata = ProblemMetadata.require(input[1]);
        final EvaluationFinalizer.ProblemDetails problem = EvaluationFinalizer.fromMetadata(input[1]);
        final DataTableSpec decoded = decodedSpec(input[0], metadata);
        validatePopulation(input[0], decoded, metadata);
        final DataTableSpec calculated;
        switch (method()) {
            case "BUILT_IN" -> {
                validateBenchmark(metadata);
                calculated = calculatedSpec(decoded, problem);
            }
            case "FORMULAS" -> {
                final List<CompiledFormula> formulas = compiledFormulas(decoded, metadata);
                calculated = formulaSpec(decoded, formulas);
            }
            case "EXISTING_RESULTS" -> calculated = decoded;
            default -> throw new InvalidSettingsException("Unsupported evaluation method.");
        }
        EvaluationFinalizer.validateEvaluationColumns(calculated, problem);
        return new DataTableSpec[]{EvaluationFinalizer.evaluatedPopulationSpec(calculated, problem),
            EvaluationFinalizer.eventSpec(problem)};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] input, final ExecutionContext execution)
            throws Exception {
        final ProblemMetadata.Schema metadata = ProblemMetadata.require(input[1].getDataTableSpec());
        final EvaluationFinalizer.ProblemDetails problem =
            EvaluationFinalizer.readProblemDefinition(input[1]);
        final DataTableSpec inputSpec = input[0].getDataTableSpec();
        final DataTableSpec decodedSpec = decodedSpec(inputSpec, metadata);
        validatePopulation(inputSpec, decodedSpec, metadata);
        final List<String> variableNames = metadata.evaluatorVariableNames();
        final List<String> objectiveNames = problem.objectives().stream()
            .map(EvaluationFinalizer.Objective::column).toList();
        final List<CompiledFormula> formulas;
        final DataTableSpec calculatedSpec;
        switch (method()) {
            case "BUILT_IN" -> {
                validateBenchmark(metadata);
                formulas = List.of();
                calculatedSpec = calculatedSpec(decodedSpec, problem);
            }
            case "FORMULAS" -> {
                formulas = compiledFormulas(decodedSpec, metadata);
                calculatedSpec = formulaSpec(decodedSpec, formulas);
            }
            case "EXISTING_RESULTS" -> {
                formulas = List.of();
                calculatedSpec = decodedSpec;
            }
            default -> throw new InvalidSettingsException("Unsupported evaluation method.");
        }
        EvaluationFinalizer.validateEvaluationColumns(calculatedSpec, problem);
        final int[] indices = KnimeTableSupport.requireNumericColumns(calculatedSpec, variableNames);
        final BufferedDataContainer calculated = execution.createDataContainer(calculatedSpec);
        long rowNumber = 0;
        for (DataRow row : input[0]) {
            final DataCell[] cells = KnimeTableSupport.copyToSpec(row, inputSpec, calculatedSpec);
            decode(row, inputSpec, cells, calculatedSpec, metadata);
            if ("BUILT_IN".equals(method())) {
                final double[] point = point(cells, indices, row, variableNames);
                final double[] values = evaluate(point, objectiveNames.size());
                for (int index = 0; index < values.length; index++) {
                    cells[calculatedSpec.findColumnIndex(objectiveNames.get(index))] = new DoubleCell(values[index]);
                }
            } else if ("FORMULAS".equals(method())) {
                final double[] point = point(cells, indices, row, variableNames);
                final Map<String, Double> variables = new LinkedHashMap<>();
                for (int index = 0; index < variableNames.size(); index++) {
                    variables.put(variableNames.get(index), point[index]);
                }
                for (CompiledFormula formula : formulas) {
                    try {
                        cells[calculatedSpec.findColumnIndex(formula.result())] =
                            new DoubleCell(formula.expression().evaluate(variables));
                    } catch (IllegalArgumentException exception) {
                        throw new InvalidSettingsException("Formula for " + formula.result()
                            + " failed on row " + row.getKey() + ": " + exception.getMessage(), exception);
                    }
                }
            }
            calculated.addRowToTable(new DefaultRow(row.getKey(), cells));
            rowNumber++;
            execution.checkCanceled();
            if (input[0].size() > 0) execution.setProgress(0.5 * rowNumber / input[0].size());
        }
        calculated.close();
        return EvaluationFinalizer.finalizePopulation(calculated.getTable(), problem, execution,
            switch (method()) {
                case "BUILT_IN" -> "benchmark-" + m_function.getStringValue().toLowerCase(java.util.Locale.ROOT);
                case "FORMULAS" -> "formula-evaluator";
                default -> "external-evaluator";
            });
    }

    private static double[] point(final DataCell[] cells, final int[] indices, final DataRow row,
            final List<String> variableNames) throws InvalidSettingsException {
        final double[] point = new double[indices.length];
        for (int index = 0; index < indices.length; index++) {
            point[index] = KnimeTableSupport.number(cells[indices[index]], row, variableNames.get(index));
        }
        return point;
    }

    private static DataTableSpec calculatedSpec(final DataTableSpec input,
            final EvaluationFinalizer.ProblemDetails problem) {
        final List<DataColumnSpec> columns = new ArrayList<>();
        final List<String> objectives = problem.objectives().stream()
            .map(EvaluationFinalizer.Objective::column).toList();
        for (DataColumnSpec column : input) {
            if (!objectives.contains(column.getName())) columns.add(column);
        }
        for (String objective : objectives) {
            columns.add(new DataColumnSpecCreator(objective, DoubleCell.TYPE).createSpec());
        }
        return new DataTableSpec(input.getName(), columns.toArray(DataColumnSpec[]::new));
    }

    private static DataTableSpec formulaSpec(final DataTableSpec input, final List<CompiledFormula> formulas) {
        final DataColumnSpec[] results = formulas.stream()
            .map(formula -> KnimeTableSupport.doubleColumn(formula.result())).toArray(DataColumnSpec[]::new);
        return KnimeTableSupport.appendOrReplace(input, results);
    }

    private static DataTableSpec decodedSpec(final DataTableSpec input,
            final ProblemMetadata.Schema problem) {
        final List<DataColumnSpec> decoded = problem.variables().stream()
            .filter(ProblemMetadata.Variable::encoded)
            .map(variable -> KnimeTableSupport.doubleColumn(variable.name())).toList();
        return decoded.isEmpty() ? input
            : KnimeTableSupport.appendOrReplace(input, decoded.toArray(DataColumnSpec[]::new));
    }

    private static void decode(final DataRow row, final DataTableSpec inputSpec, final DataCell[] cells,
            final DataTableSpec outputSpec, final ProblemMetadata.Schema problem)
            throws InvalidSettingsException {
        for (ProblemMetadata.Variable variable : problem.variables()) {
            if (!variable.encoded()) continue;
            long code = 0L;
            for (int bit = 0; bit < variable.bits(); bit++) {
                final String gene = BinaryEncoding.geneName(variable.name(), bit);
                final DataCell cell = row.getCell(inputSpec.findColumnIndex(gene));
                if (!(cell instanceof IntValue value) || value.getIntValue() < 0 || value.getIntValue() > 1) {
                    throw new InvalidSettingsException(
                        "Evaluation requires 0/1 integer cells for encoded gene " + gene + '.');
                }
                code = (code << 1) | value.getIntValue();
            }
            cells[outputSpec.findColumnIndex(variable.name())] =
                new DoubleCell(BinaryEncoding.decode(code, variable.bits(), variable.lower(), variable.upper()));
        }
    }

    private double[] evaluate(final double[] x, final int objectiveCount) throws InvalidSettingsException {
        return switch (m_function.getStringValue()) {
            case "SPHERE" -> new double[]{Arrays.stream(x).map(value -> value * value).sum()};
            case "ACKLEY" -> {
                double squares = 0.0;
                double cosines = 0.0;
                for (double value : x) {
                    squares += value * value;
                    cosines += Math.cos(2.0 * Math.PI * value);
                }
                yield new double[]{-20.0 * Math.exp(-0.2 * Math.sqrt(squares / x.length))
                    - Math.exp(cosines / x.length) + 20.0 + Math.E};
            }
            case "ROSENBROCK" -> {
                double sum = 0.0;
                for (int index = 0; index < x.length - 1; index++) {
                    sum += 100.0 * Math.pow(x[index + 1] - x[index] * x[index], 2.0)
                        + Math.pow(x[index] - 1.0, 2.0);
                }
                yield new double[]{sum};
            }
            case "RASTRIGIN" -> {
                double sum = 10.0 * x.length;
                for (double value : x) sum += value * value - 10.0 * Math.cos(2.0 * Math.PI * value);
                yield new double[]{sum};
            }
            case "GRIEWANK" -> {
                double sum = 0.0;
                double product = 1.0;
                for (int index = 0; index < x.length; index++) {
                    sum += x[index] * x[index] / 4000.0;
                    product *= Math.cos(x[index] / Math.sqrt(index + 1.0));
                }
                yield new double[]{sum - product + 1.0};
            }
            case "ONEMAX" -> {
                double sum = 0.0;
                for (double value : x) {
                    if (value != 0.0 && value != 1.0) {
                        throw new InvalidSettingsException("OneMax requires binary values.");
                    }
                    sum += value;
                }
                yield new double[]{sum};
            }
            case "ZDT1", "ZDT2", "ZDT3" -> {
                final double f1 = x[0];
                final double g = 1.0 + 9.0 * Arrays.stream(x, 1, x.length).average().orElse(0.0);
                final double ratio = f1 / g;
                final double h = switch (m_function.getStringValue()) {
                    case "ZDT1" -> 1.0 - Math.sqrt(ratio);
                    case "ZDT2" -> 1.0 - ratio * ratio;
                    default -> 1.0 - Math.sqrt(ratio) - ratio * Math.sin(10.0 * Math.PI * f1);
                };
                yield new double[]{f1, g * h};
            }
            case "DTLZ2" -> {
                final int objectives = objectiveCount;
                final int k = x.length - objectives + 1;
                double g = 0.0;
                for (int index = x.length - k; index < x.length; index++) {
                    g += Math.pow(x[index] - 0.5, 2.0);
                }
                final double[] values = new double[objectives];
                for (int objective = 0; objective < objectives; objective++) {
                    double value = 1.0 + g;
                    for (int index = 0; index < objectives - objective - 1; index++) {
                        value *= Math.cos(x[index] * Math.PI / 2.0);
                    }
                    if (objective > 0) value *= Math.sin(x[objectives - objective - 1] * Math.PI / 2.0);
                    values[objective] = value;
                }
                yield values;
            }
            default -> throw new InvalidSettingsException("Unsupported benchmark.");
        };
    }

    private void validatePopulation(final DataTableSpec population, final DataTableSpec decoded,
            final ProblemMetadata.Schema problem)
            throws InvalidSettingsException {
        final List<String> variables = problem.evaluatorVariableNames();
        for (ProblemMetadata.Variable variable : problem.variables()) {
            if (variable.encoded()) {
                for (String gene : variable.populationColumns()) {
                    final int index = population.findColumnIndex(gene);
                    if (index < 0 || !population.getColumnSpec(index).getType().isCompatible(IntValue.class)) {
                        throw new InvalidSettingsException("Missing integer binary column: " + gene);
                    }
                }
            }
        }
        KnimeTableSupport.requireNumericColumns(decoded, variables);
    }

    private void validateBenchmark(final ProblemMetadata.Schema problem) throws InvalidSettingsException {
        final List<String> variables = problem.evaluatorVariableNames();
        final int objectiveCount = problem.objectives().size();
        if (!List.of("ACKLEY", "SPHERE", "ROSENBROCK", "RASTRIGIN", "GRIEWANK", "ONEMAX",
                "ZDT1", "ZDT2", "ZDT3", "DTLZ2").contains(m_function.getStringValue())) {
            throw new InvalidSettingsException("Unsupported benchmark.");
        }
        final boolean zdt = m_function.getStringValue().startsWith("ZDT");
        if (zdt && objectiveCount != 2) {
            throw new InvalidSettingsException("ZDT benchmarks require exactly two objectives.");
        }
        if ("DTLZ2".equals(m_function.getStringValue())
                && (objectiveCount < 2 || variables.size() < objectiveCount)) {
            throw new InvalidSettingsException(
                "DTLZ2 requires at least two objectives and at least as many variables.");
        }
        if (!zdt && !"DTLZ2".equals(m_function.getStringValue()) && objectiveCount != 1) {
            throw new InvalidSettingsException(m_function.getStringValue() + " requires one objective.");
        }
        if (zdt && variables.size() < 2) {
            throw new InvalidSettingsException("ZDT requires at least two variables.");
        }
    }

    private String method() throws InvalidSettingsException {
        final String value = m_method.getStringValue();
        if (List.of("BUILT_IN", "FORMULAS", "EXISTING_RESULTS").contains(value)) return value;
        throw new InvalidSettingsException("Unsupported evaluation method: " + value);
    }

    private List<CompiledFormula> compiledFormulas(final DataTableSpec decoded,
            final ProblemMetadata.Schema problem) throws InvalidSettingsException {
        final List<FormulaDefinition> definitions = decodeFormulaSettings(m_formulas.getStringValue());
        final List<String> required = new ArrayList<>(problem.objectiveNames());
        required.addAll(problem.constraints().stream().map(ProblemMetadata.Constraint::column).toList());
        final Set<String> expected = Set.copyOf(required);
        final Set<String> seen = new HashSet<>();
        final Set<String> allowedVariables = Set.copyOf(problem.evaluatorVariableNames());
        final List<CompiledFormula> result = new ArrayList<>();
        for (FormulaDefinition definition : definitions) {
            if (!expected.contains(definition.result())) {
                throw new InvalidSettingsException(
                    "Formula result is not declared in Problem Setup: " + definition.result());
            }
            if (!seen.add(definition.result())) {
                throw new InvalidSettingsException("Duplicate formula result: " + definition.result());
            }
            final FormulaExpression expression;
            try {
                expression = FormulaExpression.compile(definition.expression());
            } catch (IllegalArgumentException exception) {
                throw new InvalidSettingsException("Invalid formula for " + definition.result()
                    + ": " + exception.getMessage(), exception);
            }
            for (String variable : expression.variables()) {
                if (!allowedVariables.contains(variable)) {
                    throw new InvalidSettingsException("Formula for " + definition.result()
                        + " uses unknown variable '" + variable + "'. Available variables: "
                        + String.join(", ", allowedVariables));
                }
            }
            result.add(new CompiledFormula(definition.result(), expression));
        }
        if (!seen.equals(expected)) {
            final List<String> missing = required.stream().filter(name -> !seen.contains(name)).toList();
            throw new InvalidSettingsException("Add formulas for: " + String.join(", ", missing));
        }
        KnimeTableSupport.requireNumericColumns(decoded, problem.evaluatorVariableNames());
        return List.copyOf(result);
    }

    static String encodeFormulaSettings(final List<FormulaDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) return "";
        return definitions.stream().map(definition -> encode(definition.result()) + ":" + encode(definition.expression()))
            .collect(java.util.stream.Collectors.joining(","));
    }

    static List<FormulaDefinition> decodeFormulaSettings(final String encoded) throws InvalidSettingsException {
        if (encoded == null || encoded.isBlank()) return List.of();
        final List<FormulaDefinition> result = new ArrayList<>();
        try {
            for (String item : encoded.split(",")) {
                final int separator = item.indexOf(':');
                if (separator < 1) throw new IllegalArgumentException();
                result.add(new FormulaDefinition(decode(item.substring(0, separator)),
                    decode(item.substring(separator + 1))));
            }
        } catch (IllegalArgumentException exception) {
            throw new InvalidSettingsException("Invalid formula settings.", exception);
        }
        return List.copyOf(result);
    }

    private static String encode(final String value) {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(final String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private SettingsModelString[] models() {
        return new SettingsModelString[]{m_legacyVariables, m_legacyObjectives, m_function, m_method, m_formulas};
    }

    @Override protected void saveSettingsTo(final NodeSettingsWO settings) {
        for (SettingsModelString model : models()) model.saveSettingsTo(settings);
    }
    @Override protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_function.validateSettings(settings);
        try { m_method.validateSettings(settings); } catch (InvalidSettingsException ignored) { }
        try { m_formulas.validateSettings(settings); } catch (InvalidSettingsException ignored) { }
    }
    @Override protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_function.loadSettingsFrom(settings);
        try { m_method.loadSettingsFrom(settings); } catch (InvalidSettingsException ignored) { }
        try { m_formulas.loadSettingsFrom(settings); } catch (InvalidSettingsException ignored) { }
    }
    @Override protected void loadInternals(final File directory, final ExecutionMonitor monitor)
        throws IOException, CanceledExecutionException { }
    @Override protected void saveInternals(final File directory, final ExecutionMonitor monitor)
        throws IOException, CanceledExecutionException { }
    @Override protected void reset() { }
}
