package org.ideaflow.nodes.initialization;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.random.RandomGenerator;

import org.ideaflow.api.IdeaFlowState;
import org.ideaflow.api.IdeaFlowStateCell;
import org.ideaflow.core.DeterministicRandom;
import org.ideaflow.core.BinaryEncoding;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.ideaflow.knime.PopulationState;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

public final class InitialPopulationNodeModel extends NodeModel {
    static final String CFG_POPULATION_SIZE = "population_size";
    static final String CFG_POPULATION_ID = "population_id";

    private final SettingsModelIntegerBounded m_populationSize =
        new SettingsModelIntegerBounded(CFG_POPULATION_SIZE, 50, 1, 1_000_000);
    private final SettingsModelString m_populationId = new SettingsModelString(CFG_POPULATION_ID, "population-0");

    private record Variable(String name, String type, double lower, double upper, String representation,
        String encoding, int bits) {
        boolean encoded() {
            return "BINARY_ENCODED".equals(representation);
        }
    }

    InitialPopulationNodeModel() {
        super(1, 1);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] input) throws InvalidSettingsException {
        requireProblemSetup(input[0]);
        validateSettingsValues();
        final ProblemMetadata.Schema problem = ProblemMetadata.require(input[0]);
        return new DataTableSpec[]{outputSpec(fromMetadata(problem.variables()), problem)};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] input, final ExecutionContext execution)
            throws Exception {
        requireProblemSetup(input[0].getDataTableSpec());
        validateSettingsValues();
        final List<Variable> variables = readVariables(input[0]);
        if (variables.isEmpty()) throw new InvalidSettingsException("Problem Definition contains no variable rows.");
        final ProblemMetadata.Schema metadata = ProblemMetadata.read(input[0].getDataTableSpec()).orElse(null);
        final DataTableSpec outputSpec = outputSpec(variables, metadata);
        final Run run = readRun(input[0]);
        final BufferedDataContainer output = execution.createDataContainer(outputSpec);
        final long total = m_populationSize.getIntValue();
        long done = 0;
        for (int individual = 0; individual < m_populationSize.getIntValue(); individual++) {
            final RandomGenerator random = DeterministicRandom.forScope(run.seed(), run.id(),
                m_populationId.getStringValue(), "initialization", individual);
            final DataCell[] cells = new DataCell[outputSpec.getNumColumns()];
            int column = 0;
            for (Variable variable : variables) {
                if (variable.encoded()) {
                    for (int bit = 0; bit < variable.bits(); bit++) {
                        cells[column++] = new IntCell(random.nextBoolean() ? 1 : 0);
                    }
                } else {
                    cells[column++] = directValue(variable, random);
                }
            }
            cells[column++] = new LongCell(0);
            final String individualId =
                run.id() + ':' + m_populationId.getStringValue() + ':' + individual;
            final IdeaFlowState state = IdeaFlowState.empty()
                .with(IdeaFlowState.RUN, run.id())
                .with(IdeaFlowState.POPULATION, m_populationId.getStringValue())
                .with(IdeaFlowState.INDIVIDUAL, individualId)
                .with(IdeaFlowState.SEED, run.seed())
                .with(IdeaFlowState.INITIAL_POPULATION_SIZE, m_populationSize.getIntValue())
                .with(IdeaFlowState.GENERATION, 0L)
                .with(IdeaFlowState.EVALUATED, false);
            cells[column] = new IdeaFlowStateCell(state);
            output.addRowToTable(new DefaultRow("Individual-" + individual, cells));
            done++;
            execution.checkCanceled();
            execution.setProgress((double)done / total);
        }
        output.close();
        return new BufferedDataTable[]{output.getTable()};
    }

    private static DataCell directValue(final Variable variable, final RandomGenerator random)
            throws InvalidSettingsException {
        return switch (variable.type()) {
            case "REAL" -> new DoubleCell(random.nextDouble(variable.lower(), Math.nextUp(variable.upper())));
            case "INTEGER" -> {
                final long lower = (long)variable.lower();
                final long upperExclusive = (long)variable.upper() + 1L;
                if (lower < Integer.MIN_VALUE || upperExclusive - 1 > Integer.MAX_VALUE) {
                    throw new InvalidSettingsException("Whole-number bounds exceed KNIME Integer range: " + variable.name());
                }
                yield new IntCell((int)random.nextLong(lower, upperExclusive));
            }
            case "BINARY" -> new IntCell(random.nextBoolean() ? 1 : 0);
            default -> throw new InvalidSettingsException("Unsupported variable type: " + variable.type());
        };
    }

    private static List<Variable> readVariables(final BufferedDataTable problem) throws InvalidSettingsException {
        final DataTableSpec spec = problem.getDataTableSpec();
        final int kind = spec.findColumnIndex("kind");
        final int name = spec.findColumnIndex("name");
        final int type = spec.findColumnIndex("type");
        final int lower = spec.findColumnIndex("lower_bound");
        final int upper = spec.findColumnIndex("upper_bound");
        final int representation = spec.findColumnIndex("representation");
        final int encoding = spec.findColumnIndex("encoding");
        final int bits = spec.findColumnIndex("bits");
        final int problemIdIndex = spec.findColumnIndex("problem_id");
        final List<Variable> result = new ArrayList<>();
        String expectedProblem = null;
        for (DataRow row : problem) {
            final String rowProblem = row.getCell(problemIdIndex).toString();
            if (expectedProblem == null) expectedProblem = rowProblem;
            else if (!expectedProblem.equals(rowProblem)) {
                throw new InvalidSettingsException("Population Initialization accepts one problem definition at a time.");
            }
            if (!"variable".equalsIgnoreCase(row.getCell(kind).toString())) continue;
            if (row.getCell(lower).isMissing() || row.getCell(upper).isMissing()) {
                throw new InvalidSettingsException("Variable bounds are required: " + row.getCell(name));
            }
            final String variableType = row.getCell(type).toString().toUpperCase(Locale.ROOT);
            final double low = ((DoubleValue)row.getCell(lower)).getDoubleValue();
            final double high = ((DoubleValue)row.getCell(upper)).getDoubleValue();
            final String variableRepresentation = row.getCell(representation).toString().toUpperCase(Locale.ROOT);
            final String variableEncoding = row.getCell(encoding).toString().toUpperCase(Locale.ROOT);
            final int variableBits = row.getCell(bits).isMissing() ? 1 : ((IntValue)row.getCell(bits)).getIntValue();
            if (low > high) throw new InvalidSettingsException("Lower bound exceeds upper bound for " + row.getCell(name));
            if (!Set.of("DIRECT", "BINARY_ENCODED").contains(variableRepresentation)) {
                throw new InvalidSettingsException("Unsupported representation for " + row.getCell(name));
            }
            if ("BINARY_ENCODED".equals(variableRepresentation)
                    && (!"NATURAL".equals(variableEncoding) || variableBits < 1 || variableBits > 52)) {
                throw new InvalidSettingsException("Invalid binary encoding for " + row.getCell(name));
            }
            result.add(new Variable(row.getCell(name).toString(), variableType, low, high, variableRepresentation,
                variableEncoding, variableBits));
        }
        return result;
    }

    private record Run(String id, long seed) { }

    private static Run readRun(final BufferedDataTable setup) throws InvalidSettingsException {
        final DataTableSpec spec = setup.getDataTableSpec();
        final int runIndex = spec.findColumnIndex("run_id");
        final int seedIndex = spec.findColumnIndex("seed");
        Run result = null;
        for (DataRow row : setup) {
            if (!(row.getCell(seedIndex) instanceof LongValue seedValue)) {
                throw new InvalidSettingsException("Problem Setup contains an invalid random seed.");
            }
            final Run current = new Run(row.getCell(runIndex).toString(), seedValue.getLongValue());
            if (current.id().isBlank()) throw new InvalidSettingsException("Problem Setup contains an empty run ID.");
            if (result == null) result = current;
            else if (!result.equals(current)) {
                throw new InvalidSettingsException(
                    "Problem Setup must contain one consistent run ID and seed on every definition row.");
            }
        }
        if (result == null) throw new InvalidSettingsException("Problem Setup contains no definition rows.");
        return result;
    }

    private static List<Variable> fromMetadata(final List<ProblemMetadata.Variable> variables) {
        return variables.stream().map(variable -> new Variable(variable.name(), variable.type(), variable.lower(),
            variable.upper(), variable.representation(), variable.encoding(), variable.bits())).toList();
    }

    private static DataTableSpec outputSpec(final List<Variable> variables, final ProblemMetadata.Schema metadata)
            throws InvalidSettingsException {
        final List<DataColumnSpec> columns = new ArrayList<>();
        final Set<String> names = new HashSet<>();
        for (Variable variable : variables) {
            if (variable.encoded()) {
                for (int bit = 0; bit < variable.bits(); bit++) {
                    addColumn(columns, names, BinaryEncoding.geneName(variable.name(), bit), IntCell.TYPE);
                }
            } else {
                final DataType type = switch (variable.type()) {
                    case "REAL" -> DoubleCell.TYPE;
                    case "INTEGER", "BINARY" -> IntCell.TYPE;
                    default -> throw new InvalidSettingsException("Unsupported variable type: " + variable.type());
                };
                addColumn(columns, names, variable.name(), type);
            }
        }
        columns.add(new DataColumnSpecCreator(PopulationState.NFE, LongCell.TYPE).createSpec());
        columns.add(PopulationState.column());
        final DataTableSpec result = new DataTableSpec(columns.toArray(DataColumnSpec[]::new));
        return metadata == null ? result : ProblemMetadata.attach(result, PopulationState.COLUMN, metadata);
    }

    private static void addColumn(final List<DataColumnSpec> columns, final Set<String> names, final String name,
            final DataType type) throws InvalidSettingsException {
        if (!names.add(name)) throw new InvalidSettingsException("Duplicate generated population column: " + name);
        columns.add(new DataColumnSpecCreator(name, type).createSpec());
    }

    private static void requireProblemSetup(final DataTableSpec spec) throws InvalidSettingsException {
        for (String required : List.of("experiment_id", "run_id", "seed", "max_evaluations", "problem_id",
            "kind", "name", "type", "lower_bound", "upper_bound",
            "representation", "encoding", "bits")) {
            if (spec.findColumnIndex(required) < 0) {
                throw new InvalidSettingsException("Input must come from Problem Setup; missing " + required);
            }
        }
    }

    private void validateSettingsValues() throws InvalidSettingsException {
        if (m_populationId.getStringValue().isBlank()) throw new InvalidSettingsException("Population ID is required.");
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_populationSize.saveSettingsTo(settings);
        m_populationId.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_populationSize.validateSettings(settings);
        m_populationId.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_populationSize.loadSettingsFrom(settings);
        m_populationId.loadSettingsFrom(settings);
    }

    @Override protected void loadInternals(final File directory, final ExecutionMonitor monitor)
        throws IOException, CanceledExecutionException { }
    @Override protected void saveInternals(final File directory, final ExecutionMonitor monitor)
        throws IOException, CanceledExecutionException { }
    @Override protected void reset() { }
}
