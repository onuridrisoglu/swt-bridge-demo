package com.example.warehouse.swt.ui.products;

import java.util.Arrays;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import com.example.warehouse.model.Product;
import com.example.warehouse.model.StockMovement;
import com.example.warehouse.service.ProductService;
import com.example.warehouse.swt.ui.util.DataTable;
import com.example.warehouse.swt.ui.util.ObjectCombo;
import com.example.warehouse.swt.ui.util.StatusBar;
import com.example.warehouse.swt.ui.util.TableExporter;
import com.example.warehouse.swt.ui.util.Toolbar;
import com.example.warehouse.swt.ui.util.Ui;

/** Product catalogue: searchable table with add / edit / delete / stock adjust. */
public class ProductsPanel extends Composite {

    private static final String OUT_OF_STOCK = "OUT OF STOCK";
    private static final String LOW_STOCK = "Low Stock";
    private static final String OK = "OK";

    private final ProductService service = new ProductService();
    private final StatusBar statusBar;
    private DataTable<Product> table;

    private Button editBtn;
    private Button deleteBtn;
    private Button stockBtn;

    public ProductsPanel(Composite parent, StatusBar statusBar) {
        super(parent, SWT.NONE);
        this.statusBar = statusBar;
        setBackground(Ui.color(Ui.WHITE));
        setLayout(Ui.grid(1, 8, 0));

        buildToolbar();
        buildTable();

        refresh();
    }

    private void buildToolbar() {
        Toolbar toolbar = new Toolbar(this);
        toolbar.button("+ Add Product", this::openAddDialog);
        toolbar.separator();
        editBtn = toolbar.button("Edit", this::openEditDialog);
        deleteBtn = toolbar.button("Delete", this::deleteSelected);
        toolbar.separator();
        stockBtn = toolbar.button("± Stock Adjust", this::openStockAdjustDialog);
        toolbar.separator();
        toolbar.button("↺ Refresh", this::refresh);
        toolbar.separator();
        toolbar.button("⬇ Export CSV",
                () -> TableExporter.exportToCsv(table, "products.csv", getShell()));
        toolbar.fill();
        toolbar.label("Search:");
        toolbar.searchField(this::filterTable);
        toolbar.done();
    }

    private void buildTable() {
        table = new DataTable<>(this);
        table.getTable().setLayoutData(Ui.fill());
        table.column("SKU", 80, Product::getSku);
        table.column("Product Name", 200, Product::getName);
        table.column("Category", 130, p -> p.getCategory().getName());
        table.column("Supplier", 180,
                p -> p.getSupplier() != null ? p.getSupplier().getName() : "—");
        // Numeric columns are centered, like the Swing renderer
        table.column("On Hand", 75, Product::getQuantity).center();
        table.column("Reorder", 75, Product::getReorderLevel).center();
        table.column("Unit Price", 90, Product::getUnitPrice).center();
        // Status text colored green / orange / red
        table.column("Status", 90, ProductsPanel::statusOf).foreground(p -> switch (statusOf(p)) {
            case OUT_OF_STOCK -> Ui.DANGER;
            case LOW_STOCK -> Ui.WARNING;
            default -> Ui.SUCCESS;
        });
        table.setRowBackground(p -> p.getQuantity() == 0 ? Integer.valueOf(Ui.CRITICAL)
                : p.isLowStock() ? Integer.valueOf(Ui.LOW_STOCK) : null);

        table.onSelectionChanged(this::updateButtons);
        table.onOpen(p -> openEditDialog());

        Menu menu = table.contextMenu();
        DataTable.menuItem(menu, "Edit Product", this::openEditDialog);
        DataTable.menuItem(menu, "Delete Product", this::deleteSelected);
        new MenuItem(menu, SWT.SEPARATOR);
        DataTable.menuItem(menu, "Adjust Stock", this::openStockAdjustDialog);
    }

    private void updateButtons() {
        boolean selected = table.getSelected() != null;
        editBtn.setEnabled(selected);
        deleteBtn.setEnabled(selected);
        stockBtn.setEnabled(selected);
    }

    private static String statusOf(Product p) {
        return p.isLowStock() ? (p.getQuantity() == 0 ? OUT_OF_STOCK : LOW_STOCK) : OK;
    }

    /** Case-insensitive match of the text against any column's displayed value. */
    public void filterTable(String text) {
        String needle = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            table.setFilter(null);
        } else {
            table.setFilter(p -> table.anyColumnMatches(p,
                    v -> v.toLowerCase(Locale.ROOT).contains(needle)));
        }
        statusBar.setMessage(table.getVisibleItems().size() + " of " + table.getItems().size()
                + " products shown");
    }

    public void refresh() {
        table.setItems(service.findAll());
        statusBar.setRowCount(table.getItems().size(), "product");
    }

    private void openAddDialog() {
        if (new ProductDialog(getShell(), null).open()) {
            refresh();
        }
    }

    private void openEditDialog() {
        Product p = table.getSelected();
        if (p == null) {
            return;
        }
        if (new ProductDialog(getShell(), p).open()) {
            refresh();
        }
    }

    private void deleteSelected() {
        Product p = table.getSelected();
        if (p == null) {
            return;
        }
        Shell shell = getShell();
        if (!Ui.confirm(shell, "Confirm Delete", "Delete product '" + p.getName() + "' ("
                + p.getSku() + ")?\n\nThis cannot be undone.")) {
            return;
        }
        try {
            service.delete(p.getId());
            refresh();
            statusBar.setMessage("Product '" + p.getName() + "' deleted.");
        } catch (Exception ex) {
            Ui.error(shell, "Delete Error",
                    "Cannot delete: product may be referenced by orders or stock movements.");
        }
    }

    private void openStockAdjustDialog() {
        Product p = table.getSelected();
        if (p == null) {
            return;
        }

        Shell dialog = Ui.dialogShell(getShell(), "Stock Adjustment — " + p.getName(), true);
        dialog.setLayout(Ui.grid(1, 0, 0));

        Composite form = new Composite(dialog, SWT.NONE);
        form.setLayoutData(Ui.fill());
        GridLayout layout = Ui.grid(2, 0, 10);
        layout.marginTop = 16;
        layout.marginLeft = 20;
        layout.marginRight = 20;
        layout.marginBottom = 8;
        form.setLayout(layout);

        leftLabel(form, "Product:");
        new Label(form, SWT.NONE).setText(p.getName());
        leftLabel(form, "Current Qty:");
        Label currentQty = new Label(form, SWT.NONE);
        currentQty.setText(String.valueOf(p.getQuantity()));
        currentQty.setFont(Ui.bold());

        leftLabel(form, "Movement Type:");
        ObjectCombo<StockMovement.MovementType> typeBox = new ObjectCombo<>(form,
                StockMovement.MovementType::toString);
        typeBox.setItems(Arrays.asList(StockMovement.MovementType.values()));
        typeBox.selectFirst();
        Combo typeCombo = typeBox.getControl();
        typeCombo.setLayoutData(Ui.fillHorizontal());

        leftLabel(form, "Quantity:");
        Spinner qtySpinner = new Spinner(form, SWT.BORDER);
        qtySpinner.setValues(1, 0, 99999, 0, 1, 10);
        qtySpinner.setLayoutData(Ui.fillHorizontal());

        leftLabel(form, "Reason:");
        Text reasonField = Ui.textField(form, 20);

        Composite buttons = new Composite(dialog, SWT.NONE);
        buttons.setLayoutData(Ui.fillHorizontal());
        buttons.setLayout(Ui.grid(1, 12, 0));
        Ui.buttonBar(buttons, "Apply", () -> {
            String reason = reasonField.getText().trim();
            if (reason.isEmpty()) {
                Ui.warn(dialog, "Validation", "Please enter a reason.");
                return;
            }
            try {
                service.adjustStock(p, typeBox.getValue(), qtySpinner.getSelection(), reason);
            } catch (RuntimeException ex) {
                Ui.error(dialog, "Error", ex.getMessage());
                return;
            }
            refresh();
            dialog.close();
        });

        dialog.setSize(dialog.computeSize(380, SWT.DEFAULT));
        Ui.openModal(dialog, reasonField);
    }

    private static void leftLabel(Composite parent, String text) {
        Label label = Ui.fieldLabel(parent, text);
        label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
    }
}
