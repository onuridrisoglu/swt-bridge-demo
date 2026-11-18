package com.example.warehouse.swt.ui.suppliers;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

import com.example.warehouse.interop.SupplierEditRequestEvent;
import com.example.warehouse.interop.SupplierEditRequestListener;
import com.example.warehouse.interop.SupplierSelectedEvent;
import com.example.warehouse.interop.SupplierSelectionListener;
import com.example.warehouse.model.Supplier;
import com.example.warehouse.service.SupplierService;
import com.example.warehouse.swt.ui.util.DataTable;
import com.example.warehouse.swt.ui.util.StatusBar;
import com.example.warehouse.swt.ui.util.TableExporter;
import com.example.warehouse.swt.ui.util.Toolbar;
import com.example.warehouse.swt.ui.util.Ui;

/** The Suppliers tab: a searchable supplier table with add / edit / delete. */
public class SuppliersPanel extends Composite {

    private final SupplierService service = new SupplierService();
    private final StatusBar statusBar;
    private DataTable<Supplier> table;
    private Composite toolbar;
    private SupplierSelectionListener selectionListener;
    private SupplierEditRequestListener editRequestListener;

    private Button editBtn;
    private Button deleteBtn;

    public SuppliersPanel(Composite parent, StatusBar statusBar) {
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
        toolbar.button("+ Add Supplier", this::openAddDialog);
        toolbar.separator();
        editBtn = toolbar.button("Edit", this::openEditDialog);
        deleteBtn = toolbar.button("Delete", this::deleteSelected);
        toolbar.separator();
        toolbar.button("↺ Refresh", this::refresh);
        toolbar.separator();
        toolbar.button("⬇ Export CSV",
                () -> TableExporter.exportToCsv(table, "suppliers.csv", getShell()));
        toolbar.fill();
        toolbar.label("Search:");
        toolbar.searchField(this::filterTable);
        this.toolbar = toolbar.done();
        editBtn.setEnabled(false);
        deleteBtn.setEnabled(false);
    }

    private void buildTable() {
        table = new DataTable<>(this);
        table.column("ID", 40, Supplier::getId);
        table.column("Supplier Name", 200, Supplier::getName);
        table.column("Contact Person", 140, Supplier::getContactName);
        table.column("Email", 180, Supplier::getEmail);
        table.column("Phone", 120, Supplier::getPhone);
        table.column("Address", 280, Supplier::getAddress);

        table.onSelectionChanged(() -> {
            Supplier selected = table.getSelected();
            editBtn.setEnabled(selected != null);
            deleteBtn.setEnabled(selected != null);
            if (selectionListener != null) {
                selectionListener.onSelectionChanged(
                        new SupplierSelectedEvent(selected == null ? null : selected.getId()));
            }
        });
        table.onOpen(s -> {
            if (editRequestListener != null) {
                // Driven from Vaadin: its form edits the supplier.
                editRequestListener.onEditRequested(new SupplierEditRequestEvent(s.getId()));
            } else {
                openEditDialog();
            }
        });

        Menu menu = table.contextMenu();
        DataTable.menuItem(menu, "Edit Supplier", this::openEditDialog);
        DataTable.menuItem(menu, "Delete Supplier", this::deleteSelected);
    }

    /** Shows or hides the toolbar, for a Vaadin toolbar to take its place. */
    public void setToolbarVisible(boolean visible) {
        ((GridData) toolbar.getLayoutData()).exclude = !visible;
        toolbar.setVisible(visible);
        layout(true, true);
    }

    /** Tells {@code listener} about selection changes; {@code null} to stop. */
    public void setSelectionListener(SupplierSelectionListener listener) {
        this.selectionListener = listener;
    }

    /**
     * Sends double-clicks to {@code listener} instead of opening the edit
     * dialog; {@code null} to open the dialog again.
     */
    public void setEditRequestListener(SupplierEditRequestListener listener) {
        this.editRequestListener = listener;
    }

    /**
     * Shows only rows where any column matches the text, as a case-insensitive
     * regular expression (like Swing's {@code RowFilter.regexFilter}).
     */
    public void filterTable(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            table.setFilter(null);
            return;
        }
        Pattern pattern;
        try {
            pattern = Pattern.compile(trimmed, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException ex) {
            pattern = Pattern.compile(Pattern.quote(trimmed), Pattern.CASE_INSENSITIVE);
        }
        Pattern p = pattern;
        table.setFilter(s -> table.anyColumnMatches(s, v -> p.matcher(v).find()));
    }

    public void refresh() {
        table.setItems(service.findAll());
        statusBar.setRowCount(table.getItems().size(), "supplier");
    }

    private void openAddDialog() {
        if (new SupplierDialog(getShell(), null).open()) {
            refresh();
        }
    }

    private void openEditDialog() {
        Supplier s = table.getSelected();
        if (s == null) {
            return;
        }
        if (new SupplierDialog(getShell(), s).open()) {
            refresh();
        }
    }

    private void deleteSelected() {
        Supplier s = table.getSelected();
        if (s == null) {
            return;
        }
        if (!Ui.confirm(getShell(), "Confirm Delete",
                "Delete supplier '" + s.getName() + "'?\n\nThis cannot be undone.")) {
            return;
        }
        try {
            service.delete(s.getId());
            refresh();
            statusBar.setMessage("Supplier '" + s.getName() + "' deleted.");
        } catch (Exception ex) {
            Ui.error(getShell(), "Delete Error",
                    "Cannot delete: supplier may be referenced by products or orders.");
        }
    }
}
