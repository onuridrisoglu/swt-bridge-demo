package com.example.warehouse.swt.ui.dashboard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import com.example.warehouse.swt.ui.util.Ui;

/**
 * A single-series vertical bar chart drawn with {@link GC}: title, value
 * axis with grid lines, category axis, axis labels and a legend, like the
 * JavaFX {@code BarChart} it replaces.
 */
final class BarChart extends Canvas {

    private static final int PAD = 10;
    private static final int GRID = 0xe6e6e6;
    private static final int AXIS = 0x8c8c8c;
    private static final int TEXT = 0x333333;

    private final String title;
    private final String xLabel;
    private final String yLabel;
    private final String seriesName;
    private final int barColor;
    private final List<String> categories = new ArrayList<>();
    private final List<Double> values = new ArrayList<>();

    BarChart(Composite parent, String title, String xLabel, String yLabel, String seriesName,
            int barColor) {
        super(parent, SWT.DOUBLE_BUFFERED);
        this.title = title;
        this.xLabel = xLabel;
        this.yLabel = yLabel;
        this.seriesName = seriesName;
        this.barColor = barColor;
        setBackground(Ui.color(Ui.WHITE));
        addPaintListener(e -> paint(e.gc));
        addListener(SWT.Resize, e -> redraw());
    }

    void setData(List<String> newCategories, List<Double> newValues) {
        categories.clear();
        categories.addAll(newCategories);
        values.clear();
        values.addAll(newValues);
        redraw();
    }

    private void paint(GC gc) {
        Rectangle area = getClientArea();
        gc.setAntialias(SWT.ON);
        gc.setTextAntialias(SWT.ON);
        ChartSupport.frame(gc, area);
        int top = ChartSupport.title(gc, area, title);

        // Legend at the bottom, the axis label and category labels above it
        gc.setFont(Ui.font(-1, SWT.NORMAL));
        int fontHeight = gc.getFontMetrics().getHeight();
        int legendY = area.height - PAD - fontHeight;
        ChartSupport.legend(gc, area.width, legendY, List.of(seriesName), List.of(barColor));
        int xLabelY = legendY - 6 - fontHeight;
        int categoryY = xLabelY - 4 - fontHeight;
        int plotBottom = categoryY - 6;

        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double step = niceStep(max <= 0 ? 1 : max / 5);
        double axisMax = Math.max(step, Math.ceil(max / step) * step);

        int tickWidth = 0;
        for (double v = 0; v <= axisMax + step / 2; v += step) {
            tickWidth = Math.max(tickWidth, gc.textExtent(formatTick(v)).x);
        }
        int plotLeft = PAD + fontHeight + 8 + tickWidth + 6;
        int plotRight = area.width - PAD - 6;
        int plotTop = top + 4;
        int plotHeight = plotBottom - plotTop;
        int plotWidth = plotRight - plotLeft;
        if (plotHeight < 20 || plotWidth < 40) {
            return;
        }

        // Value axis: grid lines and tick labels
        gc.setLineWidth(1);
        for (double v = 0; v <= axisMax + step / 2; v += step) {
            int y = plotBottom - (int) Math.round(v / axisMax * plotHeight);
            gc.setForeground(Ui.color(GRID));
            gc.drawLine(plotLeft, y, plotRight, y);
            gc.setForeground(Ui.color(TEXT));
            String tick = formatTick(v);
            Point extent = gc.textExtent(tick);
            gc.drawText(tick, plotLeft - 6 - extent.x, y - extent.y / 2, true);
        }
        gc.setForeground(Ui.color(AXIS));
        gc.drawLine(plotLeft, plotTop, plotLeft, plotBottom);
        gc.drawLine(plotLeft, plotBottom, plotRight, plotBottom);

        // Y axis label, rotated
        Point yExtent = gc.textExtent(yLabel);
        Transform transform = new Transform(gc.getDevice());
        try {
            transform.translate(PAD, plotTop + (plotHeight + yExtent.x) / 2f);
            transform.rotate(-90);
            gc.setTransform(transform);
            gc.setForeground(Ui.color(TEXT));
            gc.drawText(yLabel, 0, 0, true);
        } finally {
            gc.setTransform(null);
            transform.dispose();
        }

        // X axis label
        Point xExtent = gc.textExtent(xLabel);
        gc.drawText(xLabel, plotLeft + (plotWidth - xExtent.x) / 2, xLabelY, true);

        // Bars and category labels
        int n = categories.size();
        if (n == 0) {
            return;
        }
        double slot = (double) plotWidth / n;
        int barWidth = (int) Math.max(4, Math.min(slot - 20, slot * 0.8));
        gc.setBackground(Ui.color(barColor));
        for (int i = 0; i < n; i++) {
            int barHeight = (int) Math.round(values.get(i) / axisMax * plotHeight);
            int x = plotLeft + (int) Math.round(slot * i + (slot - barWidth) / 2);
            gc.fillRectangle(x, plotBottom - barHeight, barWidth, barHeight);

            String label = ChartSupport.fit(gc, categories.get(i), (int) slot - 4);
            Point extent = gc.textExtent(label);
            gc.setForeground(Ui.color(TEXT));
            gc.drawText(label, plotLeft + (int) Math.round(slot * i + (slot - extent.x) / 2),
                    categoryY, true);
        }
    }

    private static double niceStep(double raw) {
        double magnitude = Math.pow(10, Math.floor(Math.log10(raw)));
        double residual = raw / magnitude;
        double nice = residual > 5 ? 10 : residual > 2 ? 5 : residual > 1 ? 2 : 1;
        return nice * magnitude;
    }

    private static String formatTick(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value)
                : String.format("%.1f", value);
    }
}
