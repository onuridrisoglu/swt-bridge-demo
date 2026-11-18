package com.example.warehouse.swt.ui.dashboard;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import com.example.warehouse.swt.ui.util.Ui;

/** Drawing pieces shared by the dashboard charts. */
final class ChartSupport {

    static final int CHART_BORDER = 0xd2dae6;
    private static final int TEXT = 0x333333;
    private static final int SWATCH = 10;

    private ChartSupport() {
    }

    /** White background with the chart border. */
    static void frame(GC gc, Rectangle area) {
        gc.setBackground(Ui.color(Ui.WHITE));
        gc.fillRectangle(area);
        gc.setForeground(Ui.color(CHART_BORDER));
        gc.drawRectangle(0, 0, area.width - 1, area.height - 1);
    }

    /** Draws the centered title; returns the y below it. */
    static int title(GC gc, Rectangle area, String title) {
        gc.setFont(Ui.font(3, SWT.NORMAL));
        gc.setForeground(Ui.color(TEXT));
        Point extent = gc.textExtent(title);
        gc.drawText(title, (area.width - extent.x) / 2, 10, true);
        return 10 + extent.y + 8;
    }

    /** A centered row of color swatches with their names. */
    static void legend(GC gc, int width, int y, List<String> names, List<Integer> colors) {
        int total = 0;
        for (String name : names) {
            total += SWATCH + 4 + gc.textExtent(name).x + 16;
        }
        total -= names.isEmpty() ? 0 : 16;
        int x = Math.max(4, (width - total) / 2);
        int fontHeight = gc.getFontMetrics().getHeight();
        for (int i = 0; i < names.size(); i++) {
            gc.setBackground(Ui.color(colors.get(i)));
            gc.fillRectangle(x, y + (fontHeight - SWATCH) / 2, SWATCH, SWATCH);
            x += SWATCH + 4;
            gc.setForeground(Ui.color(TEXT));
            gc.drawText(names.get(i), x, y, true);
            x += gc.textExtent(names.get(i)).x + 16;
        }
    }

    /** The text, shortened with an ellipsis to fit {@code maxWidth} pixels. */
    static String fit(GC gc, String text, int maxWidth) {
        if (gc.textExtent(text).x <= maxWidth) {
            return text;
        }
        String shortened = text;
        while (shortened.length() > 1 && gc.textExtent(shortened + "…").x > maxWidth) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened + "…";
    }
}
