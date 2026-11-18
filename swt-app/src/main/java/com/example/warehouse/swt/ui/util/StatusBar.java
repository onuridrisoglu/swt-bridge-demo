package com.example.warehouse.swt.ui.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Status line at the bottom of the main window: a message on the left, the
 * user and a clock on the right.
 */
public class StatusBar extends Composite {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm");
    private static final int CLOCK_MILLIS = 30_000;

    private final Label messageLabel;
    private final Label userLabel;
    private final Label timeLabel;
    private final Runnable clockTick = this::tick;

    public StatusBar(Composite parent, String username) {
        super(parent, SWT.NONE);
        setBackground(Ui.color(Ui.STATUS_BAR));
        setBackgroundMode(SWT.INHERIT_DEFAULT);
        GridLayout layout = new GridLayout(4, false);
        layout.marginWidth = 8;
        layout.marginHeight = 3;
        layout.horizontalSpacing = 12;
        setLayout(layout);
        addPaintListener(e -> {
            Point size = getSize();
            e.gc.setForeground(Ui.color(Ui.STATUS_BAR_BORDER));
            e.gc.drawLine(0, 0, size.x, 0);
        });

        messageLabel = new Label(this, SWT.NONE);
        messageLabel.setText("Ready");
        messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        userLabel = new Label(this, SWT.NONE);
        Label separator = new Label(this, SWT.SEPARATOR | SWT.VERTICAL);
        GridData separatorData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        separatorData.heightHint = 14;
        separator.setLayoutData(separatorData);
        timeLabel = new Label(this, SWT.NONE);

        setUser(username);
        updateTime();
        getDisplay().timerExec(CLOCK_MILLIS, clockTick);
        addListener(SWT.Dispose, e -> getDisplay().timerExec(-1, clockTick));
    }

    private void tick() {
        if (isDisposed()) {
            return;
        }
        updateTime();
        getDisplay().timerExec(CLOCK_MILLIS, clockTick);
    }

    private void updateTime() {
        timeLabel.setText(LocalDateTime.now().format(FORMAT));
        layout();
    }

    public void setUser(String username) {
        userLabel.setText("User: " + username);
        layout();
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    public void setRowCount(int count, String entity) {
        setMessage(count + " " + entity + " record" + (count != 1 ? "s" : "") + " loaded");
    }
}
