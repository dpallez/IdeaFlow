package org.ideaflow.nodes.archive;

import java.io.*;
import java.util.*;
import org.ideaflow.api.*;
import org.ideaflow.core.*;
import org.ideaflow.knime.KnimeTableSupport;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.ideaflow.knime.PopulationState;
import org.knime.core.data.*;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.*;
import org.knime.core.node.defaultnodesettings.*;
import org.knime.core.node.port.PortType;

final class ExternalArchiveNodeModel extends NodeModel {
    static final String CFG_MODE = "archive_mode", CFG_MAX_SIZE = "max_size", CFG_GROUPING = "grouping";
    private final SettingsModelString mode = new SettingsModelString(CFG_MODE, "PARETO");
    private final SettingsModelString grouping = new SettingsModelString(CFG_GROUPING, "RUN");
    private final SettingsModelIntegerBounded maxSize = new SettingsModelIntegerBounded(CFG_MAX_SIZE, 100, 0, Integer.MAX_VALUE);

    ExternalArchiveNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE_OPTIONAL, BufferedDataTable.TYPE, BufferedDataTable.TYPE_OPTIONAL},
            new PortType[]{BufferedDataTable.TYPE});
    }

    @Override protected DataTableSpec[] configure(final DataTableSpec[] input) throws InvalidSettingsException {
        validateCandidate(input[1]);
        if (input[0] != null) validatePair(input[0], input[1]);
        if (input[2] != null) validateCurrentPopulation(input[2]);
        return new DataTableSpec[]{input[1]};
    }

    @Override protected BufferedDataTable[] execute(final BufferedDataTable[] input, final ExecutionContext execution) throws Exception {
        final DataTableSpec spec = input[1].getDataTableSpec();
        validateCandidate(spec);
        if (input[0] != null) validatePair(input[0].getDataTableSpec(), spec);
        if (input[2] != null) validateCurrentPopulation(input[2].getDataTableSpec());
        final boolean pareto = "PARETO".equals(mode.getStringValue());
        final ProblemMetadata.Schema problem = pareto ? ProblemMetadata.require(spec) : null;
        final List<String> names = pareto ? problem.objectiveNames() : List.of();
        final int[] objectiveIndices = pareto ? KnimeTableSupport.requireNumericColumns(spec, names) : new int[0];
        final List<ObjectiveDefinition> definitions = pareto
            ? KnimeTableSupport.objectives(names,
                problem.objectives().stream().map(ProblemMetadata.Objective::direction).toList(), List.of())
            : List.of();
        final int violationIndex = spec.findColumnIndex(PopulationState.CONSTRAINT_VIOLATION);
        final Map<String, Integer> dynamicLimits = new HashMap<>();
        if (input[2] != null) {
            final DataTableSpec currentSpec = input[2].getDataTableSpec();
            for (DataRow row : input[2]) {
                final String key = PopulationState.run(row, currentSpec)
                    + ("RUN_AND_POPULATION".equals(grouping.getStringValue())
                        ? "\u0000" + PopulationState.population(row, currentSpec) : "");
                dynamicLimits.merge(key, 1, Integer::sum);
            }
        }
        final Map<String, LinkedHashMap<String, DataRow>> groups = new LinkedHashMap<>();
        for (int tableIndex = 0; tableIndex < 2; tableIndex++) {
            final BufferedDataTable table = input[tableIndex];
            if (table == null) continue;
            for (DataRow row : table) {
            final String key = PopulationState.run(row, spec)
                + ("RUN_AND_POPULATION".equals(grouping.getStringValue())
                    ? "\u0000" + PopulationState.population(row, spec) : "");
            final String id = PopulationState.individual(row, spec);
            final LinkedHashMap<String, DataRow> unique = groups.computeIfAbsent(key, ignored -> new LinkedHashMap<>());
            unique.remove(id); // replacement becomes the newest entry for FIFO mode
            unique.put(id, row);
            }
        }
        final BufferedDataContainer output = execution.createDataContainer(spec);
        int number = 0;
        for (Map.Entry<String, LinkedHashMap<String, DataRow>> group : groups.entrySet()) {
            final LinkedHashMap<String, DataRow> unique = group.getValue();
            final List<DataRow> rows = new ArrayList<>(unique.values());
            if (!pareto) {
                final int configured = maxSize.getIntValue();
                final int limit = dynamicLimits.getOrDefault(group.getKey(), configured);
                final int first = limit > 0 ? Math.max(0, rows.size() - limit) : 0;
                for (int index = first; index < rows.size(); index++)
                    output.addRowToTable(new DefaultRow("Archive" + number++, rows.get(index)));
                continue;
            }
            final List<Candidate> candidates = new ArrayList<>();
            for (DataRow row : rows)
                candidates.add(KnimeTableSupport.candidate(row, new int[0], objectiveIndices, violationIndex, List.of(), names));
            final List<Integer> front = new ArrayList<>(FastNonDominatedSort.sort(candidates, definitions).get(0));
            if (maxSize.getIntValue() > 0 && front.size() > maxSize.getIntValue()) {
                final double[] crowding = CrowdingDistance.compute(candidates, List.of(front), definitions);
                front.sort(Comparator.<Integer>comparingDouble(index -> crowding[index]).reversed()
                    .thenComparing(index -> candidates.get(index).id()));
                front.subList(maxSize.getIntValue(), front.size()).clear();
            }
            for (int index : front) output.addRowToTable(new DefaultRow("Archive" + number++, rows.get(index)));
        }
        output.close();
        return new BufferedDataTable[]{output.getTable()};
    }

    private static void validatePair(final DataTableSpec a, final DataTableSpec b) throws InvalidSettingsException {
        if (a.getNumColumns() != b.getNumColumns()) throw new InvalidSettingsException("Archive and candidate schemas must match.");
        for (int i = 0; i < a.getNumColumns(); i++)
            if (!a.getColumnSpec(i).getName().equals(b.getColumnSpec(i).getName()) || !a.getColumnSpec(i).getType().equals(b.getColumnSpec(i).getType()))
                throw new InvalidSettingsException("Archive and candidate schemas must match.");
    }
    private void validateCandidate(final DataTableSpec a) throws InvalidSettingsException {
        if (!List.of("PARETO", "FIFO_UNIQUE").contains(mode.getStringValue()))
            throw new InvalidSettingsException("Unsupported archive mode.");
        if ("PARETO".equals(mode.getStringValue())) {
            final ProblemMetadata.Schema problem = ProblemMetadata.require(a);
            KnimeTableSupport.requireNumericColumns(a, problem.objectiveNames());
        }
        if (!List.of("RUN", "RUN_AND_POPULATION").contains(grouping.getStringValue()))
            throw new InvalidSettingsException("Unsupported grouping.");
        PopulationState.requireVisibleColumns(a);
        if (a.findColumnIndex(PopulationState.CONSTRAINT_VIOLATION) >= 0)
            KnimeTableSupport.requireNumericColumns(a, List.of(PopulationState.CONSTRAINT_VIOLATION));
    }

    private SettingsModel[] models() { return new SettingsModel[]{mode, maxSize, grouping}; }
    private static void validateCurrentPopulation(final DataTableSpec spec) throws InvalidSettingsException {
        PopulationState.requireVisibleColumns(spec);
    }
    @Override protected void saveSettingsTo(final NodeSettingsWO settings) { for (SettingsModel model : models()) model.saveSettingsTo(settings); }
    @Override protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException { for (SettingsModel model : models()) model.validateSettings(settings); }
    @Override protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException { for (SettingsModel model : models()) model.loadSettingsFrom(settings); }
    @Override protected void loadInternals(final File directory, final ExecutionMonitor monitor) throws IOException, CanceledExecutionException { }
    @Override protected void saveInternals(final File directory, final ExecutionMonitor monitor) throws IOException, CanceledExecutionException { }
    @Override protected void reset() { }
}
