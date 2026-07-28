package org.ideaflow.nodes.multiobjective;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ideaflow.api.Candidate;
import org.ideaflow.api.ObjectiveDefinition;
import org.ideaflow.api.OptimizationDirection;
import org.ideaflow.api.ReservedColumns;
import org.ideaflow.core.CrowdingDistance;
import org.ideaflow.core.FastNonDominatedSort;
import org.ideaflow.knime.KnimeTableSupport;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.ideaflow.knime.PopulationState;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/** Adds constraint-aware Pareto rank and crowding using Problem Setup metadata. */
public final class RankParetoSolutionsNodeModel extends NodeModel {
    RankParetoSolutionsNodeModel() { super(1, 1); }

    @Override protected DataTableSpec[] configure(final DataTableSpec[] input) throws InvalidSettingsException {
        validate(input[0]);
        return new DataTableSpec[]{outputSpec(input[0])};
    }

    private static DataTableSpec outputSpec(final DataTableSpec input) {
        return KnimeTableSupport.appendOrReplace(input,
            KnimeTableSupport.intColumn(ReservedColumns.PARETO_RANK),
            KnimeTableSupport.doubleColumn(ReservedColumns.CROWDING_DISTANCE));
    }

    @Override protected BufferedDataTable[] execute(final BufferedDataTable[] input,
            final ExecutionContext execution) throws Exception {
        final BufferedDataTable table = input[0];
        final DataTableSpec spec = table.getDataTableSpec();
        validate(spec);
        final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
        final List<String> objectiveNames = problem.objectiveNames();
        final int[] objectiveIndices = KnimeTableSupport.requireNumericColumns(spec, objectiveNames);
        final List<OptimizationDirection> directions = problem.objectives().stream()
            .map(ProblemMetadata.Objective::direction).toList();
        final List<ObjectiveDefinition> definitions =
            KnimeTableSupport.objectives(objectiveNames, directions, List.of());
        final int violation = spec.findColumnIndex(ReservedColumns.CONSTRAINT_VIOLATION);
        final List<DataRow> rows = new ArrayList<>();
        table.forEach(rows::add);
        final Map<String, List<Integer>> groups = new LinkedHashMap<>();
        for (int index = 0; index < rows.size(); index++) {
            final DataRow row = rows.get(index);
            groups.computeIfAbsent(PopulationState.groupKey(row, spec),
                ignored -> new ArrayList<>()).add(index);
        }
        final int[] ranks = new int[rows.size()];
        final double[] crowding = new double[rows.size()];
        for (List<Integer> indices : groups.values()) {
            final List<Candidate> candidates = new ArrayList<>();
            for (int index : indices) {
                candidates.add(KnimeTableSupport.candidate(rows.get(index), new int[0],
                    objectiveIndices, violation, List.of(), objectiveNames));
            }
            final List<List<Integer>> fronts = FastNonDominatedSort.sort(candidates, definitions);
            final double[] distances = CrowdingDistance.compute(candidates, fronts, definitions);
            for (int rank = 0; rank < fronts.size(); rank++) {
                for (int local : fronts.get(rank)) {
                    ranks[indices.get(local)] = rank;
                    crowding[indices.get(local)] = distances[local];
                }
            }
        }
        final DataTableSpec outputSpec = outputSpec(spec);
        final BufferedDataContainer output = execution.createDataContainer(outputSpec);
        for (int index = 0; index < rows.size(); index++) {
            final DataCell[] cells = KnimeTableSupport.copyToSpec(rows.get(index), spec, outputSpec);
            cells[outputSpec.findColumnIndex(ReservedColumns.PARETO_RANK)] = new IntCell(ranks[index]);
            cells[outputSpec.findColumnIndex(ReservedColumns.CROWDING_DISTANCE)] =
                new DoubleCell(crowding[index]);
            output.addRowToTable(new DefaultRow(rows.get(index).getKey(), cells));
            execution.checkCanceled();
        }
        output.close();
        return new BufferedDataTable[]{output.getTable()};
    }

    private static void validate(final DataTableSpec spec) throws InvalidSettingsException {
        final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
        KnimeTableSupport.requireNumericColumns(spec, problem.objectiveNames());
        PopulationState.requireVisibleColumns(spec);
        final int violation = spec.findColumnIndex(ReservedColumns.CONSTRAINT_VIOLATION);
        if (violation >= 0) {
            KnimeTableSupport.requireNumericColumns(spec, List.of(ReservedColumns.CONSTRAINT_VIOLATION));
        }
    }

    @Override protected void saveSettingsTo(final NodeSettingsWO settings) { }
    @Override protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException { }
    @Override protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException { }
    @Override protected void loadInternals(final File directory, final ExecutionMonitor monitor)
            throws IOException, CanceledExecutionException { }
    @Override protected void saveInternals(final File directory, final ExecutionMonitor monitor)
            throws IOException, CanceledExecutionException { }
    @Override protected void reset() { }
}
