package com.example.views;

import com.example.warehouse.swt.bridge.BridgeAppBridge;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "stock", layout = MainLayout.class)
@PageTitle("Stock")
public class StockView extends BridgedPanelView {

    public StockView() {
        super(BridgeAppBridge::showStock);
    }
}
