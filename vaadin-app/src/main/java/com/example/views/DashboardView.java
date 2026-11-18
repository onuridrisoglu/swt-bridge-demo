package com.example.views;

import com.example.warehouse.swt.bridge.BridgeAppBridge;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "dashboard", layout = MainLayout.class)
@PageTitle("Dashboard")
public class DashboardView extends BridgedPanelView {

    public DashboardView() {
        super(BridgeAppBridge::showDashboard);
    }
}
