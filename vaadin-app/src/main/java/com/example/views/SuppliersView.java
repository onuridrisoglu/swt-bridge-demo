package com.example.views;

import java.io.ByteArrayInputStream;

import com.example.warehouse.swt.bridge.BridgeAppBridge;
import com.example.warehouse.interop.SupplierEditRequestEvent;
import com.example.warehouse.interop.SupplierEditRequestListener;
import com.example.warehouse.interop.SupplierSelectedEvent;
import com.example.warehouse.interop.SupplierSelectionListener;
import com.example.warehouse.model.Supplier;
import com.example.warehouse.service.SupplierService;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.shared.Registration;
import com.vaadin.swingbridge.interop.VaadinCallback;
import com.vaadin.swingbridge.swt.SwtBridge;
import com.vaadin.swingbridge.swt.interop.SwtBridgeHandle;

/**
 * Stage 3: a native Vaadin toolbar and dialogs driving the SWT suppliers
 * table, both ways. Vaadin calls {@code BridgeApp}'s exposed methods (show the
 * bare table, refresh, filter) through the generated {@link BridgeAppBridge};
 * the table's selection and double-clicks reach this view's
 * {@link VaadinCallback} methods. Add, edit, delete and CSV export run in
 * Vaadin against the same database. The SWT counterpart of the Swing demo's
 * {@code SuppliersView}.
 */
@Route(value = "suppliers", layout = MainLayout.class)
@PageTitle("Suppliers")
public class SuppliersView extends VerticalLayout {

    private final SupplierService service = new SupplierService();
    private final SwtBridgeHandle<BridgeAppBridge> app = SwtBridge
            .interop(BridgedPanelView.BRIDGE_APP).of(BridgeAppBridge.class);

    private final Button editBtn = new Button("Edit");
    private final Button deleteBtn = new Button("Delete");

    private Long selectedId;
    private Registration callbackRegistration;

    public SuppliersView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // --- Vaadin toolbar ---
        Button addBtn = new Button("+ Add Supplier");
        Button refreshBtn = new Button("↺ Refresh");
        Button exportBtn = new Button("⬇ Export CSV");
        Anchor exportAnchor = new Anchor(DownloadHandler.fromInputStream(e -> {
            byte[] csv = service.exportToCsv();
            return new DownloadResponse(new ByteArrayInputStream(csv),
                    "suppliers.csv", "text/csv", csv.length);
        }), "");
        exportAnchor.add(exportBtn);

        addBtn.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.SMALL);
        editBtn.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.SMALL);
        deleteBtn.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.PRIMARY,
                ButtonVariant.SMALL);
        refreshBtn.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
        exportBtn.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
        addBtn.setId("add-supplier");
        editBtn.setId("edit-supplier");
        deleteBtn.setId("delete-supplier");
        refreshBtn.setId("refresh-suppliers");
        editBtn.setEnabled(false);
        deleteBtn.setEnabled(false);

        TextField searchField = new TextField();
        searchField.setId("supplier-search");
        searchField.setPlaceholder("Search...");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.EAGER);

        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.addToStart(addBtn, editBtn, deleteBtn, refreshBtn, exportAnchor);
        toolbar.addToEnd(searchField);
        toolbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.setWidthFull();
        toolbar.getStyle().set("padding", "8px 12px").set("box-sizing",
                "border-box");

        // --- The SWT application, the same copy as the other routes ---
        SwtBridge bridge = BridgedPanelView.bridge();
        bridge.setSizeFull();
        app.onReady(BridgeAppBridge::showSuppliersTable);

        addBtn.addClickListener(e -> openForm(null));
        editBtn.addClickListener(e -> {
            if (selectedId != null) {
                openForm(selectedId);
            }
        });
        deleteBtn.addClickListener(e -> confirmDelete());
        refreshBtn.addClickListener(
                e -> app.onReady(BridgeAppBridge::refreshSuppliers));
        searchField.addValueChangeListener(
                e -> app.onReady(b -> b.setSupplierFilter(e.getValue())));

        add(toolbar, bridge);
        setFlexGrow(1, bridge);
    }

    @Override
    protected void onAttach(AttachEvent e) {
        super.onAttach(e);
        callbackRegistration = app.registerCallback(this);
    }

    @VaadinCallback(observerFor = SupplierSelectionListener.class)
    public void onSelectionChanged(SupplierSelectedEvent event) {
        selectedId = event.id;
        editBtn.setEnabled(selectedId != null);
        deleteBtn.setEnabled(selectedId != null);
    }

    @VaadinCallback(observerFor = SupplierEditRequestListener.class)
    public void onEditRequested(SupplierEditRequestEvent event) {
        openForm(event.id);
    }

    @Override
    protected void onDetach(DetachEvent e) {
        if (callbackRegistration != null) {
            callbackRegistration.remove();
            callbackRegistration = null;
        }
        // The setters return no remover, so unhook them here: the other
        // routes show the same running application, with its own toolbar and
        // dialogs again.
        app.onReady(b -> {
            b.setSupplierSelectionListener(null);
            b.setSupplierEditRequestListener(null);
        });
        super.onDetach(e);
    }

    private void openForm(Long existingId) {
        Supplier existing = existingId != null ? service.findById(existingId)
                : null;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existingId == null ? "Add Supplier" : "Edit Supplier");
        dialog.setWidth("420px");

        TextField nameField = new TextField("Supplier Name");
        TextField contactField = new TextField("Contact Person");
        TextField emailField = new TextField("Email");
        TextField phoneField = new TextField("Phone");
        TextArea addressField = new TextArea("Address");
        nameField.setId("supplier-name");
        nameField.setRequired(true);
        addressField.setMaxHeight("80px");

        if (existing != null) {
            nameField.setValue(nonNull(existing.getName()));
            contactField.setValue(nonNull(existing.getContactName()));
            emailField.setValue(nonNull(existing.getEmail()));
            phoneField.setValue(nonNull(existing.getPhone()));
            addressField.setValue(nonNull(existing.getAddress()));
        }

        FormLayout form = new FormLayout(nameField, contactField, emailField,
                phoneField, addressField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        dialog.add(form);

        Button cancelBtn = new Button("Cancel", ev -> dialog.close());
        Button saveBtn = new Button("Save", ev -> {
            if (nameField.isEmpty()) {
                notify("Supplier name is required.", true);
                return;
            }
            if (existing == null) {
                service.save(new Supplier(nameField.getValue(),
                        contactField.getValue(), emailField.getValue(),
                        phoneField.getValue(), addressField.getValue()));
                notify("Supplier added.", false);
            } else {
                existing.setName(nameField.getValue());
                existing.setContactName(contactField.getValue());
                existing.setEmail(emailField.getValue());
                existing.setPhone(phoneField.getValue());
                existing.setAddress(addressField.getValue());
                service.update(existing);
                notify("Supplier updated.", false);
            }
            dialog.close();
            app.onReady(BridgeAppBridge::refreshSuppliers);
        });
        saveBtn.setId("save-supplier");
        saveBtn.addThemeVariants(ButtonVariant.PRIMARY);

        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void confirmDelete() {
        if (selectedId == null) {
            return;
        }
        Supplier s = service.findById(selectedId);
        if (s == null) {
            return;
        }
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Delete Supplier");
        confirm.setText("Delete '" + s.getName() + "'? This cannot be undone.");
        confirm.setCancelable(true);
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(ev -> {
            try {
                service.delete(s.getId());
                selectedId = null;
                editBtn.setEnabled(false);
                deleteBtn.setEnabled(false);
                app.onReady(BridgeAppBridge::refreshSuppliers);
                notify("Supplier deleted.", false);
            } catch (Exception ex) {
                notify("Cannot delete: supplier may be referenced by products"
                        + " or orders.", true);
            }
        });
        confirm.open();
    }

    private static String nonNull(String value) {
        return value != null ? value : "";
    }

    private static void notify(String message, boolean error) {
        Notification n = Notification.show(message, 3000,
                Notification.Position.BOTTOM_START);
        n.addThemeVariants(error ? NotificationVariant.ERROR
                : NotificationVariant.SUCCESS);
    }
}
