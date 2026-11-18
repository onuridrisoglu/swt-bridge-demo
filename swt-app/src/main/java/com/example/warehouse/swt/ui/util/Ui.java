package com.example.warehouse.swt.ui.util;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Shared building blocks of the WarehousePro UI: the SWT counterpart of the
 * JavaFX version's {@code Ui} helpers and {@code warehouse.css}. Colors and
 * fonts are created once per display, cached, and disposed with the display.
 */
public final class Ui {

    // ---- The "enterprise blue" palette of WarehousePro 3.5 ----
    public static final int PRIMARY = 0x1f497d;
    public static final int PRIMARY_DARK = 0x112f58;
    public static final int ACCENT = 0x4682b4;
    public static final int ROW_ALT = 0xf0f6fc;
    public static final int LOW_STOCK = 0xfff3cd;
    public static final int CRITICAL = 0xffdcdc;
    public static final int SUCCESS = 0x008000;
    public static final int WARNING = 0xb46400;
    public static final int DANGER = 0xb41e1e;
    public static final int TOOLBAR = 0xecf0f5;
    public static final int TOOLBAR_BORDER = 0xc8d2e1;
    public static final int BORDER = 0xbec8d7;
    public static final int STATUS_BAR = 0xdce4f0;
    public static final int STATUS_BAR_BORDER = 0xa0afc8;
    public static final int HINT = 0x78829a;
    public static final int LIGHT_TEXT = 0xbed7f0;
    public static final int PANEL_BG = 0xf5f7fa;
    public static final int WHITE = 0xffffff;

    public static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.US);

    private static final Map<Integer, Color> COLORS = new HashMap<>();
    private static final Map<String, Font> FONTS = new HashMap<>();
    private static boolean disposeHookInstalled;

    private Ui() {
    }

    // ---------------------------------------------------------------- resources

    /** A cached color for a 0xRRGGBB value; disposed with the display. */
    public static Color color(int rgb) {
        return COLORS.computeIfAbsent(rgb, key -> {
            Display display = display();
            return new Color(display, (key >> 16) & 0xff, (key >> 8) & 0xff, key & 0xff);
        });
    }

    /** The system font, {@code delta} points larger, with the given SWT style. */
    public static Font font(int delta, int style) {
        String key = delta + "/" + style;
        return FONTS.computeIfAbsent(key, k -> {
            Display display = display();
            FontData base = display.getSystemFont().getFontData()[0];
            return new Font(display, base.getName(), Math.max(6, base.getHeight() + delta), style);
        });
    }

    public static Font bold() {
        return font(0, SWT.BOLD);
    }

    private static Display display() {
        Display display = Display.getCurrent();
        if (display == null) {
            throw new IllegalStateException("Not on the SWT UI thread");
        }
        if (!disposeHookInstalled) {
            disposeHookInstalled = true;
            display.disposeExec(Ui::disposeResources);
        }
        return display;
    }

    private static void disposeResources() {
        COLORS.values().forEach(Color::dispose);
        FONTS.values().forEach(Font::dispose);
        COLORS.clear();
        FONTS.clear();
        disposeHookInstalled = false;
    }

    // ---------------------------------------------------------------- layouts

    public static GridLayout grid(int columns, int margin, int spacing) {
        GridLayout layout = new GridLayout(columns, false);
        layout.marginWidth = margin;
        layout.marginHeight = margin;
        layout.horizontalSpacing = spacing;
        layout.verticalSpacing = spacing;
        return layout;
    }

    public static GridData fill() {
        return new GridData(SWT.FILL, SWT.FILL, true, true);
    }

    public static GridData fillHorizontal() {
        return new GridData(SWT.FILL, SWT.CENTER, true, false);
    }

    // ---------------------------------------------------------------- widgets

    /** A bold form label, right-aligned in its column, that never shrinks below its text. */
    public static Label fieldLabel(Composite parent, String text) {
        Label label = new Label(parent, SWT.RIGHT);
        label.setText(text);
        label.setFont(bold());
        label.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        return label;
    }

    /** A bordered, titled section (the JavaFX version's {@code Ui.section}). */
    public static Group section(Composite parent, String title) {
        Group group = new Group(parent, SWT.NONE);
        group.setText(title);
        group.setFont(bold());
        group.setLayout(grid(1, 8, 6));
        return group;
    }

    /** A dialog heading such as "New Product". */
    public static Label panelTitle(Composite parent, String text) {
        Label title = new Label(parent, SWT.NONE);
        title.setText(text);
        title.setFont(font(3, SWT.BOLD));
        title.setForeground(color(PRIMARY));
        return title;
    }

    /** A single-line, bordered text field with a width hint in characters. */
    public static Text textField(Composite parent, int columns) {
        Text text = new Text(parent, SWT.BORDER | SWT.SINGLE);
        GridData data = fillHorizontal();
        data.widthHint = charWidth(text, columns);
        text.setLayoutData(data);
        return text;
    }

    /** A multi-line, wrapping text area with a height hint in rows. */
    public static Text textArea(Composite parent, int rows) {
        Text text = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData data = fill();
        data.heightHint = text.getLineHeight() * rows + 6;
        data.widthHint = 200;
        text.setLayoutData(data);
        // Tab moves the focus instead of inserting a tab character
        text.addListener(SWT.Traverse, e -> {
            if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                e.doit = true;
            }
        });
        return text;
    }

    public static int charWidth(Control control, int columns) {
        org.eclipse.swt.graphics.GC gc = new org.eclipse.swt.graphics.GC(control);
        try {
            return (int) Math.ceil(gc.getFontMetrics().getAverageCharacterWidth() * columns) + 8;
        } finally {
            gc.dispose();
        }
    }

    /**
     * A row of dialog buttons at the bottom right, in the platform's order
     * (OK first on Windows, last elsewhere). The OK button becomes the shell's
     * default button.
     */
    public static Button buttonBar(Composite parent, String okText, Runnable onOk) {
        Composite bar = new Composite(parent, SWT.NONE);
        bar.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
        GridLayout layout = new GridLayout(2, true);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        bar.setLayout(layout);
        boolean okFirst = "win32".equals(SWT.getPlatform());
        Button ok;
        Button cancel;
        if (okFirst) {
            ok = new Button(bar, SWT.PUSH);
            cancel = new Button(bar, SWT.PUSH);
        } else {
            cancel = new Button(bar, SWT.PUSH);
            ok = new Button(bar, SWT.PUSH);
        }
        ok.setText(okText);
        cancel.setText("Cancel");
        for (Button button : new Button[] { ok, cancel }) {
            GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
            data.widthHint = Math.max(90, button.computeSize(SWT.DEFAULT, SWT.DEFAULT).x);
            button.setLayoutData(data);
        }
        ok.addListener(SWT.Selection, e -> onOk.run());
        cancel.addListener(SWT.Selection, e -> parent.getShell().close());
        parent.getShell().setDefaultButton(ok);
        return ok;
    }

    // ---------------------------------------------------------------- shells

    /**
     * A modal child shell of {@code parent}: every dialog of the application
     * belongs to the main window (or to a dialog opened from it).
     */
    public static Shell dialogShell(Shell parent, String title, boolean resizable) {
        int style = SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | (resizable ? SWT.RESIZE : 0);
        Shell shell = new Shell(parent, style);
        shell.setText(title);
        return shell;
    }

    /**
     * Centers the dialog on its parent, opens it and runs a nested event loop
     * until it is closed. The optional control gets the initial focus.
     */
    public static void openModal(Shell dialog, Control... focus) {
        Shell parent = (Shell) dialog.getParent();
        if (parent != null) {
            centerOn(dialog, parent.getBounds(), parent.getMonitor());
        } else {
            centerOnPrimaryMonitor(dialog);
        }
        dialog.open();
        if (focus.length > 0 && !focus[0].isDisposed()) {
            focus[0].setFocus();
        }
        Display display = dialog.getDisplay();
        while (!dialog.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    public static void centerOnPrimaryMonitor(Shell shell) {
        Monitor primary = shell.getDisplay().getPrimaryMonitor();
        centerOn(shell, primary.getClientArea(), primary);
    }

    private static void centerOn(Shell shell, Rectangle area, Monitor monitor) {
        Point size = shell.getSize();
        Rectangle screen = monitor.getClientArea();
        int x = area.x + (area.width - size.x) / 2;
        int y = area.y + (area.height - size.y) / 2;
        x = Math.max(screen.x, Math.min(x, screen.x + screen.width - size.x));
        y = Math.max(screen.y, Math.min(y, screen.y + screen.height - size.y));
        shell.setLocation(x, y);
    }

    // ---------------------------------------------------------------- message boxes

    public static boolean confirm(Shell owner, String title, String message) {
        return message(owner, SWT.ICON_QUESTION | SWT.YES | SWT.NO, title, message) == SWT.YES;
    }

    public static void info(Shell owner, String title, String message) {
        message(owner, SWT.ICON_INFORMATION | SWT.OK, title, message);
    }

    public static void warn(Shell owner, String title, String message) {
        message(owner, SWT.ICON_WARNING | SWT.OK, title, message);
    }

    public static void error(Shell owner, String title, String message) {
        message(owner, SWT.ICON_ERROR | SWT.OK, title, message);
    }

    private static int message(Shell owner, int style, String title, String message) {
        MessageBox box = new MessageBox(owner, style | SWT.APPLICATION_MODAL);
        box.setText(title);
        box.setMessage(message);
        return box.open();
    }

    /**
     * Lets the user pick one of {@code choices} from a drop-down (the JavaFX
     * {@code ChoiceDialog}); returns {@code null} when cancelled.
     */
    public static <T> T choose(Shell owner, String title, String prompt, List<T> choices,
            Function<T, String> label) {
        Shell dialog = dialogShell(owner, title, false);
        dialog.setLayout(grid(1, 14, 10));
        Composite row = new Composite(dialog, SWT.NONE);
        row.setLayout(grid(2, 0, 8));
        row.setLayoutData(fillHorizontal());
        Label promptLabel = new Label(row, SWT.NONE);
        promptLabel.setText(prompt);
        ObjectCombo<T> combo = new ObjectCombo<>(row, label);
        combo.setItems(choices);
        combo.selectFirst();
        GridData comboData = fillHorizontal();
        comboData.widthHint = 260;
        combo.getControl().setLayoutData(comboData);

        Object[] result = new Object[1];
        buttonBar(dialog, "OK", () -> {
            result[0] = combo.getValue();
            dialog.close();
        });
        dialog.pack();
        openModal(dialog);
        @SuppressWarnings("unchecked")
        T chosen = (T) result[0];
        return chosen;
    }
}
