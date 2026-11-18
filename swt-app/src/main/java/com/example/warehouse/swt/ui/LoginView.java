package com.example.warehouse.swt.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.example.warehouse.AppSession;
import com.example.warehouse.dao.UserDAO;
import com.example.warehouse.model.User;
import com.example.warehouse.swt.ui.util.Ui;

/**
 * The sign-in window: a fixed-size, parentless dialog-style shell centered
 * on the primary monitor. Signing in stores the user in {@link AppSession}
 * and closes the window; {@code WarehouseApp} then opens the main window.
 */
public final class LoginView {

    private static final int WIDTH = 420;
    private static final int BANNER_HEIGHT = 86;

    private final UserDAO userDAO = new UserDAO();
    private final Shell shell;
    private Text usernameField;
    private Text passwordField;
    private Label statusLabel;

    public LoginView(Display display) {
        shell = new Shell(display, SWT.DIALOG_TRIM);
        shell.setText("WarehousePro 3.5 — Login");
        shell.setBackground(Ui.color(Ui.WHITE));
        shell.setBackgroundMode(SWT.INHERIT_DEFAULT);
        GridLayout layout = Ui.grid(1, 0, 0);
        shell.setLayout(layout);

        buildBanner();
        buildForm();
        buildFooter();

        // Client area WIDTH wide, as tall as the content; the window is not resizable.
        shell.setSize(shell.computeSize(WIDTH, SWT.DEFAULT));
    }

    /** Centers and shows the window; the caller runs the event loop. */
    public Shell open() {
        Ui.centerOnPrimaryMonitor(shell);
        shell.open();
        usernameField.setFocus();
        usernameField.selectAll();
        return shell;
    }

    private void buildBanner() {
        Canvas banner = new Canvas(shell, SWT.DOUBLE_BUFFERED);
        GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
        data.heightHint = BANNER_HEIGHT;
        banner.setLayoutData(data);
        banner.addPaintListener(e -> {
            GC gc = e.gc;
            Rectangle area = banner.getClientArea();
            gc.setForeground(Ui.color(Ui.PRIMARY));
            gc.setBackground(Ui.color(Ui.PRIMARY_DARK));
            gc.fillGradientRectangle(0, 0, area.width, area.height, false);
            gc.setTextAntialias(SWT.ON);

            gc.setFont(Ui.font(10, SWT.BOLD));
            String name = "WarehousePro™";
            Point nameSize = gc.textExtent(name);
            gc.setFont(Ui.font(-1, SWT.NORMAL));
            String subtitle = "Inventory & Warehouse Management  v3.5";
            Point subSize = gc.textExtent(subtitle);

            int top = (area.height - nameSize.y - 4 - subSize.y) / 2;
            gc.setFont(Ui.font(10, SWT.BOLD));
            gc.setForeground(Ui.color(Ui.WHITE));
            gc.drawText(name, (area.width - nameSize.x) / 2, top, true);
            gc.setFont(Ui.font(-1, SWT.NORMAL));
            gc.setForeground(Ui.color(Ui.LIGHT_TEXT));
            gc.drawText(subtitle, (area.width - subSize.x) / 2, top + nameSize.y + 4, true);
        });
    }

    private void buildForm() {
        Composite outer = new Composite(shell, SWT.NONE);
        GridLayout outerLayout = new GridLayout(1, false);
        outerLayout.marginWidth = 40;
        outerLayout.marginTop = 20;
        outerLayout.marginBottom = 14;
        outer.setLayout(outerLayout);
        outer.setLayoutData(Ui.fill());

        Group section = Ui.section(outer, "Sign In");
        section.setLayoutData(Ui.fill());
        GridLayout form = Ui.grid(2, 10, 10);
        form.horizontalSpacing = 8;
        section.setLayout(form);

        Ui.fieldLabel(section, "Username:");
        usernameField = Ui.textField(section, 18);
        usernameField.setText("admin");
        Ui.fieldLabel(section, "Password:");
        passwordField = new Text(section, SWT.BORDER | SWT.SINGLE | SWT.PASSWORD);
        passwordField.setLayoutData(Ui.fillHorizontal());
        passwordField.setText("admin");

        statusLabel = new Label(section, SWT.CENTER);
        statusLabel.setForeground(Ui.color(Ui.DANGER));
        statusLabel.setText(" ");
        statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        Button loginButton = new Button(section, SWT.PUSH);
        loginButton.setText("Sign In");
        loginButton.setFont(Ui.bold());
        GridData buttonData = new GridData(SWT.CENTER, SWT.CENTER, true, false, 2, 1);
        buttonData.widthHint = 120;
        loginButton.setLayoutData(buttonData);
        loginButton.addListener(SWT.Selection, e -> doLogin());
        shell.setDefaultButton(loginButton);

        // Enter in the username field moves on to the password
        usernameField.addListener(SWT.Traverse, e -> {
            if (e.detail == SWT.TRAVERSE_RETURN) {
                e.doit = false;
                passwordField.setFocus();
                passwordField.selectAll();
            }
        });
    }

    private void buildFooter() {
        Composite footer = new Composite(shell, SWT.NONE);
        footer.setBackground(Ui.color(Ui.PANEL_BG));
        GridLayout layout = Ui.grid(1, 8, 0);
        footer.setLayout(layout);
        footer.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
        footer.addPaintListener(e -> {
            e.gc.setForeground(Ui.color(Ui.TOOLBAR_BORDER));
            e.gc.drawLine(0, 0, footer.getSize().x, 0);
        });
        Label hint = new Label(footer, SWT.CENTER);
        hint.setText("Default credentials:  admin / admin  |  manager / manager");
        hint.setFont(Ui.font(-2, SWT.ITALIC));
        hint.setForeground(Ui.color(Ui.HINT));
        hint.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
    }

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Please enter username and password.");
            return;
        }
        User user = userDAO.authenticate(username, password);
        if (user == null) {
            showStatus("Invalid username or password.");
            passwordField.setText("");
            passwordField.setFocus();
            return;
        }
        AppSession.login(user);
        shell.dispose();
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
        statusLabel.getParent().layout();
    }
}
