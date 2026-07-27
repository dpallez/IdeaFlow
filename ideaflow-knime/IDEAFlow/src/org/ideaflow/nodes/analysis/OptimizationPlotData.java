package org.ideaflow.nodes.analysis;

import java.util.List;

/** Immutable data passed from an executed plot node model to its view. */
record OptimizationPlotData(String title, String xLabel, String yLabel, boolean ecdf,
        boolean logX, boolean logY, List<Series> series) {
    static OptimizationPlotData empty(final String title, final String xLabel,
            final String yLabel, final boolean ecdf) {
        return new OptimizationPlotData(title, xLabel, yLabel, ecdf, false, false, List.of());
    }

    record Series(String name, List<Point> points) { }
    record Point(double x, double y, double lower, double upper) { }
}
