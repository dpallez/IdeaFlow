package org.ideaflow.nodes.analysis.convergence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.ideaflow.knime.KnimeTableSupport;
import org.ideaflow.knime.OptimizationSummary;
import org.ideaflow.knime.PopulationState;
import org.ideaflow.nodes.analysis.OptimizationPlotData;
import org.ideaflow.nodes.analysis.OptimizationPlotNodeModel;
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

/** Terminal visualization node for median optimization convergence and its interquartile band. */
final class ConvergencePlotNodeModel extends OptimizationPlotNodeModel {
  static final String CFG_SERIES = "series_column";
  static final String CFG_NFE = "nfe_column";
  static final String CFG_VALUE = "value_column";
  static final String CFG_LOWER = "lower_column";
  static final String CFG_UPPER = "upper_column";
  static final String CFG_LOG_X = "log_x";
  static final String CFG_LOG_Y = "log_y";

  private final SettingsModelString m_series =
      new SettingsModelString(CFG_SERIES, OptimizationSummary.SERIES);
  private final SettingsModelString m_nfe = new SettingsModelString(CFG_NFE, PopulationState.NFE);
  private final SettingsModelString m_value = new SettingsModelString(CFG_VALUE, "Median");
  private final SettingsModelString m_lower = new SettingsModelString(CFG_LOWER, "Q1");
  private final SettingsModelString m_upper = new SettingsModelString(CFG_UPPER, "Q3");
  private final SettingsModelBoolean m_logX = new SettingsModelBoolean(CFG_LOG_X, false);
  private final SettingsModelBoolean m_logY = new SettingsModelBoolean(CFG_LOG_Y, false);

  ConvergencePlotNodeModel() {
    super(OptimizationPlotData.empty("Convergence plot", "NFE", "Median", false));
  }

  @Override
  protected DataTableSpec[] configure(final DataTableSpec[] input) throws InvalidSettingsException {
    validate(input[0]);
    return new DataTableSpec[0];
  }

  @Override
  protected BufferedDataTable[] execute(
      final BufferedDataTable[] input, final ExecutionContext execution) throws Exception {
    final DataTableSpec spec = input[0].getDataTableSpec();
    validate(spec);
    final int seriesIndex = spec.findColumnIndex(m_series.getStringValue());
    final int xIndex = spec.findColumnIndex(m_nfe.getStringValue());
    final int yIndex = spec.findColumnIndex(m_value.getStringValue());
    final int lowerIndex = optionalNumericIndex(spec, m_lower.getStringValue());
    final int upperIndex = optionalNumericIndex(spec, m_upper.getStringValue());
    final Map<String, TreeMap<Double, OptimizationPlotData.Point>> grouped = new TreeMap<>();
    for (DataRow row : input[0]) {
      if (row.getCell(seriesIndex).isMissing()) continue;
      final double x = number(row.getCell(xIndex), row, m_nfe.getStringValue());
      final double y = number(row.getCell(yIndex), row, m_value.getStringValue());
      final double lower =
          lowerIndex < 0 ? y : number(row.getCell(lowerIndex), row, m_lower.getStringValue());
      final double upper =
          upperIndex < 0 ? y : number(row.getCell(upperIndex), row, m_upper.getStringValue());
      if (Double.isFinite(x) && Double.isFinite(y)) {
        grouped
            .computeIfAbsent(row.getCell(seriesIndex).toString(), ignored -> new TreeMap<>())
            .put(x, new OptimizationPlotData.Point(x, y, lower, upper));
      }
      execution.checkCanceled();
    }
    if (grouped.isEmpty()) {
      throw new InvalidSettingsException("The convergence table contains no finite plot points.");
    }
    final List<OptimizationPlotData.Series> series = new ArrayList<>();
    for (Map.Entry<String, TreeMap<Double, OptimizationPlotData.Point>> entry :
        grouped.entrySet()) {
      series.add(
          new OptimizationPlotData.Series(entry.getKey(), List.copyOf(entry.getValue().values())));
    }
    setPlotData(
        new OptimizationPlotData(
            "Convergence plot",
            m_nfe.getStringValue(),
            m_value.getStringValue(),
            false,
            m_logX.getBooleanValue(),
            m_logY.getBooleanValue(),
            List.copyOf(series)));
    return new BufferedDataTable[0];
  }

  private void validate(final DataTableSpec spec) throws InvalidSettingsException {
    final int seriesIndex = spec.findColumnIndex(m_series.getStringValue());
    if (seriesIndex < 0
        || !StringValue.class.isAssignableFrom(
            spec.getColumnSpec(seriesIndex).getType().getPreferredValueClass())) {
      throw new InvalidSettingsException(
          "Missing or non-text series column: " + m_series.getStringValue());
    }
    KnimeTableSupport.requireNumericColumns(
        spec, List.of(m_nfe.getStringValue(), m_value.getStringValue()));
    optionalNumericIndex(spec, m_lower.getStringValue());
    optionalNumericIndex(spec, m_upper.getStringValue());
  }

  private static int optionalNumericIndex(final DataTableSpec spec, final String name)
      throws InvalidSettingsException {
    if (name == null || name.isBlank()) return -1;
    KnimeTableSupport.requireNumericColumns(spec, List.of(name));
    return spec.findColumnIndex(name);
  }

  private static double number(final DataCell cell, final DataRow row, final String column)
      throws InvalidSettingsException {
    if (cell.isMissing()) return Double.NaN;
    return KnimeTableSupport.number(cell, row, column);
  }

  @Override
  protected OptimizationPlotData emptyPlot() {
    return OptimizationPlotData.empty(
        "Convergence plot", m_nfe.getStringValue(), m_value.getStringValue(), false);
  }

  private org.knime.core.node.defaultnodesettings.SettingsModel[] models() {
    return new org.knime.core.node.defaultnodesettings.SettingsModel[] {
      m_series, m_nfe, m_value, m_lower, m_upper, m_logX, m_logY
    };
  }

  @Override
  protected void saveSettingsTo(final NodeSettingsWO settings) {
    for (var model : models()) model.saveSettingsTo(settings);
  }

  @Override
  protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
    for (var model : models()) model.validateSettings(settings);
  }

  @Override
  protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
      throws InvalidSettingsException {
    for (var model : models()) model.loadSettingsFrom(settings);
  }
}
