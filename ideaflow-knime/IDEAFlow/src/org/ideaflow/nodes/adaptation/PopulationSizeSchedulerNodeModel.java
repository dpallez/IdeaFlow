package org.ideaflow.nodes.adaptation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ideaflow.api.IdeaFlowState;
import org.ideaflow.core.LinearPopulationSchedule;
import org.ideaflow.knime.KnimeTableSupport;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.ideaflow.knime.PopulationState;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
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
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/** Reduces a population according to NFE without exposing scheduler bookkeeping columns. */
final class PopulationSizeSchedulerNodeModel extends NodeModel {
    static final String CFG_MINIMUM = "minimum_size";
    private final SettingsModelIntegerBounded minimum =
        new SettingsModelIntegerBounded(CFG_MINIMUM, 4, 4, Integer.MAX_VALUE);

    PopulationSizeSchedulerNodeModel() { super(1, 1); }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] input) throws InvalidSettingsException {
        validate(input[0]);
        return new DataTableSpec[]{input[0]};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] input,
            final ExecutionContext execution) throws Exception {
        final DataTableSpec spec = input[0].getDataTableSpec();
        validate(spec);
        final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
        final ProblemMetadata.Objective objective = problem.objectives().get(0);
        final int rank = spec.findColumnIndex("Pareto rank");
        final int crowding = spec.findColumnIndex("Crowding distance");
        final int objectiveIndex = spec.findColumnIndex(objective.column());
        final Map<String, List<DataRow>> groups = new LinkedHashMap<>();
        for (DataRow row : input[0]) {
            groups.computeIfAbsent(PopulationState.groupKey(row, spec), ignored -> new ArrayList<>()).add(row);
        }
        final BufferedDataContainer output = execution.createDataContainer(spec);
        int number = 0;
        for (List<DataRow> rows : groups.values()) {
            long nfe = 0;
            int initial = rows.size();
            for (DataRow row : rows) {
                nfe = Math.max(nfe, PopulationState.nfe(row, spec));
                initial = PopulationState.get(row, spec)
                    .intValue(IdeaFlowState.INITIAL_POPULATION_SIZE, initial);
            }
            final int target = Math.min(rows.size(), LinearPopulationSchedule.sizeAt(
                initial, minimum.getIntValue(), nfe, problem.maxEvaluations()));
            rows.sort((a, b) -> {
                if (rank >= 0 && crowding >= 0 && !a.getCell(rank).isMissing() && !b.getCell(rank).isMissing()) {
                    final int rankComparison = Integer.compare(
                        ((IntValue)a.getCell(rank)).getIntValue(),
                        ((IntValue)b.getCell(rank)).getIntValue());
                    if (rankComparison != 0) return rankComparison;
                    return -Double.compare(value(a, crowding), value(b, crowding));
                }
                return Double.compare(objective.direction().normalize(value(a, objectiveIndex)),
                    objective.direction().normalize(value(b, objectiveIndex)));
            });
            for (int i = 0; i < target; i++) {
                output.addRowToTable(new DefaultRow("Scheduled" + number++, rows.get(i)));
            }
        }
        output.close();
        return new BufferedDataTable[]{output.getTable()};
    }

    private static double value(final DataRow row, final int index) {
        final DataCell cell = row.getCell(index);
        if (cell instanceof DoubleValue value) return value.getDoubleValue();
        if (cell instanceof IntValue value) return value.getIntValue();
        if (cell instanceof LongValue value) return value.getLongValue();
        return Double.NaN;
    }

    private void validate(final DataTableSpec spec) throws InvalidSettingsException {
        final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
        KnimeTableSupport.requireNumericColumns(spec, List.of(problem.objectives().get(0).column()));
        PopulationState.requireVisibleColumns(spec);
    }

    private SettingsModel[] models() { return new SettingsModel[]{minimum}; }
    @Override protected void saveSettingsTo(final NodeSettingsWO settings) {
        for (SettingsModel model : models()) model.saveSettingsTo(settings);
    }
    @Override protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        for (SettingsModel model : models()) model.validateSettings(settings);
    }
    @Override protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        for (SettingsModel model : models()) model.loadSettingsFrom(settings);
    }
    @Override protected void loadInternals(final File directory, final ExecutionMonitor monitor)
            throws IOException, CanceledExecutionException { }
    @Override protected void saveInternals(final File directory, final ExecutionMonitor monitor)
            throws IOException, CanceledExecutionException { }
    @Override protected void reset() { }
}
