package com.example.warehouse.interop;

/**
 * Told when the user asks to edit a supplier in the SWT suppliers table
 * (a double-click on a row), so a Vaadin view can open its own form.
 */
public interface SupplierEditRequestListener {
    void onEditRequested(SupplierEditRequestEvent event);
}
