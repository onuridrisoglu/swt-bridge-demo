package com.example.views;

import java.util.function.Consumer;

import com.example.swt.AppLibsClassLoader;
import com.example.warehouse.model.User;
import com.example.warehouse.swt.bridge.BridgeAppBridge;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.swingbridge.swt.SwtBridge;
import com.vaadin.swingbridge.swt.interop.SwtBridgeHandle;

/**
 * Stage 2: one SWT module inside the native Vaadin shell. Every route shows
 * the same running application, {@code BridgeApp} in bridge mode (one copy
 * per user), and tells it which module to show through interop, so the user's
 * state in the SWT application survives moving between routes. Like the Swing
 * demo's {@code MainFrameBridge::showDashboard}.
 */
abstract class BridgedPanelView extends Div {

    static final String BRIDGE_APP = "com.example.warehouse.swt.bridge.BridgeApp";

    /** Typed calls into the session's {@code BridgeApp}. */
    protected final SwtBridgeHandle<BridgeAppBridge> app = SwtBridge
            .interop(BRIDGE_APP).of(BridgeAppBridge.class);

    /**
     * @param module
     *            selects this route's module, e.g.
     *            {@code BridgeAppBridge::showProducts}
     */
    BridgedPanelView(Consumer<BridgeAppBridge> module) {
        setSizeFull();
        addClassName("bridged-panel");
        add(bridge());
        // Runs once the application is up, or at once when it already is.
        app.onReady(module);
    }

    /**
     * The session's {@code BridgeApp}, signed in as the user of the Vaadin
     * login. Every route passes the same arguments, so they share one copy of
     * the application.
     */
    static SwtBridge bridge() {
        User user = (User) VaadinSession.getCurrent().getAttribute("user");
        return user == null
                ? new SwtBridge(BRIDGE_APP, AppLibsClassLoader.fromAppLibs())
                : new SwtBridge(BRIDGE_APP, AppLibsClassLoader.fromAppLibs(),
                        "--user=" + user.getUsername());
    }
}
