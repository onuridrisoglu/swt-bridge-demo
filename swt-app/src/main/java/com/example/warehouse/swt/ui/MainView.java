package com.example.warehouse.swt.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import com.example.warehouse.AppSession;
import com.example.warehouse.model.User;
import com.example.warehouse.swt.ui.dashboard.DashboardPanel;
import com.example.warehouse.swt.ui.orders.PurchaseOrdersPanel;
import com.example.warehouse.swt.ui.products.ProductsPanel;
import com.example.warehouse.swt.ui.stock.StockMovementsPanel;
import com.example.warehouse.swt.ui.suppliers.SuppliersPanel;
import com.example.warehouse.swt.ui.util.StatusBar;
import com.example.warehouse.swt.ui.util.Ui;

/**
 * The main window: menu bar, title bar, the five module tabs and the status
 * bar. A parentless, resizable shell; the module tabs are the primary
 * navigation, the View menu only mirrors them.
 */
public final class MainView {

    private static final String[] TABS = { "Dashboard", "Products", "Suppliers",
            "Orders", "Stock Log" };
    private static final int TITLE_BAR_HEIGHT = 42;

    private final Shell shell;
    private final StatusBar statusBar;
    private final TabFolder tabFolder;
    private boolean loggedOut;

    public MainView(Display display) {
        User user = AppSession.getCurrentUser();
        shell = new Shell(display, SWT.SHELL_TRIM);
        shell.setText("WarehousePro™ 3.5  —  Inventory & Warehouse Management");
        shell.setLayout(Ui.grid(1, 0, 0));
        shell.setMinimumSize(900, 600);

        buildMenuBar();
        buildTitleBar(user);
        tabFolder = new TabFolder(shell, SWT.TOP);
        tabFolder.setLayoutData(Ui.fill());
        tabFolder.setFont(Ui.bold());
        statusBar = new StatusBar(shell, user.getFullName());
        statusBar.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
        buildTabs();

        Rectangle screen = display.getPrimaryMonitor().getClientArea();
        shell.setSize(Math.min(1280, screen.width), Math.min(800, screen.height));
    }

    /** Centers and shows the window; the caller runs the event loop. */
    public Shell open() {
        Ui.centerOnPrimaryMonitor(shell);
        shell.open();
        return shell;
    }

    /** Whether the window was closed by logging out (show the login again). */
    public boolean isLoggedOut() {
        return loggedOut;
    }

    private void buildTitleBar(User user) {
        Canvas bar = new Canvas(shell, SWT.DOUBLE_BUFFERED);
        GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
        data.heightHint = TITLE_BAR_HEIGHT;
        bar.setLayoutData(data);
        GridLayout layout = Ui.grid(1, 0, 0);
        layout.marginRight = 12;
        bar.setLayout(layout);

        Button logout = new Button(bar, SWT.PUSH);
        logout.setText("Logout");
        logout.setFont(Ui.font(-1, SWT.NORMAL));
        logout.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, true));
        logout.addListener(SWT.Selection, e -> doLogout());

        String userInfo = user.getFullName() + "  [" + user.getRole() + "]";
        bar.addPaintListener(e -> {
            GC gc = e.gc;
            Rectangle area = bar.getClientArea();
            gc.setForeground(Ui.color(Ui.PRIMARY_DARK));
            gc.setBackground(Ui.color(Ui.PRIMARY));
            gc.fillGradientRectangle(0, 0, area.width, area.height, false);
            gc.setTextAntialias(SWT.ON);

            gc.setFont(Ui.font(3, SWT.BOLD));
            gc.setForeground(Ui.color(Ui.WHITE));
            Point title = gc.textExtent("WarehousePro™ 3.5");
            gc.drawText("WarehousePro™ 3.5", 12, (area.height - title.y) / 2, true);

            gc.setFont(Ui.font(-1, SWT.NORMAL));
            gc.setForeground(Ui.color(Ui.LIGHT_TEXT));
            Point info = gc.textExtent(userInfo);
            int x = logout.getBounds().x - 8 - info.x;
            gc.drawText(userInfo, x, (area.height - info.y) / 2, true);
        });
    }

    private void buildTabs() {
        addTab(TABS[0], new DashboardPanel(tabFolder, statusBar));
        addTab(TABS[1], new ProductsPanel(tabFolder, statusBar));
        addTab(TABS[2], new SuppliersPanel(tabFolder, statusBar));
        addTab(TABS[3], new PurchaseOrdersPanel(tabFolder, statusBar));
        addTab(TABS[4], new StockMovementsPanel(tabFolder, statusBar));
    }

    private void addTab(String title, Control panel) {
        TabItem item = new TabItem(tabFolder, SWT.NONE);
        item.setText(title);
        item.setControl(panel);
    }

    private void buildMenuBar() {
        Menu bar = new Menu(shell, SWT.BAR);
        shell.setMenuBar(bar);

        Menu fileMenu = submenu(bar, "&File");
        menuItem(fileMenu, "Logout", this::doLogout);
        new MenuItem(fileMenu, SWT.SEPARATOR);
        menuItem(fileMenu, "Exit", this::confirmExit);

        Menu viewMenu = submenu(bar, "&View");
        for (int i = 0; i < TABS.length; i++) {
            int index = i;
            menuItem(viewMenu, TABS[i], () -> tabFolder.setSelection(index));
        }

        Menu helpMenu = submenu(bar, "&Help");
        menuItem(helpMenu, "About WarehousePro", () -> Ui.info(shell, "About WarehousePro",
                "WarehousePro™ 3.5\nInventory & Warehouse Management System\n\n"
                        + "© 2009 Acme Logistics Software Inc.\nAll rights reserved.\n\n"
                        + "Build 3.5.0 (2009-11-04)"));
    }

    private Menu submenu(Menu bar, String text) {
        MenuItem header = new MenuItem(bar, SWT.CASCADE);
        header.setText(text);
        Menu menu = new Menu(shell, SWT.DROP_DOWN);
        header.setMenu(menu);
        return menu;
    }

    private static void menuItem(Menu menu, String text, Runnable action) {
        MenuItem item = new MenuItem(menu, SWT.PUSH);
        item.setText(text);
        item.addListener(SWT.Selection, e -> action.run());
    }

    private void doLogout() {
        AppSession.logout();
        loggedOut = true;
        shell.dispose();
    }

    private void confirmExit() {
        if (Ui.confirm(shell, "Confirm Exit", "Are you sure you want to exit WarehousePro?")) {
            // Close the window rather than exiting the JVM: WarehouseApp's
            // event loop then ends and shuts the database down.
            shell.dispose();
        }
    }
}
