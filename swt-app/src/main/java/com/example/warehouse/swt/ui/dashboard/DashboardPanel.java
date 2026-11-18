package com.example.warehouse.swt.ui.dashboard;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import com.example.warehouse.model.Product;
import com.example.warehouse.service.ProductService;
import com.example.warehouse.service.PurchaseOrderService;
import com.example.warehouse.swt.ui.util.DataTable;
import com.example.warehouse.swt.ui.util.StatusBar;
import com.example.warehouse.swt.ui.util.Ui;

/**
 * Overview tab: KPI cards, stock and order charts, and the low-stock alert
 * table.
 */
public final class DashboardPanel extends Composite {

    private static final Map<String, Integer> STATUS_COLORS = Map.of(
            "Delivered", 0x32a050,
            "Ordered", 0x4682b4,
            "Pending", 0xc88c1e,
            "Cancelled", 0xb43c3c);

    private final ProductService productService = new ProductService();
    private final PurchaseOrderService orderService = new PurchaseOrderService();
    private final StatusBar statusBar;

    private Label totalProductsValue;
    private Label stockValueLabel;
    private Label pendingOrdersValue;
    private Label lowStockValue;

    private BarChart stockChart;
    private PieChart orderStatusChart;
    private DataTable<Product> alertTable;

    public DashboardPanel(Composite parent, StatusBar statusBar) {
        super(parent, SWT.NONE);
        this.statusBar = statusBar;
        setBackground(Ui.color(Ui.PANEL_BG));
        setLayout(Ui.grid(1, 10, 8));

        buildKpiPanel();
        buildChartsPanel();
        buildAlertPanel();

        refresh();
    }

    private void buildKpiPanel() {
        Composite grid = new Composite(this, SWT.NONE);
        GridLayout layout = new GridLayout(4, true);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = 10;
        grid.setLayout(layout);
        grid.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        totalProductsValue = kpiCard(grid, "Total Products", Ui.PRIMARY);
        stockValueLabel = kpiCard(grid, "Inventory Value", 0x00643c);
        pendingOrdersValue = kpiCard(grid, "Pending Orders", 0x8c5000);
        lowStockValue = kpiCard(grid, "Low Stock Alerts", Ui.DANGER);
    }

    /** A white card with a colored left edge, a caption and a big value. */
    private static Label kpiCard(Composite parent, String caption, int accentColor) {
        Composite card = new Composite(parent, SWT.NONE);
        card.setBackground(Ui.color(Ui.WHITE));
        card.setBackgroundMode(SWT.INHERIT_DEFAULT);
        GridLayout layout = new GridLayout(1, false);
        layout.marginLeft = 19;
        layout.marginRight = 14;
        layout.marginHeight = 10;
        layout.verticalSpacing = 4;
        card.setLayout(layout);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
        data.heightHint = 90;
        card.setLayoutData(data);
        card.addPaintListener(e -> {
            Point size = card.getSize();
            e.gc.setForeground(Ui.color(Ui.BORDER));
            e.gc.drawRectangle(0, 0, size.x - 1, size.y - 1);
            e.gc.setBackground(Ui.color(accentColor));
            e.gc.fillRectangle(0, 0, 5, size.y);
        });

        Label captionLabel = new Label(card, SWT.NONE);
        captionLabel.setText(caption.toUpperCase());
        captionLabel.setFont(Ui.font(-2, SWT.NORMAL));
        captionLabel.setForeground(Ui.color(0x5a6c87));

        Label value = new Label(card, SWT.NONE);
        value.setText("—");
        value.setFont(Ui.font(8, SWT.BOLD));
        value.setForeground(Ui.color(Ui.PRIMARY_DARK));
        value.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        return value;
    }

    private void buildChartsPanel() {
        Composite grid = new Composite(this, SWT.NONE);
        GridLayout layout = new GridLayout(2, true);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = 10;
        grid.setLayout(layout);
        GridData data = Ui.fill();
        data.heightHint = 220;
        grid.setLayoutData(data);

        stockChart = new BarChart(grid, "Stock Quantity by Category", "Category",
                "Units in Stock", "Qty", Ui.ACCENT);
        stockChart.setLayoutData(Ui.fill());
        orderStatusChart = new PieChart(grid, "Purchase Orders by Status", STATUS_COLORS::get);
        orderStatusChart.setLayoutData(Ui.fill());
    }

    private void buildAlertPanel() {
        Group section = Ui.section(this, "⚠ Low Stock Alerts");
        GridData data = new GridData(SWT.FILL, SWT.BOTTOM, true, false);
        data.heightHint = 180;
        section.setLayoutData(data);

        alertTable = new DataTable<>(section);
        alertTable.column("SKU", 90, Product::getSku);
        alertTable.column("Product Name", 220, Product::getName);
        alertTable.column("Category", 140, p -> p.getCategory().getName());
        alertTable.column("Supplier", 200,
                p -> p.getSupplier() != null ? p.getSupplier().getName() : "—");
        alertTable.column("On Hand", 90, Product::getQuantity).center();
        alertTable.column("Reorder Level", 100, Product::getReorderLevel).center();
        alertTable.column("Shortage", 90, p -> p.getReorderLevel() - p.getQuantity()).center();
        alertTable.setRowBackground(p -> p.getQuantity() == 0 ? Ui.CRITICAL : Ui.LOW_STOCK);
    }

    public void refresh() {
        long total = productService.count();
        long lowStock = productService.countLowStock();
        long pendingOrders = orderService.countPending();
        BigDecimal value = productService.totalInventoryValue();

        totalProductsValue.setText(String.valueOf(total));
        lowStockValue.setText(String.valueOf(lowStock));
        lowStockValue.setForeground(Ui.color(lowStock > 0 ? Ui.DANGER : Ui.SUCCESS));
        pendingOrdersValue.setText(String.valueOf(pendingOrders));
        pendingOrdersValue.setForeground(Ui.color(pendingOrders > 0 ? Ui.WARNING : Ui.SUCCESS));
        stockValueLabel.setText(Ui.CURRENCY.format(value));

        refreshCharts();

        List<Product> lowStockProducts = productService.findLowStock();
        alertTable.setItems(lowStockProducts);

        statusBar.setMessage("Dashboard refreshed — " + lowStockProducts.size()
                + " low-stock alert(s)");
    }

    private void refreshCharts() {
        List<String> categories = new ArrayList<>();
        List<Double> quantities = new ArrayList<>();
        for (Object[] row : productService.findStockByCategory()) {
            categories.add((String) row[0]);
            quantities.add(((Number) row[1]).doubleValue());
        }
        stockChart.setData(categories, quantities);

        List<String> statuses = new ArrayList<>();
        List<Double> counts = new ArrayList<>();
        for (Object[] row : orderService.countByAllStatuses()) {
            statuses.add(row[0].toString());
            counts.add(((Number) row[1]).doubleValue());
        }
        orderStatusChart.setData(statuses, counts);
    }
}
