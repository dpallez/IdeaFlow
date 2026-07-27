package org.ideaflow.nodes.analysis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

/** Lightweight responsive Swing renderer with exact ECDF steps and convergence quartile bands. */
final class OptimizationPlotPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color[] COLORS = {
        new Color(46, 111, 204), new Color(222, 95, 49), new Color(42, 157, 103),
        new Color(151, 90, 184), new Color(202, 153, 34), new Color(32, 151, 168),
        new Color(205, 74, 121), new Color(91, 99, 112)
    };
    private static final DecimalFormat NUMBER = new DecimalFormat("0.###E0");
    private volatile OptimizationPlotData m_data;

    OptimizationPlotPanel(final OptimizationPlotData data) {
        m_data = data;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(900, 560));
        setMinimumSize(new Dimension(500, 320));
    }

    void setData(final OptimizationPlotData data) {
        m_data = data;
        repaint();
    }

    static BufferedImage renderImage(final OptimizationPlotData data, final int width,
            final int height) {
        final OptimizationPlotPanel panel = new OptimizationPlotPanel(data);
        panel.setSize(width, height);
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = image.createGraphics();
        try {
            panel.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    @Override
    protected void paintComponent(final Graphics graphics) {
        super.paintComponent(graphics);
        final Graphics2D g = (Graphics2D)graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            draw(g, m_data);
        } finally {
            g.dispose();
        }
    }

    private void draw(final Graphics2D g, final OptimizationPlotData data) {
        final int width = getWidth();
        final int height = getHeight();
        final int left = 86;
        final int right = Math.max(left + 40, width - 34);
        final int top = 58;
        final int bottom = Math.max(top + 40, height - 70);

        g.setColor(new Color(32, 36, 43));
        g.setFont(getFont().deriveFont(Font.BOLD, 17f));
        g.drawString(data.title(), left, 30);
        if (data.series().isEmpty()) {
            g.setFont(getFont().deriveFont(13f));
            g.setColor(new Color(100, 106, 116));
            g.drawString("Execute the node to display the plot.", left, top + 35);
            return;
        }

        final Bounds bounds = bounds(data);
        drawGridAndAxes(g, data, bounds, left, right, top, bottom);
        for (int index = 0; index < data.series().size(); index++) {
            drawSeries(g, data, data.series().get(index), COLORS[index % COLORS.length],
                bounds, left, right, top, bottom);
        }
        drawLegend(g, data, left, right, top);
    }

    private static Bounds bounds(final OptimizationPlotData data) {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = data.ecdf() ? 0.0 : Double.POSITIVE_INFINITY;
        double maxY = data.ecdf() ? 1.0 : Double.NEGATIVE_INFINITY;
        for (OptimizationPlotData.Series series : data.series()) {
            for (OptimizationPlotData.Point point : series.points()) {
                final double x = transformed(point.x(), data.logX());
                final double y = transformed(point.y(), data.logY());
                if (Double.isFinite(x) && Double.isFinite(y)) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
                if (!data.ecdf()) {
                    final double lower = transformed(point.lower(), data.logY());
                    final double upper = transformed(point.upper(), data.logY());
                    if (Double.isFinite(lower)) minY = Math.min(minY, lower);
                    if (Double.isFinite(upper)) maxY = Math.max(maxY, upper);
                }
            }
        }
        if (!Double.isFinite(minX)) {
            minX = 0.0;
            maxX = 1.0;
        }
        if (!Double.isFinite(minY)) {
            minY = 0.0;
            maxY = 1.0;
        }
        if (maxX <= minX) maxX = minX + 1.0;
        if (maxY <= minY) maxY = minY + 1.0;
        if (!data.ecdf()) {
            final double padding = (maxY - minY) * 0.06;
            minY -= padding;
            maxY += padding;
        }
        return new Bounds(minX, maxX, minY, maxY);
    }

    private static void drawGridAndAxes(final Graphics2D g, final OptimizationPlotData data,
            final Bounds bounds, final int left, final int right, final int top, final int bottom) {
        g.setFont(g.getFont().deriveFont(11f));
        final FontMetrics metrics = g.getFontMetrics();
        for (int tick = 0; tick <= 5; tick++) {
            final double fraction = tick / 5.0;
            final int x = (int)Math.round(left + fraction * (right - left));
            final int y = (int)Math.round(bottom - fraction * (bottom - top));
            g.setColor(new Color(230, 233, 238));
            g.drawLine(x, top, x, bottom);
            g.drawLine(left, y, right, y);
            g.setColor(new Color(82, 88, 98));
            final String xText = label(bounds.minX + fraction * (bounds.maxX - bounds.minX), data.logX());
            g.drawString(xText, x - metrics.stringWidth(xText) / 2, bottom + 20);
            final String yText = label(bounds.minY + fraction * (bounds.maxY - bounds.minY), data.logY());
            g.drawString(yText, left - 10 - metrics.stringWidth(yText), y + 4);
        }
        g.setStroke(new BasicStroke(1.2f));
        g.setColor(new Color(83, 89, 99));
        g.drawLine(left, bottom, right, bottom);
        g.drawLine(left, top, left, bottom);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 12f));
        g.drawString(data.xLabel(), (left + right - g.getFontMetrics().stringWidth(data.xLabel())) / 2,
            bottom + 48);
        g.rotate(-Math.PI / 2);
        g.drawString(data.yLabel(), -(top + bottom + g.getFontMetrics().stringWidth(data.yLabel())) / 2,
            25);
        g.rotate(Math.PI / 2);
    }

    private static void drawSeries(final Graphics2D g, final OptimizationPlotData data,
            final OptimizationPlotData.Series series, final Color color, final Bounds bounds,
            final int left, final int right, final int top, final int bottom) {
        final List<ScreenPoint> points = new ArrayList<>();
        for (OptimizationPlotData.Point point : series.points()) {
            final double transformedX = transformed(point.x(), data.logX());
            final double transformedY = transformed(point.y(), data.logY());
            if (!Double.isFinite(transformedX) || !Double.isFinite(transformedY)) continue;
            points.add(new ScreenPoint(toX(transformedX, bounds, left, right),
                toY(transformedY, bounds, top, bottom), point));
        }
        if (points.isEmpty()) return;
        if (!data.ecdf()) drawBand(g, data, points, color, bounds, left, right, top, bottom);

        final Path2D line = new Path2D.Double();
        if (data.ecdf()) {
            line.moveTo(points.get(0).x, toY(0.0, bounds, top, bottom));
            line.lineTo(points.get(0).x, points.get(0).y);
        } else {
            line.moveTo(points.get(0).x, points.get(0).y);
        }
        for (int index = 1; index < points.size(); index++) {
            final ScreenPoint point = points.get(index);
            if (data.ecdf()) {
                line.lineTo(point.x, points.get(index - 1).y);
            }
            line.lineTo(point.x, point.y);
        }
        g.setColor(color);
        g.setStroke(new BasicStroke(2.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(line);
    }

    private static void drawBand(final Graphics2D g, final OptimizationPlotData data,
            final List<ScreenPoint> points, final Color color, final Bounds bounds,
            final int left, final int right, final int top, final int bottom) {
        final Path2D band = new Path2D.Double();
        boolean started = false;
        for (ScreenPoint point : points) {
            final double upper = transformed(point.source.upper(), data.logY());
            if (!Double.isFinite(upper)) continue;
            final int y = toY(upper, bounds, top, bottom);
            if (!started) {
                band.moveTo(point.x, y);
                started = true;
            } else {
                band.lineTo(point.x, y);
            }
        }
        if (!started) return;
        for (int index = points.size() - 1; index >= 0; index--) {
            final ScreenPoint point = points.get(index);
            final double lower = transformed(point.source.lower(), data.logY());
            if (Double.isFinite(lower)) band.lineTo(point.x, toY(lower, bounds, top, bottom));
        }
        band.closePath();
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 42));
        g.fill(band);
    }

    private static void drawLegend(final Graphics2D g, final OptimizationPlotData data,
            final int left, final int right, final int top) {
        g.setFont(g.getFont().deriveFont(12f));
        int x = left;
        int y = top - 18;
        for (int index = 0; index < data.series().size(); index++) {
            final String name = data.series().get(index).name();
            final int itemWidth = 28 + g.getFontMetrics().stringWidth(name) + 18;
            if (x + itemWidth > right && x > left) {
                x = left;
                y += 18;
            }
            g.setColor(COLORS[index % COLORS.length]);
            g.setStroke(new BasicStroke(3f));
            g.drawLine(x, y - 4, x + 17, y - 4);
            g.setColor(new Color(55, 60, 68));
            g.drawString(name, x + 23, y);
            x += itemWidth;
        }
    }

    private static double transformed(final double value, final boolean logarithmic) {
        return logarithmic ? value > 0.0 ? Math.log10(value) : Double.NaN : value;
    }

    private static String label(final double transformed, final boolean logarithmic) {
        final double value = logarithmic ? Math.pow(10.0, transformed) : transformed;
        final double absolute = Math.abs(value);
        if (absolute != 0.0 && (absolute >= 100000.0 || absolute < 0.001)) return NUMBER.format(value);
        return new DecimalFormat(absolute >= 100.0 ? "0" : absolute >= 1.0 ? "0.##" : "0.###").format(value);
    }

    private static int toX(final double value, final Bounds bounds, final int left, final int right) {
        return (int)Math.round(left + (value - bounds.minX) / (bounds.maxX - bounds.minX) * (right - left));
    }

    private static int toY(final double value, final Bounds bounds, final int top, final int bottom) {
        return (int)Math.round(bottom - (value - bounds.minY) / (bounds.maxY - bounds.minY) * (bottom - top));
    }

    private record Bounds(double minX, double maxX, double minY, double maxY) { }
    private record ScreenPoint(int x, int y, OptimizationPlotData.Point source) { }
}
