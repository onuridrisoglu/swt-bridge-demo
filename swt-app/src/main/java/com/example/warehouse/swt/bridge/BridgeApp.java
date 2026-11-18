package com.example.warehouse.swt.bridge;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.example.warehouse.AppSession;
import com.example.warehouse.config.PersistenceUtil;
import com.example.warehouse.dao.UserDAO;
import com.example.warehouse.interop.SupplierEditRequestListener;
import com.example.warehouse.interop.SupplierSelectionListener;
import com.example.warehouse.model.User;
import com.example.warehouse.seed.DataSeeder;
import com.example.warehouse.swt.ui.dashboard.DashboardPanel;
import com.example.warehouse.swt.ui.orders.PurchaseOrdersPanel;
import com.example.warehouse.swt.ui.products.ProductsPanel;
import com.example.warehouse.swt.ui.stock.StockMovementsPanel;
import com.example.warehouse.swt.ui.suppliers.SuppliersPanel;
import com.example.warehouse.swt.ui.util.StatusBar;
import com.example.warehouse.swt.ui.util.Ui;
import com.vaadin.swingbridge.interop.ExposedMethod;
import com.vaadin.swingbridge.interop.InstanceProvider;

/**
 * The bridge entry point of WarehousePro: the main window in <em>bridge
 * mode</em>, without menu, title bar or tabs, showing one module at a time.
 * The Vaadin application provides the shell (login, navigation) and picks the
 * module through the {@link ExposedMethod} methods, which Swing Bridge's
 * codegen plugin turns into a typed {@code BridgeAppBridge} for the Vaadin
 * views. The SWT counterpart of the JavaFX demo's {@code BridgeApp}.
 * <p>
 * The bridge starts one process per user, and every Vaadin route of that user
 * talks to it, so moving between modules keeps its state. The signed-in user
 * comes from the Vaadin login as {@code --user=<username>}.
 * </p>
 */
public final class BridgeApp {

    private static final String USER_ARG = "--user=";
    /**
     * The SWT bridge's {@code SwtBridgeAgent.MAIN_WINDOW}: the application
     * does not depend on the bridge, so the key is repeated here.
     */
    private static final String MAIN_WINDOW = "swingbridge.swt.mainWindow";

    /** One per process, and so per user. */
    private static volatile BridgeApp current;

    private final Shell shell;
    private final Composite modules;
    private final StackLayout stack = new StackLayout();
    private final DashboardPanel dashboard;
    private final ProductsPanel products;
    private final SuppliersPanel suppliers;
    private final PurchaseOrdersPanel orders;
    private final StockMovementsPanel stock;

    private BridgeApp(Display display) {
        User user = AppSession.getCurrentUser();
        // No trim: the Vaadin page is the window frame. SWT drops SWT.RESIZE
        // from a NO_TRIM shell, so tell the bridge it is the main window,
        // which it sizes to the browser viewport.
        shell = new Shell(display, SWT.NO_TRIM);
        shell.setData(MAIN_WINDOW, Boolean.TRUE);
        shell.setText("WarehousePro");
        shell.setLayout(Ui.grid(1, 0, 0));

        modules = new Composite(shell, SWT.NONE);
        modules.setLayoutData(Ui.fill());
        modules.setLayout(stack);
        StatusBar statusBar = new StatusBar(shell,
                user != null ? user.getFullName() : AppSession.getUsername());
        statusBar.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));

        dashboard = new DashboardPanel(modules, statusBar);
        products = new ProductsPanel(modules, statusBar);
        suppliers = new SuppliersPanel(modules, statusBar);
        orders = new PurchaseOrdersPanel(modules, statusBar);
        stock = new StockMovementsPanel(modules, statusBar);
        show(dashboard);
        shell.setSize(1200, 700);
    }

    /**
     * The running application, which the bridge calls the exposed methods on.
     *
     * @return the application, or {@code null} before its window is built
     */
    @InstanceProvider
    public static BridgeApp current() {
        return current;
    }

    public static void main(String[] args) {
        Display.setAppName("WarehousePro");
        Display display = new Display();
        try {
            PersistenceUtil.init();
            DataSeeder.seedIfEmpty();
            signIn(args);
            BridgeApp app = new BridgeApp(display);
            app.shell.open();
            current = app;
            while (!app.shell.isDisposed()) {
                try {
                    if (!display.readAndDispatch()) {
                        display.sleep();
                    }
                } catch (RuntimeException ex) {
                    // Report it and keep the application running, as in
                    // WarehouseApp.
                    ex.printStackTrace();
                }
            }
        } finally {
            current = null;
            PersistenceUtil.shutdown();
            display.dispose();
        }
        // No System.exit: it would end the server and every user's
        // application.
    }

    /** Signs in the user the Vaadin login authenticated. */
    private static void signIn(String[] args) {
        String username = System.getProperty("warehouse.autologin");
        for (String arg : args) {
            if (arg.startsWith(USER_ARG)) {
                username = arg.substring(USER_ARG.length());
            }
        }
        if (username == null || username.isBlank()) {
            return;
        }
        User user = new UserDAO().findByUsername(username.trim());
        if (user != null) {
            AppSession.login(user);
        } else {
            System.err.println("No user '" + username + "'; running signed out.");
        }
    }

    @ExposedMethod
    public void showDashboard() {
        show(dashboard);
    }

    @ExposedMethod
    public void showProducts() {
        show(products);
    }

    @ExposedMethod
    public void showSuppliers() {
        suppliers.setToolbarVisible(true);
        show(suppliers);
    }

    /**
     * Shows the suppliers module as a plain table, for a Vaadin toolbar and
     * dialogs to drive it (stage 3).
     */
    @ExposedMethod
    public void showSuppliersTable() {
        suppliers.setToolbarVisible(false);
        show(suppliers);
    }

    /** Tells {@code listener} about supplier selection changes. */
    @ExposedMethod
    public void setSupplierSelectionListener(SupplierSelectionListener listener) {
        suppliers.setSelectionListener(listener);
    }

    /** Sends supplier edit requests (double-clicks) to {@code listener}. */
    @ExposedMethod
    public void setSupplierEditRequestListener(SupplierEditRequestListener listener) {
        suppliers.setEditRequestListener(listener);
    }

    /** Reloads the suppliers, e.g. after a Vaadin form saved one. */
    @ExposedMethod
    public void refreshSuppliers() {
        suppliers.refresh();
    }

    /** Filters the suppliers table as its own search field would. */
    @ExposedMethod
    public void setSupplierFilter(String filter) {
        suppliers.filterTable(filter);
    }

    @ExposedMethod
    public void showOrders() {
        show(orders);
    }

    @ExposedMethod
    public void showStock() {
        show(stock);
    }

    private void show(Control module) {
        stack.topControl = module;
        modules.layout(true, true);
    }
}
