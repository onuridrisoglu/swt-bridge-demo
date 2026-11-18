package com.example.warehouse.swt.ui.dashboard;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import com.example.warehouse.swt.ui.util.Ui;

/**
 * A pie chart drawn with {@link GC}: title, slices clockwise from three
 * o'clock with outside labels and leader lines, and a legend, like the JavaFX
 * {@code PieChart} it replaces.
 */
final class PieChart extends Canvas {

    private static final int PAD = 10;
    private static final int TEXT = 0x333333;
    private static final int[] FALLBACK = { 0xf3622d, 0xfba71b, 0x57b757, 0x41a9c9,
            0x4258c9, 0x9a42c8, 0xc84164, 0x888888 };

    private final String title;
    private final Function<String, Integer> colorOf;
    private final List<String> names = new ArrayList<>();
    private final List<Double> values = new ArrayList<>();

    PieChart(Composite parent, String title, Function<String, Integer> colorOf) {
        super(parent, SWT.DOUBLE_BUFFERED);
        this.title = title;
        this.colorOf = colorOf;
        setBackground(Ui.color(Ui.WHITE));
        addPaintListener(e -> paint(e.gc));
        addListener(SWT.Resize, e -> redraw());
    }

    void setData(List<String> newNames, List<Double> newValues) {
        names.clear();
        names.addAll(newNames);
        values.clear();
        values.addAll(newValues);
        redraw();
    }

    private int sliceColor(int index) {
        Integer color = colorOf.apply(names.get(index));
        return color != null ? color : FALLBACK[index % FALLBACK.length];
    }

    private void paint(GC gc) {
        Rectangle area = getClientArea();
        gc.setAntialias(SWT.ON);
        gc.setTextAntialias(SWT.ON);
        ChartSupport.frame(gc, area);
        int top = ChartSupport.title(gc, area, title);

        gc.setFont(Ui.font(-1, SWT.NORMAL));
        int fontHeight = gc.getFontMetrics().getHeight();
        List<Integer> colors = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            colors.add(sliceColor(i));
        }
        int legendY = area.height - PAD - fontHeight;
        ChartSupport.legend(gc, area.width, legendY, names, colors);

        double total = values.stream().mapToDouble(Double::doubleValue).sum();
        int pieTop = top + 4;
        int pieBottom = legendY - 8;
        if (total <= 0) {
            gc.setForeground(Ui.color(Ui.HINT));
            Point extent = gc.textExtent("No data");
            gc.drawText("No data", (area.width - extent.x) / 2,
                    pieTop + (pieBottom - pieTop - extent.y) / 2, true);
            return;
        }

        int labelWidth = 0;
        for (String name : names) {
            labelWidth = Math.max(labelWidth, gc.textExtent(name).x);
        }
        int diameter = Math.min(area.width - 2 * (PAD + labelWidth + 24),
                pieBottom - pieTop - 2 * (fontHeight + 10));
        if (diameter < 20) {
            return;
        }
        int radius = diameter / 2;
        int cx = area.width / 2;
        int cy = pieTop + (pieBottom - pieTop) / 2;

        // Slices: SWT angles run counter-clockwise from 3 o'clock; go clockwise
        double cumulative = 0;
        for (int i = 0; i < values.size(); i++) {
            int start = (int) Math.round(-cumulative * 360 / total);
            cumulative += values.get(i);
            int end = (int) Math.round(-cumulative * 360 / total);
            gc.setBackground(Ui.color(colors.get(i)));
            gc.fillArc(cx - radius, cy - radius, diameter, diameter, start, end - start);
        }

        // Slice borders, then labels with leader lines
        gc.setForeground(Ui.color(Ui.WHITE));
        gc.setLineWidth(2);
        cumulative = 0;
        for (Double value : values) {
            double angle = Math.toRadians(-cumulative * 360 / total);
            gc.drawLine(cx, cy, cx + (int) Math.round(radius * Math.cos(angle)),
                    cy - (int) Math.round(radius * Math.sin(angle)));
            cumulative += value;
        }
        gc.setLineWidth(1);
        cumulative = 0;
        for (int i = 0; i < values.size(); i++) {
            double value = values.get(i);
            double mid = Math.toRadians(-(cumulative + value / 2) * 360 / total);
            cumulative += value;
            if (value <= 0) {
                continue;
            }
            double cos = Math.cos(mid);
            double sin = Math.sin(mid);
            int x1 = cx + (int) Math.round((radius - 2) * cos);
            int y1 = cy - (int) Math.round((radius - 2) * sin);
            int x2 = cx + (int) Math.round((radius + 12) * cos);
            int y2 = cy - (int) Math.round((radius + 12) * sin);
            gc.setForeground(Ui.color(0x8c8c8c));
            gc.drawLine(x1, y1, x2, y2);
            String name = names.get(i);
            Point extent = gc.textExtent(name);
            int tx = cos >= 0 ? x2 + 4 : x2 - 4 - extent.x;
            int ty = y2 - extent.y / 2;
            gc.setForeground(Ui.color(TEXT));
            gc.drawText(name, tx, ty, true);
        }
    }
}
