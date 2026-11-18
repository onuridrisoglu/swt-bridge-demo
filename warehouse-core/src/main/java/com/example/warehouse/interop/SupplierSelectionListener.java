package com.example.warehouse.interop;

/**
 * Told when the supplier selection in the SWT suppliers table changes.
 * Shared by the SWT application and the Vaadin host (a Vaadin view
 * implements it through {@code @VaadinCallback}), so it lives here: both
 * have warehouse-core on their class path, and the event crosses from the
 * application's process to the host's.
 */
public interface SupplierSelectionListener {
    void onSelectionChanged(SupplierSelectedEvent event);
}
