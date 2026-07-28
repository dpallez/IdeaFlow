package org.ideaflow.nodes.analysis.ecdf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ideaflow.nodes.analysis.OptimizationPlotData;
import org.ideaflow.nodes.analysis.OptimizationPlotNodeModel;
import java.util.TreeMap;

import org.ideaflow.knime.KnimeTableSupport;
import org.ideaflow.knime.OptimizationSummary;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/** Terminal visualization node for exact final-fitness empirical CDF curves. */
final class EcdfPlotNodeModel extends OptimizationPlotNodeModel {
    static final String CFG_SERIES = "series_column";
    static final String CFG_FITNESS = "fitness_column";
    static final String CFG_ECDF = "ecdf_column";
    static final String CFG_LOG_X = "log_x";

    private final SettingsModelString m_series =
        new SettingsModelString(CFG_SERIES, OptimizationSummary.SERIES);
    private final SettingsModelString m_fitness =
        new SettingsModelString(CFG_FITNESS, "Fitness");
    private final SettingsModelString m_ecdf = new SettingsModelString(CFG_ECDF, "ECDF");
    private final SettingsModelBoolean m_logX = new SettingsModelBoolean(CFG_LOG_X, false);

    EcdfPlotNodeModel() {
        super(OptimizationPlotData.empty("Final-fitness ECDF", "Fitness",
            "Fraction of runs", true));
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] input) throws InvalidSettingsException {
        validate(input[0]);
        return new DataTableSpec[0];
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] input,
            final ExecutionContext execution) throws Exception {
        final DataTableSpec spec = input[0].getDataTableSpec();
        validate(spec);
        final int seriesIndex = spec.findColumnIndex(m_series.getStringValue());
        final String fitnessColumn = m_fitness.getStringValue();
        final int xIndex = spec.findColumnIndex(fitnessColumn);
        final int yIndex = spec.findColumnIndex(m_ecdf.getStringValue());
        final Map<String, TreeMap<Double, OptimizationPlotData.Point>> grouped = new TreeMap<>();
        for (DataRow row : input[0]) {
            if (row.getCell(seriesIndex).isMissing()) continue;
            final double x = number(row.getCell(xIndex), row, fitnessColumn);
            final double y = number(row.getCell(yIndex), row, m_ecdf.getStringValue());
            if (Double.isFinite(x) && Double.isFinite(y)) {
                if (y < 0.0 || y > 1.0) {
                    throw new InvalidSettingsException("ECDF values must be between 0 and 1; found "
                        + y + " at " + row.getKey());
                }
                grouped.computeIfAbsent(row.getCell(seriesIndex).toString(), ignored -> new TreeMap<>())
                    .put(x, new OptimizationPlotData.Point(x, y, y, y));
            }
            execution.checkCanceled();
        }
        if (grouped.isEmpty()) {
            throw new InvalidSettingsException("The ECDF table contains no finite plot points.");
        }
        final List<OptimizationPlotData.Series> series = new ArrayList<>();
        for (Map.Entry<String, TreeMap<Double, OptimizationPlotData.Point>> entry : grouped.entrySet()) {
            series.add(new OptimizationPlotData.Series(entry.getKey(),
                List.copyOf(entry.getValue().values())));
        }
        setPlotData(new OptimizationPlotData("Final-fitness ECDF", fitnessColumn,
            "Fraction of runs", true, m_logX.getBooleanValue(), false, List.copyOf(series)));
        return new BufferedDataTable[0];
    }

    private void validate(final DataTableSpec spec) throws InvalidSettingsException {
        final int seriesIndex = spec.findColumnIndex(m_series.getStringValue());
        if (seriesIndex < 0 || !StringValue.class.isAssignableFrom(
                spec.getColumnSpec(seriesIndex).getType().getPreferredValueClass())) {
            throw new InvalidSettingsException("Missing or non-text series column: "
                + m_series.getStringValue());
        }
        KnimeTableSupport.requireNumericColumns(spec,
            List.of(m_fitness.getStringValue(), m_ecdf.getStringValue()));
    }


    private static double number(final DataCell cell, final DataRow row, final String column)
            throws InvalidSettingsException {
        if (cell.isMissing()) return Double.NaN;
        return KnimeTableSupport.number(cell, row, column);
    }

    @Override
    protected OptimizationPlotData emptyPlot() {
        return OptimizationPlotData.empty("Final-fitness ECDF", m_fitness.getStringValue(),
            "Fraction of runs", true);
    }

    private org.knime.core.node.defaultnodesettings.SettingsModel[] models() {
        return new org.knime.core.node.defaultnodesettings.SettingsModel[]{
            m_series, m_fitness, m_ecdf, m_logX};
    }

    @Override protected void saveSettingsTo(final NodeSettingsWO settings) {
        for (var model : models()) model.saveSettingsTo(settings);
    }
    @Override protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        for (var model : models()) model.validateSettings(settings);
    }
    @Override protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        for (var model : models()) model.loadSettingsFrom(settings);
    }
}
