package com.example.warehouse.interop;

import java.io.Serializable;

/**
 * The supplier now selected in the SWT suppliers table.
 * Serializable: the bridged application runs in its own process and sends
 * it to the Vaadin host.
 */
public class SupplierSelectedEvent implements Serializable {

    /** The selected supplier's id, or {@code null} when nothing is selected. */
    public final Long id;

    public SupplierSelectedEvent(Long id) {
        this.id = id;
    }
}
