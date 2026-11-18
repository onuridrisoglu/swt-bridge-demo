package com.example.warehouse.interop;

import java.io.Serializable;

/**
 * The supplier the user asked to edit in the SWT suppliers table.
 * Serializable: the bridged application runs in its own process and sends
 * it to the Vaadin host.
 */
public class SupplierEditRequestEvent implements Serializable {

    /** The supplier's id. */
    public final Long id;

    public SupplierEditRequestEvent(Long id) {
        this.id = id;
    }
}
