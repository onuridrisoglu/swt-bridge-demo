package com.example.warehouse.swt;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.example.warehouse.AppSession;
import com.example.warehouse.config.PersistenceUtil;
import com.example.warehouse.dao.UserDAO;
import com.example.warehouse.model.User;
import com.example.warehouse.seed.DataSeeder;
import com.example.warehouse.swt.ui.LoginView;
import com.example.warehouse.swt.ui.MainView;

/**
 * WarehousePro 3.5, the SWT edition. Opens the database, seeds it on first
 * run, shows the login window and, once signed in, the main window. Logging
 * out closes the main window and shows the login window again.
 * <p>
 * Window conventions (a web bridge relies on them): one {@link Display},
 * created here on the main thread; the login window is a fixed-size,
 * parentless {@code DIALOG_TRIM} shell; the main window is the only
 * resizable parentless shell; every dialog is a child shell of the main
 * window. Hidden option: {@code -Dwarehouse.autologin=<user>} signs in with
 * the seed credentials ({@code user / user}) and skips the login window.
 */
public final class WarehouseApp {

    private WarehouseApp() {
    }

    public static void main(String[] args) {
        Display.setAppName("WarehousePro");
        Display display = new Display();
        try {
            PersistenceUtil.init();
            DataSeeder.seedIfEmpty();
            autoLogin();

            while (true) {
                if (AppSession.getCurrentUser() == null) {
                    Shell login = new LoginView(display).open();
                    runEventLoop(display, login);
                    if (AppSession.getCurrentUser() == null) {
                        break; // login window closed without signing in
                    }
                }
                MainView main = new MainView(display);
                runEventLoop(display, main.open());
                if (!main.isLoggedOut()) {
                    break; // main window closed or File > Exit
                }
            }
        } finally {
            PersistenceUtil.shutdown();
            display.dispose();
        }
        // No System.exit: in the browser every user's application shares the
        // server's JVM.
    }

    /** The standard SWT event loop, until the given shell is closed. */
    private static void runEventLoop(Display display, Shell shell) {
        while (!shell.isDisposed()) {
            try {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            } catch (RuntimeException ex) {
                // Like an uncaught exception on the JavaFX application thread:
                // report it and keep the application running.
                ex.printStackTrace();
            }
        }
    }

    private static void autoLogin() {
        String username = System.getProperty("warehouse.autologin");
        if (username == null || username.isBlank()) {
            return;
        }
        User user = new UserDAO().authenticate(username.trim(), username.trim());
        if (user != null) {
            AppSession.login(user);
        } else {
            System.err.println("warehouse.autologin: no seed user '" + username
                    + "' with password '" + username + "'; showing the login window.");
        }
    }
}
