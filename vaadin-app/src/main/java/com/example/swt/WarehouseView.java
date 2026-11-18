package com.example.swt;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.swingbridge.swt.SwtBridge;

/**
 * Stage 1: the whole SWT application, its own login window included, in the
 * browser without a single change.
 */
@Route("")
@PageTitle("WarehousePro 3.5")
public class WarehouseView extends VerticalLayout {

    public WarehouseView() {
        setSizeFull();
        setPadding(false);
        SwtBridge bridge = new SwtBridge(
                "com.example.warehouse.swt.WarehouseApp",
                AppLibsClassLoader.fromAppLibs());
        add(bridge);
        expand(bridge);
    }
}
