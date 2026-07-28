package org.ideaflow.nodes.analysis;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeModel;

/** Shared storage and persistence for the two terminal optimization plot nodes. */
public abstract class OptimizationPlotNodeModel extends NodeModel {
  private static final String DATA_FILE = "plot-data.bin";
  private volatile OptimizationPlotData m_plot;

  protected OptimizationPlotNodeModel(final OptimizationPlotData initial) {
    super(1, 0);
    m_plot = initial;
  }

  public final OptimizationPlotData plotData() {
    return m_plot;
  }

  protected final void setPlotData(final OptimizationPlotData plot) {
    m_plot = plot;
  }

  protected abstract OptimizationPlotData emptyPlot();

  @Override
  protected final void reset() {
    m_plot = emptyPlot();
  }

  @Override
  protected final void saveInternals(final File directory, final ExecutionMonitor monitor)
      throws IOException, CanceledExecutionException {
    final OptimizationPlotData plot = m_plot;
    try (DataOutputStream output =
        new DataOutputStream(
            new BufferedOutputStream(new FileOutputStream(new File(directory, DATA_FILE))))) {
      output.writeUTF(plot.title());
      output.writeUTF(plot.xLabel());
      output.writeUTF(plot.yLabel());
      output.writeBoolean(plot.ecdf());
      output.writeBoolean(plot.logX());
      output.writeBoolean(plot.logY());
      output.writeInt(plot.series().size());
      for (OptimizationPlotData.Series series : plot.series()) {
        monitor.checkCanceled();
        output.writeUTF(series.name());
        output.writeInt(series.points().size());
        for (OptimizationPlotData.Point point : series.points()) {
          output.writeDouble(point.x());
          output.writeDouble(point.y());
          output.writeDouble(point.lower());
          output.writeDouble(point.upper());
        }
      }
    }
  }

  @Override
  protected final void loadInternals(final File directory, final ExecutionMonitor monitor)
      throws IOException, CanceledExecutionException {
    final File file = new File(directory, DATA_FILE);
    if (!file.isFile()) {
      m_plot = emptyPlot();
      return;
    }
    try (DataInputStream input =
        new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
      final String title = input.readUTF();
      final String xLabel = input.readUTF();
      final String yLabel = input.readUTF();
      final boolean ecdf = input.readBoolean();
      final boolean logX = input.readBoolean();
      final boolean logY = input.readBoolean();
      final int seriesCount = input.readInt();
      final List<OptimizationPlotData.Series> all = new ArrayList<>(seriesCount);
      for (int seriesIndex = 0; seriesIndex < seriesCount; seriesIndex++) {
        monitor.checkCanceled();
        final String name = input.readUTF();
        final int pointCount = input.readInt();
        final List<OptimizationPlotData.Point> points = new ArrayList<>(pointCount);
        for (int pointIndex = 0; pointIndex < pointCount; pointIndex++) {
          points.add(
              new OptimizationPlotData.Point(
                  input.readDouble(), input.readDouble(), input.readDouble(), input.readDouble()));
        }
        all.add(new OptimizationPlotData.Series(name, List.copyOf(points)));
      }
      m_plot = new OptimizationPlotData(title, xLabel, yLabel, ecdf, logX, logY, List.copyOf(all));
    } catch (EOFException exception) {
      throw new IOException("The saved optimization plot data is incomplete.", exception);
    }
  }
}
