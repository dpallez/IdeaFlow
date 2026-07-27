package org.ideaflow.nodes.migration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

import org.ideaflow.api.Candidate;
import org.ideaflow.api.IdeaFlowState;
import org.ideaflow.api.IdeaFlowStateCell;
import org.ideaflow.api.ObjectiveDefinition;
import org.ideaflow.api.ReservedColumns;
import org.ideaflow.core.CrowdingDistance;
import org.ideaflow.core.DeterministicRandom;
import org.ideaflow.core.FastNonDominatedSort;
import org.ideaflow.knime.KnimeTableSupport;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.ideaflow.knime.PopulationState;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
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

/** Migration between population groups using the objectives declared by Problem Setup. */
public final class PopulationMigrationNodeModel extends NodeModel {
    static final String CFG_COUNT = "migrant_count";
    static final String CFG_TOPOLOGY = "migration_topology";
    static final String CFG_REPLACEMENT = "migration_replacement";
    private final SettingsModelIntegerBounded m_count =
        new SettingsModelIntegerBounded(CFG_COUNT, 1, 1, Integer.MAX_VALUE);
    private final SettingsModelString m_topology = new SettingsModelString(CFG_TOPOLOGY, "RING");
    private final SettingsModelString m_replacement =
        new SettingsModelString(CFG_REPLACEMENT, "REPLACE_WORST");

    PopulationMigrationNodeModel() { super(1, 1); }

    @Override protected DataTableSpec[] configure(final DataTableSpec[] input) throws InvalidSettingsException {
        validateSettingsValues();
        validate(input[0]);
        return new DataTableSpec[]{input[0]};
    }

    @Override protected BufferedDataTable[] execute(final BufferedDataTable[] input,
            final ExecutionContext execution) throws Exception {
        final DataTableSpec spec = input[0].getDataTableSpec();
        validateSettingsValues();
        validate(spec);
        final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
        final int stateIndex = spec.findColumnIndex(PopulationState.COLUMN);
        final Map<String, Map<String, List<DataRow>>> runs = new LinkedHashMap<>();
        for (DataRow row : input[0]) {
            runs.computeIfAbsent(PopulationState.run(row, spec), ignored -> new LinkedHashMap<>())
                .computeIfAbsent(PopulationState.population(row, spec), ignored -> new ArrayList<>()).add(row);
        }
        final BufferedDataContainer output = execution.createDataContainer(spec);
        long key = 0;
        for (Map.Entry<String, Map<String, List<DataRow>>> runEntry : runs.entrySet()) {
            final List<String> islands = new ArrayList<>(runEntry.getValue().keySet());
            islands.sort(String::compareTo);
            if (islands.size() < 2) {
                throw new InvalidSettingsException("Migration requires at least two populations per run.");
            }
            final long migrationNfe = runEntry.getValue().values().stream().flatMap(List::stream)
                .mapToLong(row -> PopulationState.nfe(row, spec)).max().orElse(0L);
            final Map<String, List<DataRow>> incoming = new LinkedHashMap<>();
            for (String island : islands) incoming.put(island, new ArrayList<>());
            for (int sourceIndex = 0; sourceIndex < islands.size(); sourceIndex++) {
                final String source = islands.get(sourceIndex);
                final List<DataRow> sourceRows =
                    preferenceOrder(new ArrayList<>(runEntry.getValue().get(source)), spec, problem);
                final int migrantCount = Math.min(m_count.getIntValue(), sourceRows.size());
                for (String destination : destinations(islands, sourceIndex, sourceRows.get(0), spec, migrationNfe)) {
                    for (int migrantIndex = 0; migrantIndex < migrantCount; migrantIndex++) {
                        incoming.get(destination).add(migrant(sourceRows.get(migrantIndex), source, destination,
                            migrantIndex, migrationNfe, stateIndex, spec));
                    }
                }
            }
            final Map<String, List<DataRow>> next = new LinkedHashMap<>();
            for (String island : islands) {
                next.put(island, migratedPopulation(runEntry.getValue().get(island), incoming.get(island),
                    spec, problem));
                for (DataRow row : next.get(island)) {
                    output.addRowToTable(new DefaultRow("Migrated" + key++, row));
                    execution.checkCanceled();
                }
            }
        }
        output.close();
        return new BufferedDataTable[]{output.getTable()};
    }

    private List<String> destinations(final List<String> populations, final int sourceIndex,
            final DataRow sourceRow, final DataTableSpec spec, final long nfe) throws InvalidSettingsException {
        if ("RING".equals(m_topology.getStringValue())) {
            return List.of(populations.get((sourceIndex + 1) % populations.size()));
        }
        if ("ALL_TO_ALL".equals(m_topology.getStringValue())) {
            final List<String> result = new ArrayList<>(populations);
            result.remove(sourceIndex);
            return result;
        }
        final RandomGenerator random = DeterministicRandom.forScope(PopulationState.seed(sourceRow, spec),
            PopulationState.run(sourceRow, spec), nfe, "population-migration", populations.get(sourceIndex));
        int destinationIndex = random.nextInt(populations.size() - 1);
        if (destinationIndex >= sourceIndex) destinationIndex++;
        return List.of(populations.get(destinationIndex));
    }

    private static DataRow migrant(final DataRow row, final String source, final String destination,
            final int migrantIndex, final long nfe, final int stateIndex, final DataTableSpec spec)
            throws InvalidSettingsException {
        final DataCell[] cells = new DataCell[spec.getNumColumns()];
        for (int column = 0; column < cells.length; column++) cells[column] = row.getCell(column);
        final String individual = PopulationState.individual(row, spec) + ":from:" + source + ":to:"
            + destination + ":nfe:" + nfe + ":" + migrantIndex;
        final IdeaFlowState state = PopulationState.get(row, spec)
            .with(IdeaFlowState.POPULATION, destination)
            .with(IdeaFlowState.INDIVIDUAL, individual);
        cells[stateIndex] = new IdeaFlowStateCell(state);
        return new DefaultRow("Migrant-" + individual, cells);
    }

    private List<DataRow> migratedPopulation(final List<DataRow> current, final List<DataRow> incoming,
            final DataTableSpec spec, final ProblemMetadata.Schema problem) throws InvalidSettingsException {
        final List<DataRow> result = new ArrayList<>(current);
        if (incoming.isEmpty()) return result;
        final List<DataRow> preferredIncoming = preferenceOrder(new ArrayList<>(incoming), spec, problem);
        if ("ADD".equals(m_replacement.getStringValue())) {
            result.addAll(preferredIncoming);
            return result;
        }
        final int replacements = Math.min(result.size(), preferredIncoming.size());
        final List<DataRow> preference = preferenceOrder(new ArrayList<>(result), spec, problem);
        final Set<DataRow> discarded = Collections.newSetFromMap(new IdentityHashMap<>());
        discarded.addAll(preference.subList(preference.size() - replacements, preference.size()));
        result.removeIf(discarded::contains);
        result.addAll(preferredIncoming.subList(0, replacements));
        return result;
    }

    private static List<DataRow> preferenceOrder(final List<DataRow> rows, final DataTableSpec spec,
            final ProblemMetadata.Schema problem) throws InvalidSettingsException {
        final List<String> names = problem.objectiveNames();
        final int[] objectives = KnimeTableSupport.requireNumericColumns(spec, names);
        final int violation = spec.findColumnIndex(ReservedColumns.CONSTRAINT_VIOLATION);
        final List<ObjectiveDefinition> definitions = KnimeTableSupport.objectives(names,
            problem.objectives().stream().map(ProblemMetadata.Objective::direction).toList(), List.of());
        final List<Candidate> candidates = new ArrayList<>();
        for (DataRow row : rows) {
            candidates.add(KnimeTableSupport.candidate(row, new int[0], objectives, violation, List.of(), names));
        }
        final List<List<Integer>> fronts = FastNonDominatedSort.sort(candidates, definitions);
        final double[] crowding = CrowdingDistance.compute(candidates, fronts, definitions);
        final int[] ranks = new int[rows.size()];
        for (int rank = 0; rank < fronts.size(); rank++) {
            for (int index : fronts.get(rank)) ranks[index] = rank;
        }
        final List<Integer> order = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) order.add(index);
        order.sort(Comparator.comparingInt((Integer index) -> ranks[index])
            .thenComparing(Comparator.comparingDouble((Integer index) -> crowding[index]).reversed())
            .thenComparing(index -> candidates.get(index).id()));
        return order.stream().map(rows::get).toList();
    }

    private static void validate(final DataTableSpec spec) throws InvalidSettingsException {
        final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
        KnimeTableSupport.requireNumericColumns(spec, problem.objectiveNames());
        PopulationState.requireVisibleColumns(spec);
    }

    private void validateSettingsValues() throws InvalidSettingsException {
        if (!List.of("RING", "RANDOM", "ALL_TO_ALL").contains(m_topology.getStringValue())) {
            throw new InvalidSettingsException("Unsupported migration topology: " + m_topology.getStringValue());
        }
        if (!List.of("REPLACE_WORST", "ADD").contains(m_replacement.getStringValue())) {
            throw new InvalidSettingsException("Unsupported migration replacement: "
                + m_replacement.getStringValue());
        }
    }

    @Override protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_count.saveSettingsTo(settings);
        m_topology.saveSettingsTo(settings);
        m_replacement.saveSettingsTo(settings);
    }
    @Override protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_count.validateSettings(settings);
        m_topology.validateSettings(settings);
        m_replacement.validateSettings(settings);
    }
    @Override protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_count.loadSettingsFrom(settings);
        m_topology.loadSettingsFrom(settings);
        m_replacement.loadSettingsFrom(settings);
    }
    @Override protected void loadInternals(final File directory, final ExecutionMonitor monitor)
            throws IOException, CanceledExecutionException { }
    @Override protected void saveInternals(final File directory, final ExecutionMonitor monitor)
            throws IOException, CanceledExecutionException { }
    @Override protected void reset() { }
}
