package com.example.warehouse.swt.ui.util;

import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * A panel toolbar: a light strip with buttons and separators on the left
 * and, after {@link #fill()}, controls pushed to the right edge. Call
 * {@link #done()} once all controls are added.
 */
public final class Toolbar {

    private final Composite bar;

    public Toolbar(Composite parent) {
        bar = new Composite(parent, SWT.NONE);
        bar.setBackground(Ui.color(Ui.TOOLBAR));
        bar.setBackgroundMode(SWT.INHERIT_DEFAULT);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 8;
        layout.marginHeight = 5;
        layout.horizontalSpacing = 4;
        bar.setLayout(layout);
        bar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        bar.addPaintListener(e -> {
            Point size = bar.getSize();
            e.gc.setForeground(Ui.color(Ui.TOOLBAR_BORDER));
            e.gc.drawLine(0, size.y - 1, size.x, size.y - 1);
        });
    }

    public Button button(String text, Runnable action) {
        Button button = new Button(bar, SWT.PUSH);
        button.setText(text);
        button.addListener(SWT.Selection, e -> action.run());
        return button;
    }

    public void separator() {
        Label separator = new Label(bar, SWT.SEPARATOR | SWT.VERTICAL);
        GridData data = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        data.heightHint = 20;
        separator.setLayoutData(data);
    }

    /** A fixed gap of {@code width} pixels. */
    public void gap(int width) {
        Label strut = new Label(bar, SWT.NONE);
        GridData data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        data.widthHint = width;
        strut.setLayoutData(data);
    }

    /** Pushes everything added after it to the right edge. */
    public void fill() {
        Label spacer = new Label(bar, SWT.NONE);
        spacer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    public Label label(String text) {
        Label label = new Label(bar, SWT.NONE);
        label.setText(text);
        return label;
    }

    /** A search field that reports every change of its text. */
    public Text searchField(Consumer<String> onChange) {
        Text field = new Text(bar, SWT.SEARCH | SWT.ICON_CANCEL | SWT.BORDER);
        GridData data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        data.widthHint = Ui.charWidth(field, 22);
        field.setLayoutData(data);
        field.addListener(SWT.Modify, e -> onChange.accept(field.getText()));
        return field;
    }

    public Composite done() {
        ((GridLayout) bar.getLayout()).numColumns = bar.getChildren().length;
        return bar;
    }
}
