package com.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * Walks the migration stages in the running app (start-vaadin.bat), saving
 * a screenshot of each step under target/it-screenshots:
 * <ul>
 * <li>stage 1: the whole SWT application with its own login window;</li>
 * <li>stage 2: the Vaadin login and shell, each route showing a module of the
 * one SWT application of the user;</li>
 * <li>stage 3: the Vaadin toolbar and form driving the SWT suppliers table, and
 * the table's selection and double-clicks reaching Vaadin;</li>
 * <li>stage 4: the dashboard rebuilt with Vaadin components.</li>
 * </ul>
 */
class WarehouseIT {

    private static final String URL = System.getProperty("it.url",
            "http://localhost:8890/");
    private static final Path SHOTS = Path.of("target", "it-screenshots");
    /** A row of the SWT suppliers table, without its toolbar, in canvas pixels. */
    private static final double ROW_X = 150;
    private static final double FIRST_ROW_Y = 52;

    private static Playwright playwright;
    private static Browser browser;

    @BeforeAll
    static void start() {
        playwright = Playwright.create();
        browser = playwright.chromium()
                .launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void stop() {
        browser.close();
        playwright.close();
    }

    @Test
    void stage1RunsTheWholeApplication() {
        Page page = browser.newContext().newPage();
        page.setViewportSize(1400, 900);
        page.navigate(URL);
        waitForFirstFrame(page);
        shot(page, "stage1-1-login");
        page.keyboard().type("admin");
        page.keyboard().press("Tab");
        // In case the password field's text is not selected on focus.
        page.keyboard().press("Control+A");
        page.keyboard().type("admin");
        page.keyboard().press("Enter");
        page.waitForTimeout(4000);
        shot(page, "stage1-2-main");
        page.context().close();
    }

    @Test
    void stages2To4ShareTheApplicationAndDatabase() {
        Page page = browser.newContext().newPage();
        page.setViewportSize(1400, 900);
        signIn(page);

        // Stage 2: every route shows a module of the same application.
        waitForFirstFrame(page);
        page.waitForTimeout(1500);
        byte[] dashboard = canvasShot(page, "stage2-1-dashboard");
        navigate(page, "Products", "products");
        byte[] products = canvasShot(page, "stage2-2-products");
        assertNotEquals(dashboard.length, products.length,
                "Products must show another module than the dashboard");
        navigate(page, "Orders", "orders");
        canvasShot(page, "stage2-3-orders");
        navigate(page, "Stock", "stock");
        canvasShot(page, "stage2-4-stock");

        // Stage 4: the dashboard rebuilt natively, on the same database.
        page.locator("vaadin-side-nav-item").filter(new Locator.FilterOptions()
                .setHasText("Modern")).first().click();
        page.waitForURL("**/dashboard-modern");
        page.locator("vaadin-dashboard").waitFor();
        page.waitForTimeout(1000);
        shot(page, "stage4-dashboard-modern");

        // Stage 3: the Vaadin toolbar over the bare SWT suppliers table.
        navigate(page, "Suppliers", "suppliers");
        Locator canvas = page.locator(".swt-bridge canvas");
        Locator edit = page.locator("#edit-supplier");
        shot(page, "stage3-1-suppliers");
        assertTrue(isDisabled(edit), "Nothing is selected yet");

        // SWT -> Vaadin: selecting a row enables Edit and Delete.
        canvas.click(new Locator.ClickOptions().setPosition(ROW_X,
                FIRST_ROW_Y));
        waitUntilEnabled(page, "#edit-supplier");
        assertFalse(isDisabled(page.locator("#delete-supplier")));
        shot(page, "stage3-2-selected");

        // SWT -> Vaadin: a double-click opens the Vaadin form, filled in.
        canvas.dblclick(new Locator.DblclickOptions().setPosition(ROW_X,
                FIRST_ROW_Y));
        Locator name = page.locator("#supplier-name input");
        name.waitFor();
        String original = name.inputValue();
        assertFalse(original.isBlank(), "The form shows the supplier");
        shot(page, "stage3-3-edit-form");

        // Vaadin -> SWT: save a new name, which refreshes the SWT table.
        String renamed = "Zeta Interop " + System.currentTimeMillis() % 100000;
        name.fill(renamed);
        page.locator("#save-supplier").click();
        name.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.DETACHED));
        page.waitForTimeout(1000);

        // Vaadin -> SWT: filter the table by the new name. Only the renamed
        // row is left, and only if the SWT table reloaded it.
        page.locator("#supplier-search input").fill(renamed);
        page.waitForTimeout(1500);
        canvas.click(new Locator.ClickOptions().setPosition(ROW_X,
                FIRST_ROW_Y));
        waitUntilEnabled(page, "#edit-supplier");
        shot(page, "stage3-4-filtered");
        canvas.dblclick(new Locator.DblclickOptions().setPosition(ROW_X,
                FIRST_ROW_Y));
        name.waitFor();
        assertEquals(renamed, name.inputValue());

        // Put the name back, for the next run.
        name.fill(original);
        page.locator("#save-supplier").click();
        name.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.DETACHED));
        page.locator("#supplier-search input").fill("");
        page.waitForTimeout(1500);
        shot(page, "stage3-5-restored");

        // Leaving and coming back keeps the one application, wired again.
        navigate(page, "Products", "products");
        navigate(page, "Suppliers", "suppliers");
        canvas = page.locator(".swt-bridge canvas");
        canvas.click(new Locator.ClickOptions().setPosition(ROW_X,
                FIRST_ROW_Y));
        waitUntilEnabled(page, "#edit-supplier");
        shot(page, "stage3-6-back-again");
        page.context().close();
    }

    private static void signIn(Page page) {
        page.navigate(URL + "login");
        page.locator("vaadin-text-field input").fill("admin");
        page.locator("vaadin-password-field input").fill("admin");
        page.locator("vaadin-button.login-btn").click();
        page.waitForURL("**/dashboard");
    }

    private static void navigate(Page page, String item, String route) {
        page.locator("vaadin-side-nav-item").filter(
                new Locator.FilterOptions().setHasText(item)).first().click();
        page.waitForURL("**/" + route);
        waitForFirstFrame(page);
        page.waitForTimeout(1500);
    }

    /** The bridge hides its status once the first frame is shown. */
    private static void waitForFirstFrame(Page page) {
        page.locator(".swt-bridge canvas").waitFor();
        page.waitForFunction("() => {"
                + " const s = document.querySelector('.swt-bridge > div');"
                + " return s && s.hidden; }", null,
                new Page.WaitForFunctionOptions().setTimeout(90_000));
    }

    private static boolean isDisabled(Locator button) {
        return button.getAttribute("disabled") != null;
    }

    private static void waitUntilEnabled(Page page, String selector) {
        page.waitForFunction("s => !document.querySelector(s)"
                + ".hasAttribute('disabled')", selector,
                new Page.WaitForFunctionOptions().setTimeout(10_000));
    }

    private static void shot(Page page, String name) {
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(SHOTS.resolve(name + ".png")));
    }

    private static byte[] canvasShot(Page page, String name) {
        shot(page, name);
        return page.locator(".swt-bridge canvas").screenshot();
    }
}
