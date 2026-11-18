package com.example.views;

import com.example.warehouse.swt.bridge.BridgeAppBridge;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "products", layout = MainLayout.class)
@PageTitle("Products")
public class ProductsView extends BridgedPanelView {

    public ProductsView() {
        super(BridgeAppBridge::showProducts);
    }
}
