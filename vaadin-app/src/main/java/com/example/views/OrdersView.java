package com.example.views;

import com.example.warehouse.swt.bridge.BridgeAppBridge;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "orders", layout = MainLayout.class)
@PageTitle("Orders")
public class OrdersView extends BridgedPanelView {

    public OrdersView() {
        super(BridgeAppBridge::showOrders);
    }
}
