package org.ideaflow.nodes.surrogate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ideaflow.api.IdeaFlowState;
import org.ideaflow.api.IdeaFlowStateCell;
import org.ideaflow.api.OptimizationDirection;
import org.ideaflow.knime.KnimeTableSupport;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.ideaflow.knime.PopulationState;
import org.ideaflow.nodes.variation.RecombinationNodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
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
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/** Splits proposed candidates into exact and surrogate paths without adding helper columns. */
final class SurrogateCoordinatorNodeModel extends NodeModel {
    static final String CFG_ACQUISITION = "acquisition_column";
    static final String CFG_DIRECTION = "score_direction";
    static final String CFG_EXACT_COUNT = "exact_count";
    private final SettingsModelString acquisition =
        new SettingsModelString(CFG_ACQUISITION, "acquisition");
    private final SettingsModelString direction =
        new SettingsModelString(CFG_DIRECTION, "MAXIMIZE");
    private final SettingsModelIntegerBounded exactCount =
        new SettingsModelIntegerBounded(CFG_EXACT_COUNT, 10, 1, Integer.MAX_VALUE);

    SurrogateCoordinatorNodeModel() { super(1, 2); }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] input) throws InvalidSettingsException {
        validate(input[0]);
        return new DataTableSpec[]{input[0], input[0]};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] input,
            final ExecutionContext execution) throws Exception {
        final DataTableSpec spec = input[0].getDataTableSpec();
        validate(spec);
        final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
        final int score = spec.findColumnIndex(acquisition.getStringValue());
        final int stateIndex = spec.findColumnIndex(PopulationState.COLUMN);
        final List<String> resultNames = new ArrayList<>(problem.objectiveNames());
        problem.constraints().forEach(item -> resultNames.add(item.column()));
        final Map<String, List<DataRow>> groups = new LinkedHashMap<>();
        for (DataRow row : input[0]) {
            groups.computeIfAbsent(PopulationState.groupKey(row, spec), ignored -> new ArrayList<>()).add(row);
        }
        final BufferedDataContainer exact = execution.createDataContainer(spec);
        final BufferedDataContainer deferred = execution.createDataContainer(spec);
        int exactNumber = 0;
        int deferredNumber = 0;
        for (List<DataRow> rows : groups.values()) {
            rows.sort((a, b) -> {
                try {
                    final double left = KnimeTableSupport.number(
                        a.getCell(score), a, acquisition.getStringValue());
                    final double right = KnimeTableSupport.number(
                        b.getCell(score), b, acquisition.getStringValue());
                    final int comparison = Double.compare(left, right);
                    return OptimizationDirection.parse(direction.getStringValue())
                        == OptimizationDirection.MINIMIZE ? comparison : -comparison;
                } catch (InvalidSettingsException exception) {
                    throw new IllegalArgumentException(exception);
                }
            });
            for (int index = 0; index < rows.size(); index++) {
                final DataRow row = rows.get(index);
                final DataCell[] cells = KnimeTableSupport.copyToSpec(row, spec, spec);
                IdeaFlowState state = PopulationState.get(row, spec);
                if (index < Math.min(exactCount.getIntValue(), rows.size())) {
                    RecombinationNodeModel.invalidate(cells, spec, resultNames);
                    final int violation = spec.findColumnIndex(PopulationState.CONSTRAINT_VIOLATION);
                    if (violation >= 0) cells[violation] = DataType.getMissingCell();
                    state = ((IdeaFlowStateCell)cells[stateIndex]).state()
                        .with(IdeaFlowState.EVALUATION_SOURCE, "exact");
                    cells[stateIndex] = new IdeaFlowStateCell(state);
                    exact.addRowToTable(new DefaultRow("Exact" + exactNumber++, cells));
                } else {
                    state = state.with(IdeaFlowState.EVALUATION_SOURCE, "surrogate");
                    cells[stateIndex] = new IdeaFlowStateCell(state);
                    deferred.addRowToTable(new DefaultRow("Surrogate" + deferredNumber++, cells));
                }
            }
        }
        exact.close();
        deferred.close();
        return new BufferedDataTable[]{exact.getTable(), deferred.getTable()};
    }

    private void validate(final DataTableSpec spec) throws InvalidSettingsException {
        ProblemMetadata.require(spec);
        KnimeTableSupport.requireNumericColumns(spec, List.of(acquisition.getStringValue()));
        OptimizationDirection.parse(direction.getStringValue());
        PopulationState.requireVisibleColumns(spec);
    }

    private SettingsModel[] models() { return new SettingsModel[]{acquisition, direction, exactCount}; }
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
