package com.example.warehouse.swt.ui.orders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.example.warehouse.model.Product;
import com.example.warehouse.model.PurchaseOrder;
import com.example.warehouse.model.PurchaseOrderItem;
import com.example.warehouse.model.Supplier;
import com.example.warehouse.service.ProductService;
import com.example.warehouse.service.PurchaseOrderService;
import com.example.warehouse.service.SupplierService;
import com.example.warehouse.swt.ui.util.DataTable;
import com.example.warehouse.swt.ui.util.ObjectCombo;
import com.example.warehouse.swt.ui.util.Toolbar;
import com.example.warehouse.swt.ui.util.Ui;

/**
 * Create / edit dialog for a purchase order with an editable line items
 * table (click a Quantity or Unit Price cell to edit it). A modal child shell
 * of the main window; {@link #open()} returns whether the order was saved.
 */
public class PurchaseOrderDialog {

    private static final int QUANTITY_COLUMN = 2;
    private static final int PRICE_COLUMN = 3;

    private final PurchaseOrder existing;
    private final PurchaseOrderService orderService = new PurchaseOrderService();
    private final SupplierService supplierService = new SupplierService();
    private final ProductService productService = new ProductService();
    private final Shell shell;
    private boolean saved = false;

    private ObjectCombo<Supplier> supplierBox;
    private Text orderDateField;
    private Text expectedDateField;
    private ObjectCombo<PurchaseOrder.OrderStatus> statusBox;
    private Text notesArea;

    private DataTable<ItemRow> itemTable;
    private Label totalLabel;
    private SashForm split;
    private Group headerSection;

    public PurchaseOrderDialog(Shell owner, PurchaseOrder order) {
        this.existing = order;
        shell = Ui.dialogShell(owner, order == null ? "New Purchase Order"
                : "Purchase Order — PO-" + String.format("%04d", order.getId()), true);
        shell.setLayout(Ui.grid(1, 12, 8));

        buildUI();
        if (existing != null) {
            populateFields();
        } else {
            orderDateField.setText(LocalDate.now().format(PurchaseOrdersPanel.DATE_FMT));
        }
        // The JavaFX dialog pane's preferred size, as the content area
        Rectangle trim = shell.computeTrim(0, 0, 700, 540);
        shell.setSize(trim.width, trim.height);
        fitHeaderPane();
    }

    /** Gives the order details their preferred height; the line items get the rest. */
    private void fitHeaderPane() {
        shell.layout(true, true);
        int total = split.getSize().y;
        int header = headerSection.computeSize(split.getSize().x, SWT.DEFAULT).y;
        if (total > header) {
            split.setWeights(header, total - header);
        }
    }

    /** Shows the dialog until it is closed; returns whether the order was saved. */
    public boolean open() {
        Ui.openModal(shell);
        return saved;
    }

    private void buildUI() {
        Label title = Ui.panelTitle(shell,
                existing == null ? "New Purchase Order" : "Edit Purchase Order");
        title.setForeground(Ui.color(Ui.PRIMARY_DARK));

        split = new SashForm(shell, SWT.VERTICAL | SWT.SMOOTH);
        split.setLayoutData(Ui.fill());
        buildHeaderPanel(split);
        buildItemsPanel(split);
        split.setWeights(40, 60);

        Ui.buttonBar(shell, "Save Order", () -> {
            // Keep the dialog open on validation or save errors
            if (doSave()) {
                shell.close();
            }
        });
    }

    private void buildHeaderPanel(SashForm parent) {
        Group section = Ui.section(parent, "Order Details");
        headerSection = section;
        GridLayout form = Ui.grid(2, 8, 8);
        form.horizontalSpacing = 12;
        section.setLayout(form);

        Ui.fieldLabel(section, "Supplier: *");
        supplierBox = new ObjectCombo<>(section, Supplier::getName);
        supplierBox.setItems(supplierService.findAll());
        supplierBox.selectFirst();
        supplierBox.getControl().setLayoutData(Ui.fillHorizontal());

        Ui.fieldLabel(section, "Order Date: *");
        orderDateField = Ui.textField(section, 10);
        Ui.fieldLabel(section, "Expected Date:");
        expectedDateField = Ui.textField(section, 10);

        Ui.fieldLabel(section, "Status:");
        statusBox = new ObjectCombo<>(section, PurchaseOrder.OrderStatus::toString);
        statusBox.setItems(Arrays.asList(PurchaseOrder.OrderStatus.values()));
        statusBox.selectFirst();
        statusBox.getControl().setLayoutData(Ui.fillHorizontal());

        Label notesLabel = Ui.fieldLabel(section, "Notes:");
        notesLabel.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));
        notesArea = Ui.textArea(section, 3);
    }

    private void buildItemsPanel(SashForm parent) {
        Group section = Ui.section(parent, "Line Items");
        section.setLayout(Ui.grid(1, 8, 4));

        Toolbar toolbar = new Toolbar(section);
        toolbar.button("+ Add Item", this::addItem);
        toolbar.button("Remove", this::removeItem);
        toolbar.done();

        itemTable = new DataTable<>(section);
        itemTable.column("Product SKU", 110, r -> r.sku).notSortable();
        itemTable.column("Product Name", 200, r -> r.name).notSortable();
        itemTable.column("Quantity", 90, r -> r.quantity).notSortable();
        itemTable.column("Unit Price ($)", 110, r -> r.unitPrice).notSortable();
        itemTable.column("Line Total", 100, r -> r.lineTotal).notSortable();
        installCellEditor(itemTable.getTable());

        totalLabel = new Label(section, SWT.RIGHT);
        totalLabel.setText("Total: $0.00");
        totalLabel.setFont(Ui.font(1, SWT.BOLD));
        GridData totalData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        totalData.horizontalIndent = 0;
        totalLabel.setLayoutData(totalData);
    }

    /** Clicking a Quantity or Unit Price cell edits it in place, like the editable Swing / JavaFX table. */
    private void installCellEditor(Table table) {
        TableEditor editor = new TableEditor(table);
        editor.grabHorizontal = true;
        editor.horizontalAlignment = SWT.LEFT;
        table.addListener(SWT.MouseDown, e -> {
            Point point = new Point(e.x, e.y);
            TableItem item = table.getItem(point);
            if (item == null) {
                return;
            }
            for (int column : new int[] { QUANTITY_COLUMN, PRICE_COLUMN }) {
                if (item.getBounds(column).contains(point)) {
                    editCell(editor, item, column);
                    return;
                }
            }
        });
    }

    private void editCell(TableEditor editor, TableItem item, int column) {
        ItemRow row = (ItemRow) item.getData();
        Text text = new Text(itemTable.getTable(), SWT.NONE);
        text.setText(item.getText(column));
        boolean[] finished = { false };
        Listener listener = (Event e) -> {
            if (finished[0]) {
                return;
            }
            boolean commit;
            if (e.type == SWT.FocusOut) {
                commit = true;
            } else if (e.detail == SWT.TRAVERSE_RETURN) {
                commit = true;
                e.doit = false; // not the dialog's default button
            } else if (e.detail == SWT.TRAVERSE_ESCAPE) {
                commit = false;
                e.doit = false; // not closing the dialog
            } else {
                return;
            }
            finished[0] = true;
            String value = text.getText();
            text.dispose();
            if (commit) {
                commitIfValid(row, column == QUANTITY_COLUMN, value);
            }
        };
        text.addListener(SWT.FocusOut, listener);
        text.addListener(SWT.Traverse, listener);
        editor.setEditor(text, item, column);
        text.selectAll();
        text.setFocus();
    }

    /** Accepts a quantity / price edit only when it parses; otherwise keeps the old value. */
    private void commitIfValid(ItemRow row, boolean quantity, String value) {
        String text = value == null ? "" : value.trim();
        try {
            if (quantity) {
                row.quantity = String.valueOf(Integer.parseInt(text));
            } else {
                row.unitPrice = new BigDecimal(text).toPlainString();
            }
        } catch (NumberFormatException ex) {
            // keep the previous value
        }
        updateTotal();
    }

    private void addItem() {
        List<Product> products = productService.findAll();
        Product product = Ui.choose(shell, "Add Line Item", "Select product to add:", products,
                Product::getName);
        if (product == null) {
            return;
        }
        itemTable.getItems().add(new ItemRow(new PurchaseOrderItem(product, 1,
                product.getUnitPrice())));
        updateTotal();
    }

    private void removeItem() {
        ItemRow row = itemTable.getSelected();
        if (row == null) {
            return;
        }
        itemTable.getItems().remove(row);
        updateTotal();
    }

    private void updateTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemRow row : itemTable.getItems()) {
            try {
                int qty = Integer.parseInt(row.quantity);
                BigDecimal price = new BigDecimal(row.unitPrice);
                BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(qty));
                row.lineTotal = lineTotal.toPlainString();
                total = total.add(lineTotal);
            } catch (RuntimeException ignored) {
                // Unparseable rows do not count, as in the Swing version
            }
        }
        itemTable.refresh();
        totalLabel.setText("Order Total: " + Ui.CURRENCY.format(total));
        totalLabel.getParent().layout();
    }

    private void populateFields() {
        supplierBox.selectFirstMatch(s -> s.getId().equals(existing.getSupplier().getId()));
        orderDateField.setText(existing.getOrderDate().format(PurchaseOrdersPanel.DATE_FMT));
        expectedDateField.setText(existing.getExpectedDate() != null
                ? existing.getExpectedDate().format(PurchaseOrdersPanel.DATE_FMT) : "");
        statusBox.select(existing.getStatus());
        notesArea.setText(existing.getNotes() != null ? existing.getNotes() : "");

        itemTable.setItems(existing.getItems().stream().map(ItemRow::new).toList());
        updateTotal();
    }

    /** Validates and saves; returns false to keep the dialog open. */
    private boolean doSave() {
        String dateStr = orderDateField.getText().trim();
        if (supplierBox.getValue() == null || dateStr.isEmpty()) {
            Ui.warn(shell, "Validation", "Supplier and Order Date are required.");
            return false;
        }

        LocalDate orderDate;
        try {
            orderDate = LocalDate.parse(dateStr, PurchaseOrdersPanel.DATE_FMT);
        } catch (DateTimeParseException e) {
            Ui.warn(shell, "Validation", "Order date must be in format yyyy-MM-dd.");
            return false;
        }

        LocalDate expectedDate = null;
        String expStr = expectedDateField.getText().trim();
        if (!expStr.isEmpty()) {
            try {
                expectedDate = LocalDate.parse(expStr, PurchaseOrdersPanel.DATE_FMT);
            } catch (DateTimeParseException e) {
                Ui.warn(shell, "Validation", "Expected date must be in format yyyy-MM-dd.");
                return false;
            }
        }

        // Sync item quantities/prices from the table
        List<ItemRow> rows = itemTable.getItems();
        for (ItemRow row : rows) {
            try {
                row.item.setQuantity(Integer.parseInt(row.quantity));
                row.item.setUnitPrice(new BigDecimal(row.unitPrice));
            } catch (RuntimeException ignored) {
                // Keep the previous value
            }
        }

        PurchaseOrder order = existing != null ? existing : new PurchaseOrder();
        order.setSupplier(supplierBox.getValue());
        order.setOrderDate(orderDate);
        order.setExpectedDate(expectedDate);
        order.setStatus(statusBox.getValue());
        order.setNotes(notesArea.getText().trim());

        List<PurchaseOrderItem> lineItems = rows.stream().map(r -> r.item).toList();
        if (existing == null) {
            lineItems.forEach(order::addItem);
            try {
                orderService.save(order);
            } catch (Exception ex) {
                Ui.error(shell, "Error", "Failed to save order:\n" + ex.getMessage());
                return false;
            }
        } else {
            // Apply added / removed line items to the existing order
            order.getItems().retainAll(lineItems);
            lineItems.stream().filter(i -> !order.getItems().contains(i)).forEach(order::addItem);
            try {
                orderService.update(order);
            } catch (Exception ex) {
                Ui.error(shell, "Error", "Failed to update order:\n" + ex.getMessage());
                return false;
            }
        }

        saved = true;
        return true;
    }

    /** One line of the items table; quantity and price are edited as text like the Swing table. */
    private static final class ItemRow {
        final PurchaseOrderItem item;
        final String sku;
        final String name;
        String quantity;
        String unitPrice;
        String lineTotal;

        ItemRow(PurchaseOrderItem item) {
            this.item = item;
            this.sku = item.getProduct().getSku();
            this.name = item.getProduct().getName();
            this.quantity = String.valueOf(item.getQuantity());
            this.unitPrice = item.getUnitPrice().toPlainString();
            this.lineTotal = item.getLineTotal().toPlainString();
        }
    }
}
