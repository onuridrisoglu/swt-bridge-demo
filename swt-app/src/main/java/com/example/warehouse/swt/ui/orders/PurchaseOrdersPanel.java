package com.example.warehouse.swt.ui.orders;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Menu;

import com.example.warehouse.model.PurchaseOrder;
import com.example.warehouse.model.PurchaseOrderItem;
import com.example.warehouse.service.PurchaseOrderService;
import com.example.warehouse.swt.ui.util.DataTable;
import com.example.warehouse.swt.ui.util.StatusBar;
import com.example.warehouse.swt.ui.util.TableExporter;
import com.example.warehouse.swt.ui.util.Toolbar;
import com.example.warehouse.swt.ui.util.Ui;

/** Purchase orders: the order list on top, the selected order's line items below. */
public class PurchaseOrdersPanel extends Composite {

    static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PurchaseOrderService service = new PurchaseOrderService();
    private final StatusBar statusBar;

    private DataTable<PurchaseOrder> orderTable;
    private DataTable<PurchaseOrderItem> itemTable;

    private Button editBtn;
    private Button deleteBtn;

    public PurchaseOrdersPanel(Composite parent, StatusBar statusBar) {
        super(parent, SWT.NONE);
        this.statusBar = statusBar;
        setBackground(Ui.color(Ui.WHITE));
        setLayout(Ui.grid(1, 8, 8));

        buildToolbar();
        SashForm split = new SashForm(this, SWT.VERTICAL | SWT.SMOOTH);
        split.setLayoutData(Ui.fill());
        buildOrderTable(split);
        buildItemPanel(split);
        split.setWeights(60, 40);

        refresh();
    }

    private void buildToolbar() {
        Toolbar toolbar = new Toolbar(this);
        toolbar.button("+ New Order", this::openAddDialog);
        toolbar.separator();
        editBtn = toolbar.button("Edit / View", this::openEditDialog);
        deleteBtn = toolbar.button("Delete", this::deleteSelected);
        toolbar.separator();
        toolbar.button("↺ Refresh", this::refresh);
        toolbar.separator();
        toolbar.button("⬇ Export CSV",
                () -> TableExporter.exportToCsv(orderTable, "orders.csv", getShell()));
        toolbar.done();
        editBtn.setEnabled(false);
        deleteBtn.setEnabled(false);
    }

    private void buildOrderTable(Composite parent) {
        Group section = Ui.section(parent, "Purchase Orders");
        orderTable = new DataTable<>(section);
        orderTable.column("Order #", 70, PurchaseOrder::getId)
                .format(id -> String.format("PO-%04d", (Long) id));
        orderTable.column("Supplier", 200, o -> o.getSupplier().getName());
        orderTable.column("Order Date", 100, PurchaseOrder::getOrderDate)
                .format(d -> ((LocalDate) d).format(DATE_FMT));
        orderTable.column("Expected", 100, PurchaseOrder::getExpectedDate)
                .format(d -> d != null ? ((LocalDate) d).format(DATE_FMT) : "—");
        orderTable.column("Status", 90, o -> o.getStatus().toString())
                .foreground(o -> switch (o.getStatus()) {
                    case CANCELLED -> Ui.DANGER;
                    case DELIVERED -> Ui.SUCCESS;
                    case ORDERED -> Ui.ACCENT;
                    case PENDING -> Ui.WARNING;
                });
        orderTable.column("Items", 60, o -> o.getItems().size());
        // Sorted by amount, shown as currency
        orderTable.column("Total Amount", 110, PurchaseOrder::getTotalAmount)
                .format(Ui.CURRENCY::format);

        // Tint rows by order status like the Swing renderer; pending rows keep the striping
        orderTable.setRowBackground(o -> switch (o.getStatus()) {
            case CANCELLED -> 0xfaebeb;
            case DELIVERED -> 0xebfaeb;
            case ORDERED -> 0xebf2fc;
            case PENDING -> null;
        });

        orderTable.onSelectionChanged(() -> {
            boolean selected = orderTable.getSelected() != null;
            editBtn.setEnabled(selected);
            deleteBtn.setEnabled(selected);
            showOrderItems();
        });
        orderTable.onOpen(o -> openEditDialog());

        Menu menu = orderTable.contextMenu();
        DataTable.menuItem(menu, "View / Edit Order", this::openEditDialog);
        DataTable.menuItem(menu, "Delete Order", this::deleteSelected);
    }

    private void buildItemPanel(Composite parent) {
        Group section = Ui.section(parent, "Order Line Items");
        itemTable = new DataTable<>(section);
        // The Swing detail table has no row sorter
        itemTable.column("Product SKU", 120, i -> i.getProduct().getSku()).notSortable();
        itemTable.column("Product Name", 220, i -> i.getProduct().getName()).notSortable();
        itemTable.column("Quantity", 90, PurchaseOrderItem::getQuantity).center().notSortable();
        itemTable.column("Unit Price", 100, i -> Ui.CURRENCY.format(i.getUnitPrice()))
                .center().notSortable();
        itemTable.column("Line Total", 100, i -> Ui.CURRENCY.format(i.getLineTotal()))
                .center().notSortable();
    }

    private void showOrderItems() {
        PurchaseOrder order = orderTable.getSelected();
        itemTable.setItems(order == null ? List.of() : order.getItems());
    }

    public void refresh() {
        orderTable.setItems(service.findAll());
        orderTable.clearSelection();
        statusBar.setRowCount(orderTable.getItems().size(), "purchase order");
    }

    private void openAddDialog() {
        if (new PurchaseOrderDialog(getShell(), null).open()) {
            refresh();
        }
    }

    private void openEditDialog() {
        PurchaseOrder o = orderTable.getSelected();
        if (o == null) {
            return;
        }
        if (new PurchaseOrderDialog(getShell(), o).open()) {
            refresh();
        }
    }

    private void deleteSelected() {
        PurchaseOrder o = orderTable.getSelected();
        if (o == null) {
            return;
        }
        boolean confirmed = Ui.confirm(getShell(), "Confirm Delete",
                "Delete purchase order PO-" + String.format("%04d", o.getId())
                        + "?\n\nThis cannot be undone.");
        if (confirmed) {
            service.delete(o.getId());
            refresh();
            statusBar.setMessage("Purchase order deleted.");
        }
    }
}
