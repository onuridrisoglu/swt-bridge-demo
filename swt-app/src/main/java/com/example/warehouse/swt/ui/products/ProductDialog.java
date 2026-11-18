package com.example.warehouse.swt.ui.products;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import com.example.warehouse.dao.CategoryDAO;
import com.example.warehouse.model.Category;
import com.example.warehouse.model.Product;
import com.example.warehouse.model.Supplier;
import com.example.warehouse.service.ProductService;
import com.example.warehouse.service.SupplierService;
import com.example.warehouse.swt.ui.util.ObjectCombo;
import com.example.warehouse.swt.ui.util.Ui;

/**
 * Add / edit form for a product, a modal child shell of the main window.
 * {@link #open()} returns whether the product was saved.
 */
public class ProductDialog {

    private static final String NO_SUPPLIER = "— None —";

    private final Product existing;
    private final ProductService productService = new ProductService();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final SupplierService supplierService = new SupplierService();

    private final Shell shell;
    private boolean saved = false;

    private Text skuField;
    private Text nameField;
    private ObjectCombo<Category> categoryBox;
    private ObjectCombo<Supplier> supplierBox;
    private Text descriptionArea;
    private Text priceField;
    private Spinner quantitySpinner;
    private Spinner reorderSpinner;
    private Text unitField;

    public ProductDialog(Shell owner, Product product) {
        this.existing = product;
        shell = Ui.dialogShell(owner,
                product == null ? "Add New Product" : "Edit Product — " + product.getSku(), true);
        GridLayout layout = Ui.grid(1, 16, 8);
        layout.marginTop = 12;
        shell.setLayout(layout);

        Ui.panelTitle(shell, existing == null ? "New Product" : "Edit Product");
        buildForm();
        Ui.buttonBar(shell, "Save", () -> {
            if (doSave()) {
                shell.close();
            }
        });

        if (existing != null) {
            populateFields();
        }
        // The JavaFX dialog pane's preferred size, as the content area
        Rectangle trim = shell.computeTrim(0, 0, 480, 480);
        shell.setSize(trim.width, trim.height);
    }

    /** Shows the dialog until it is closed; returns whether the product was saved. */
    public boolean open() {
        Ui.openModal(shell, skuField);
        return saved;
    }

    private void buildForm() {
        Group section = Ui.section(shell, "Product Details");
        section.setLayoutData(Ui.fill());
        GridLayout form = Ui.grid(2, 8, 8);
        form.horizontalSpacing = 12;
        section.setLayout(form);

        Ui.fieldLabel(section, "SKU: *");
        skuField = Ui.textField(section, 20);
        Ui.fieldLabel(section, "Name: *");
        nameField = Ui.textField(section, 20);

        Ui.fieldLabel(section, "Category: *");
        categoryBox = new ObjectCombo<>(section, Category::getName);
        categoryBox.setItems(categoryDAO.findAll());
        categoryBox.selectFirst();
        categoryBox.getControl().setLayoutData(Ui.fillHorizontal());

        Ui.fieldLabel(section, "Supplier:");
        supplierBox = new ObjectCombo<>(section, s -> s == null ? NO_SUPPLIER : s.getName());
        List<Supplier> suppliers = new ArrayList<>();
        suppliers.add(null); // "no supplier" entry
        suppliers.addAll(supplierService.findAll());
        supplierBox.setItems(suppliers);
        supplierBox.selectFirst();
        supplierBox.getControl().setLayoutData(Ui.fillHorizontal());

        Ui.fieldLabel(section, "Unit Price ($):*");
        priceField = Ui.textField(section, 20);
        Ui.fieldLabel(section, "Quantity:");
        quantitySpinner = spinner(section, 0);
        Ui.fieldLabel(section, "Reorder Level:");
        reorderSpinner = spinner(section, 5);
        Ui.fieldLabel(section, "Unit:");
        unitField = Ui.textField(section, 20);

        Label descLabel = Ui.fieldLabel(section, "Description:");
        descLabel.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));
        descriptionArea = Ui.textArea(section, 3);
    }

    private static Spinner spinner(Group parent, int value) {
        Spinner spinner = new Spinner(parent, SWT.BORDER);
        spinner.setValues(value, 0, 999999, 0, 1, 10);
        spinner.setLayoutData(Ui.fillHorizontal());
        return spinner;
    }

    private void populateFields() {
        skuField.setText(existing.getSku());
        nameField.setText(existing.getName());
        priceField.setText(existing.getUnitPrice().toPlainString());
        quantitySpinner.setSelection(existing.getQuantity());
        reorderSpinner.setSelection(existing.getReorderLevel());
        unitField.setText(existing.getUnit() != null ? existing.getUnit() : "");
        descriptionArea.setText(existing.getDescription() != null ? existing.getDescription() : "");

        categoryBox.selectFirstMatch(c -> c.getId().equals(existing.getCategory().getId()));
        if (existing.getSupplier() != null) {
            supplierBox.selectFirstMatch(
                    s -> s != null && s.getId().equals(existing.getSupplier().getId()));
        }
    }

    /** Validates and persists the form; returns whether the dialog may close. */
    private boolean doSave() {
        String sku = skuField.getText().trim();
        String name = nameField.getText().trim();
        String priceText = priceField.getText().trim();

        if (sku.isEmpty() || name.isEmpty() || priceText.isEmpty()) {
            Ui.warn(shell, "Validation Error", "SKU, Name, and Unit Price are required.");
            return false;
        }

        BigDecimal price;
        try {
            price = new BigDecimal(priceText);
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            Ui.warn(shell, "Validation Error", "Unit Price must be a valid positive number.");
            return false;
        }

        Product p = existing != null ? existing : new Product();
        p.setSku(sku);
        p.setName(name);
        p.setCategory(categoryBox.getValue());
        p.setSupplier(supplierBox.getValue());
        p.setUnitPrice(price);
        p.setQuantity(quantitySpinner.getSelection());
        p.setReorderLevel(reorderSpinner.getSelection());
        p.setUnit(unitField.getText().trim());
        p.setDescription(descriptionArea.getText().trim());

        try {
            if (existing == null) {
                productService.save(p);
            } else {
                productService.update(p);
            }
            saved = true;
            return true;
        } catch (Exception ex) {
            Ui.error(shell, "Error", "Failed to save product:\n" + ex.getMessage());
            return false;
        }
    }
}
