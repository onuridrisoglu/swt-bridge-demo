package com.example.warehouse.swt.ui.suppliers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.example.warehouse.model.Supplier;
import com.example.warehouse.service.SupplierService;
import com.example.warehouse.swt.ui.util.Ui;

/**
 * Add / edit form for a supplier, a modal child shell of the main window.
 * The dialog saves the supplier itself and only closes once saving
 * succeeded; {@link #open()} tells the caller whether it did.
 */
public class SupplierDialog {

    private final Supplier existing;
    private final SupplierService service = new SupplierService();
    private final Shell shell;
    private boolean saved;

    private Text nameField;
    private Text contactField;
    private Text emailField;
    private Text phoneField;
    private Text addressArea;

    public SupplierDialog(Shell owner, Supplier supplier) {
        this.existing = supplier;
        shell = Ui.dialogShell(owner,
                supplier == null ? "Add New Supplier" : "Edit Supplier — " + supplier.getName(),
                true);
        GridLayout layout = Ui.grid(1, 14, 8);
        shell.setLayout(layout);

        Ui.panelTitle(shell, existing == null ? "New Supplier" : "Edit Supplier");
        buildForm();
        Ui.buttonBar(shell, "Save", () -> {
            if (doSave()) {
                shell.close();
            }
        });

        if (existing != null) {
            populateFields();
        }
        shell.setSize(shell.computeSize(450, SWT.DEFAULT));
    }

    /** Shows the dialog until it is closed; returns whether the supplier was saved. */
    public boolean open() {
        Ui.openModal(shell, nameField);
        return saved;
    }

    private void buildForm() {
        Group section = Ui.section(shell, "Supplier Information");
        section.setLayoutData(Ui.fill());
        GridLayout form = Ui.grid(2, 8, 10);
        form.horizontalSpacing = 12;
        section.setLayout(form);

        Ui.fieldLabel(section, "Company Name: *");
        nameField = Ui.textField(section, 24);
        Ui.fieldLabel(section, "Contact Person:");
        contactField = Ui.textField(section, 24);
        Ui.fieldLabel(section, "Email:");
        emailField = Ui.textField(section, 24);
        Ui.fieldLabel(section, "Phone:");
        phoneField = Ui.textField(section, 16);

        // Only the address row takes extra height when the dialog grows.
        Label addressLabel = Ui.fieldLabel(section, "Address:");
        addressLabel.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));
        addressArea = Ui.textArea(section, 3);
    }

    private void populateFields() {
        nameField.setText(existing.getName());
        contactField.setText(orEmpty(existing.getContactName()));
        emailField.setText(orEmpty(existing.getEmail()));
        phoneField.setText(orEmpty(existing.getPhone()));
        addressArea.setText(orEmpty(existing.getAddress()));
    }

    private static String orEmpty(String value) {
        return value != null ? value : "";
    }

    /** Validates and persists the form; returns whether the dialog may close. */
    private boolean doSave() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            Ui.warn(shell, "Validation", "Company name is required.");
            nameField.setFocus();
            return false;
        }
        Supplier s = existing != null ? existing : new Supplier();
        s.setName(name);
        s.setContactName(contactField.getText().trim());
        s.setEmail(emailField.getText().trim());
        s.setPhone(phoneField.getText().trim());
        s.setAddress(addressArea.getText().trim());
        try {
            if (existing == null) {
                service.save(s);
            } else {
                service.update(s);
            }
            saved = true;
            return true;
        } catch (Exception ex) {
            Ui.error(shell, "Error", "Failed to save:\n" + ex.getMessage());
            return false;
        }
    }
}
