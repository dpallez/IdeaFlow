package org.ideaflow.nodes.update;

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

import org.ideaflow.api.IdeaFlowState;
import org.ideaflow.api.ObjectiveDefinition;
import org.ideaflow.api.OptimizationDirection;
import org.ideaflow.api.Candidate;
import org.ideaflow.core.CrowdingDistance;
import org.ideaflow.core.DeCompetition;
import org.ideaflow.core.FastNonDominatedSort;
import org.ideaflow.core.LinearPopulationSchedule;
import org.ideaflow.core.Nsga3Selection;
import org.ideaflow.core.ParetoDominance;
import org.ideaflow.knime.KnimeTableSupport;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.ideaflow.knime.PopulationState;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
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

public final class ElitismNodeModel extends NodeModel {
    static final String CFG_MODE = "update_mode";
    static final String CFG_REFERENCE_DIVISIONS = "reference_divisions";
    static final String CFG_SIZE_POLICY = "population_size_policy";
    static final String CFG_MINIMUM_SIZE = "minimum_population_size";

    private final SettingsModelString m_mode = new SettingsModelString(CFG_MODE, "NSGA_II");
    private final SettingsModelIntegerBounded m_referenceDivisions =
        new SettingsModelIntegerBounded(CFG_REFERENCE_DIVISIONS, 12, 1, 1000);
    private final SettingsModelString m_sizePolicy =
        new SettingsModelString(CFG_SIZE_POLICY, "FIXED");
    private final SettingsModelIntegerBounded m_minimumSize =
        new SettingsModelIntegerBounded(CFG_MINIMUM_SIZE, 4, 4, Integer.MAX_VALUE);

    ElitismNodeModel() {
        super(2, 2);
    }

    @Override protected DataTableSpec[] configure(final DataTableSpec[] input)
            throws InvalidSettingsException {
        validate(input);
        final DataTableSpec merged = mergedSpec(input[0], input[1]);
        return new DataTableSpec[]{merged, merged};
    }

    @Override protected BufferedDataTable[] execute(final BufferedDataTable[] input,
            final ExecutionContext execution) throws Exception {
        final DataTableSpec parentSpec = input[0].getDataTableSpec();
        final DataTableSpec trialSpec = input[1].getDataTableSpec();
        validate(new DataTableSpec[]{parentSpec, trialSpec});
        final DataTableSpec spec = mergedSpec(parentSpec, trialSpec);
        final ProblemMetadata.Schema problem = ProblemMetadata.require(parentSpec);
        final List<String> objectiveNames = problem.objectiveNames();
        final int[] objectiveIndices = KnimeTableSupport.requireNumericColumns(spec, objectiveNames);
        final List<OptimizationDirection> directions = problem.objectives().stream()
            .map(ProblemMetadata.Objective::direction).toList();
        final List<ObjectiveDefinition> definitions =
            KnimeTableSupport.objectives(objectiveNames, directions, List.of());
        final int violationIndex = spec.findColumnIndex(PopulationState.CONSTRAINT_VIOLATION);
        final Map<String, List<DataRow>> parents = groups(input[0], spec);
        final Map<String, List<DataRow>> trials = groups(input[1], spec);
        final BufferedDataContainer survivors = execution.createDataContainer(spec);
        final BufferedDataContainer replaced = execution.createDataContainer(spec);
        int survivorNumber = 0;
        int replacedNumber = 0;

        for (Map.Entry<String, List<DataRow>> entry : parents.entrySet()) {
            final List<DataRow> parentRows = entry.getValue();
            final List<DataRow> trialRows = trials.get(entry.getKey());
            if (trialRows == null) {
                throw new InvalidSettingsException(
                    "Missing trial population for group " + entry.getKey());
            }
            final long groupNfe = maxNfe(parentRows, trialRows, spec);
            final long generation = nextGeneration(parentRows, spec);
            final ShadeSnapshot memory = ShadeSnapshot.from(trialRows, spec);
            final List<DataRow> considered = new ArrayList<>();
            considered.addAll(parentRows);
            considered.addAll(trialRows);
            final List<DataRow> pool = new ArrayList<>();
            final List<DataRow> archiveRows = new ArrayList<>();

            if ("GDE3".equals(m_mode.getStringValue())) {
                if (parentRows.size() != trialRows.size()) {
                    throw new InvalidSettingsException("GDE3 requires one trial per parent.");
                }
                for (int index = 0; index < parentRows.size(); index++) {
                    final Candidate parent = candidate(
                        parentRows.get(index), objectiveIndices, violationIndex, objectiveNames);
                    final Candidate trial = candidate(
                        trialRows.get(index), objectiveIndices, violationIndex, objectiveNames);
                    final int comparison = ParetoDominance.compare(parent, trial, definitions);
                    if (comparison <= 0) pool.add(parentRows.get(index));
                    if (comparison >= 0) pool.add(trialRows.get(index));
                }
            } else if ("DE_PAIRWISE".equals(m_mode.getStringValue())) {
                if (parentRows.size() != trialRows.size()) {
                    throw new InvalidSettingsException(
                        "Pairwise DE update requires one trial per parent.");
                }
                final OptimizationDirection direction = directions.get(0);
                for (int index = 0; index < parentRows.size(); index++) {
                    final DataRow parentRow = parentRows.get(index);
                    final DataRow trialRow = trialRows.get(index);
                    final Candidate parent = candidate(
                        parentRow, objectiveIndices, violationIndex, objectiveNames);
                    final Candidate trial = candidate(
                        trialRow, objectiveIndices, violationIndex, objectiveNames);
                    final DeCompetition.Outcome outcome = DeCompetition.compare(
                        parent.constraintViolation(), trial.constraintViolation(),
                        parent.objectives()[0], trial.objectives()[0], direction);
                    if (outcome.trialWins()) {
                        pool.add(withDeOutcome(trialRow, spec,
                            outcome.improvement() > 0.0, outcome.improvement()));
                        archiveRows.add(withDeOutcome(parentRow, spec, false, 0.0));
                    } else {
                        pool.add(withDeOutcome(parentRow, spec, false, 0.0));
                    }
                }
            } else {
                pool.addAll(considered);
            }

            final int target = targetSize(parentRows, spec, groupNfe, problem);
            final List<DataRow> selected =
                select(pool, target, objectiveIndices, violationIndex, objectiveNames, definitions);
            final Set<DataRow> selectedIdentity =
                Collections.newSetFromMap(new IdentityHashMap<>());
            selectedIdentity.addAll(selected);
            for (DataRow row : selected) {
                survivors.addRowToTable(withPopulationState("Survivor" + survivorNumber++,
                    row, spec, groupNfe, generation, memory));
                execution.checkCanceled();
            }

            final List<DataRow> discarded =
                "DE_PAIRWISE".equals(m_mode.getStringValue()) ? archiveRows : considered;
            for (DataRow row : discarded) {
                if ("DE_PAIRWISE".equals(m_mode.getStringValue())
                        || !selectedIdentity.contains(row)) {
                    replaced.addRowToTable(withPopulationState("Discarded" + replacedNumber++,
                        row, spec, groupNfe, generation, memory));
                }
            }
        }

        survivors.close();
        replaced.close();
        return new BufferedDataTable[]{survivors.getTable(), replaced.getTable()};
    }

    private List<DataRow> select(final List<DataRow> rows, final int target,
            final int[] objectiveIndices, final int violationIndex,
            final List<String> objectiveNames, final List<ObjectiveDefinition> definitions)
            throws InvalidSettingsException {
        final List<Candidate> candidates = new ArrayList<>();
        for (DataRow row : rows) {
            candidates.add(candidate(row, objectiveIndices, violationIndex, objectiveNames));
        }
        if ("SINGLE_OBJECTIVE".equals(m_mode.getStringValue())) {
            final List<Integer> order = new ArrayList<>();
            for (int index = 0; index < rows.size(); index++) order.add(index);
            order.sort((left, right) -> {
                final Candidate a = candidates.get(left);
                final Candidate b = candidates.get(right);
                final int constraint =
                    Double.compare(a.constraintViolation(), b.constraintViolation());
                if (a.constraintViolation() > 0 || b.constraintViolation() > 0) {
                    return constraint;
                }
                return Double.compare(
                    definitions.get(0).direction().normalize(a.objectives()[0]),
                    definitions.get(0).direction().normalize(b.objectives()[0]));
            });
            return order.stream().limit(target).map(rows::get).toList();
        }
        if ("NSGA_III".equals(m_mode.getStringValue())) {
            return Nsga3Selection.select(candidates, definitions, target,
                m_referenceDivisions.getIntValue()).stream().map(rows::get).toList();
        }
        final List<List<Integer>> fronts =
            FastNonDominatedSort.sort(candidates, definitions);
        final double[] crowding =
            CrowdingDistance.compute(candidates, fronts, definitions);
        final List<Integer> selected = new ArrayList<>();
        for (List<Integer> front : fronts) {
            if (selected.size() + front.size() <= target) {
                selected.addAll(front);
            } else {
                final List<Integer> ordered = new ArrayList<>(front);
                ordered.sort(Comparator.<Integer>comparingDouble(index -> crowding[index])
                    .reversed().thenComparing(index -> candidates.get(index).id()));
                selected.addAll(ordered.subList(0, target - selected.size()));
                break;
            }
        }
        return selected.stream().map(rows::get).toList();
    }

    private static Candidate candidate(final DataRow row, final int[] objectiveIndices,
            final int violationIndex, final List<String> objectiveNames)
            throws InvalidSettingsException {
        return KnimeTableSupport.candidate(
            row, new int[0], objectiveIndices, violationIndex, List.of(), objectiveNames);
    }

    private int targetSize(final List<DataRow> parents, final DataTableSpec spec,
            final long nfe, final ProblemMetadata.Schema problem)
            throws InvalidSettingsException {
        if ("FIXED".equals(m_sizePolicy.getStringValue())) return parents.size();
        int initial = parents.size();
        for (DataRow row : parents) {
            initial = PopulationState.get(row, spec).intValue(
                IdeaFlowState.INITIAL_POPULATION_SIZE, initial);
        }
        if (m_minimumSize.getIntValue() > initial) {
            throw new InvalidSettingsException(
                "Final population size cannot exceed the initial population size (" + initial + ").");
        }
        return Math.min(parents.size(), LinearPopulationSchedule.sizeAt(
            initial, m_minimumSize.getIntValue(), nfe, problem.maxEvaluations()));
    }

    private static long maxNfe(final List<DataRow> parents, final List<DataRow> trials,
            final DataTableSpec spec) {
        long maximum = 0L;
        for (DataRow row : parents) maximum = Math.max(maximum, PopulationState.nfe(row, spec));
        for (DataRow row : trials) maximum = Math.max(maximum, PopulationState.nfe(row, spec));
        return maximum;
    }

    private static long nextGeneration(final List<DataRow> rows, final DataTableSpec spec)
            throws InvalidSettingsException {
        long generation = 0L;
        for (DataRow row : rows) {
            generation = Math.max(generation,
                PopulationState.get(row, spec).longValue(IdeaFlowState.GENERATION, 0L));
        }
        return generation + 1L;
    }

    private static DataRow withDeOutcome(final DataRow row, final DataTableSpec spec,
            final boolean successful, final double improvement)
            throws InvalidSettingsException {
        final DataCell[] cells = KnimeTableSupport.copyToSpec(row, spec, spec);
        IdeaFlowState state = PopulationState.get(row, spec)
            .with(IdeaFlowState.DE_SUCCESS, successful);
        state = successful
            ? state.with(IdeaFlowState.DE_IMPROVEMENT, improvement)
            : state.without(IdeaFlowState.DE_IMPROVEMENT);
        PopulationState.set(cells, spec, state);
        return new DefaultRow(row.getKey(), cells);
    }

    private static DataRow withPopulationState(final String key, final DataRow row,
            final DataTableSpec spec, final long nfe, final long generation,
            final ShadeSnapshot memory) throws InvalidSettingsException {
        final DataCell[] cells = KnimeTableSupport.copyToSpec(row, spec, spec);
        IdeaFlowState state = PopulationState.get(row, spec)
            .with(IdeaFlowState.GENERATION, generation);
        if (memory != null) state = memory.apply(state);
        PopulationState.set(cells, spec, state);
        PopulationState.setNfe(cells, spec, nfe);
        return new DefaultRow(key, cells);
    }

    private record ShadeSnapshot(String f, String cr, int index) {
        static ShadeSnapshot from(final List<DataRow> rows, final DataTableSpec spec)
                throws InvalidSettingsException {
            if (rows.isEmpty()) return null;
            final IdeaFlowState state = PopulationState.get(rows.get(0), spec);
            final String f = state.text(IdeaFlowState.SHADE_MEMORY_F, "");
            final String cr = state.text(IdeaFlowState.SHADE_MEMORY_CR, "");
            if (f.isBlank() || cr.isBlank()) return null;
            return new ShadeSnapshot(f, cr,
                state.intValue(IdeaFlowState.SHADE_MEMORY_INDEX, 0));
        }

        IdeaFlowState apply(final IdeaFlowState state) {
            return state.with(IdeaFlowState.SHADE_MEMORY_F, f)
                .with(IdeaFlowState.SHADE_MEMORY_CR, cr)
                .with(IdeaFlowState.SHADE_MEMORY_INDEX, index);
        }
    }

    private static Map<String, List<DataRow>> groups(final BufferedDataTable table,
            final DataTableSpec outputSpec) throws InvalidSettingsException {
        final Map<String, List<DataRow>> result = new LinkedHashMap<>();
        for (DataRow row : table) {
            final DataRow normalized = new DefaultRow(row.getKey(),
                KnimeTableSupport.copyToSpec(row, table.getDataTableSpec(), outputSpec));
            result.computeIfAbsent(PopulationState.groupKey(normalized, outputSpec),
                ignored -> new ArrayList<>()).add(normalized);
        }
        return result;
    }

    private static DataTableSpec mergedSpec(final DataTableSpec parent,
            final DataTableSpec trial) throws InvalidSettingsException {
        final List<DataColumnSpec> columns = new ArrayList<>();
        for (DataColumnSpec column : parent) columns.add(column);
        for (DataColumnSpec column : trial) {
            final int existing = parent.findColumnIndex(column.getName());
            if (existing >= 0) {
                if (!parent.getColumnSpec(existing).getType().equals(column.getType())) {
                    throw new InvalidSettingsException(
                        "Parent and child column types differ: " + column.getName());
                }
            } else {
                columns.add(column);
            }
        }
        return new DataTableSpec(parent.getName(), columns.toArray(DataColumnSpec[]::new));
    }

    private void validate(final DataTableSpec[] specs) throws InvalidSettingsException {
        if (!List.of("FIXED", "LINEAR_NFE").contains(m_sizePolicy.getStringValue())) {
            throw new InvalidSettingsException(
                "Unsupported population size policy: " + m_sizePolicy.getStringValue());
        }
        for (String allowed : List.of(
                "SINGLE_OBJECTIVE", "DE_PAIRWISE", "NSGA_II", "NSGA_III", "GDE3")) {
            if (!allowed.equals(m_mode.getStringValue())) continue;
            final DataTableSpec parent = specs[0];
            final DataTableSpec trial = specs[1];
            final DataTableSpec merged = mergedSpec(parent, trial);
            final ProblemMetadata.Schema problem = ProblemMetadata.require(parent);
            final List<String> names = problem.objectiveNames();
            KnimeTableSupport.requireNumericColumns(parent, names);
            KnimeTableSupport.requireNumericColumns(trial, names);
            PopulationState.requireVisibleColumns(merged);
            if (("SINGLE_OBJECTIVE".equals(m_mode.getStringValue())
                    || "DE_PAIRWISE".equals(m_mode.getStringValue()))
                    && names.size() != 1) {
                throw new InvalidSettingsException(
                    "The selected update requires one objective.");
            }
            if ("NSGA_III".equals(m_mode.getStringValue()) && names.size() < 3) {
                throw new InvalidSettingsException(
                    "NSGA-III requires at least three objectives.");
            }
            if (merged.findColumnIndex(PopulationState.CONSTRAINT_VIOLATION) >= 0) {
                KnimeTableSupport.requireNumericColumns(
                    merged, List.of(PopulationState.CONSTRAINT_VIOLATION));
            }
            return;
        }
        throw new InvalidSettingsException(
            "Unsupported update mode: " + m_mode.getStringValue());
    }

    @Override protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_mode.saveSettingsTo(settings);
        m_referenceDivisions.saveSettingsTo(settings);
        m_sizePolicy.saveSettingsTo(settings);
        m_minimumSize.saveSettingsTo(settings);
    }

    @Override protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_mode.validateSettings(settings);
        m_referenceDivisions.validateSettings(settings);
        m_sizePolicy.validateSettings(settings);
        m_minimumSize.validateSettings(settings);
    }

    @Override protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_mode.loadSettingsFrom(settings);
        m_referenceDivisions.loadSettingsFrom(settings);
        m_sizePolicy.loadSettingsFrom(settings);
        m_minimumSize.loadSettingsFrom(settings);
    }

    @Override protected void loadInternals(final File directory,
            final ExecutionMonitor monitor)
            throws IOException, CanceledExecutionException { }

    @Override protected void saveInternals(final File directory,
            final ExecutionMonitor monitor)
            throws IOException, CanceledExecutionException { }

    @Override protected void reset() { }
}