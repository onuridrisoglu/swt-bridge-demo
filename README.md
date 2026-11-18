# WarehousePro Demo — SWT edition

The SWT counterpart of the [JavaFX edition](https://github.com/onuridrisoglu/javafx-bridge-demo):
the same legacy desktop app, WarehousePro 3.5, written in plain SWT (no
JFace, no Eclipse RCP, no Nebula), and a Vaadin application that brings it to
the browser through the [SWT extension for SwingBridge](https://github.com/vaadin-component-factory/swing-bridge-swt-addon)
(an alpha). The app follows a few strict window conventions the
bridge relies on (see below).

## The story

1. **The desktop app, as it is.** `swt-app` is WarehousePro 3.5 in SWT:
   login, a main window with menu, title and status bars, and five modules
   (Dashboard, Products, Suppliers, Orders, Stock Log). It runs on the desktop
   with `./start-swt.sh`.
2. **Stage 1 — the whole app in the browser, unchanged.** `/` in the Vaadin
   app runs the same application jar through `SwtBridge`, its own login
   window included.
3. **Stage 2 — a Vaadin shell around SWT modules.** `/login` is a native
   Vaadin login; after it, an Aura `AppLayout` with a side navigation hosts
   the SWT modules (`/dashboard`, `/products`, `/orders`, `/stock`). Every
   route shows the same running application, `swt.bridge.BridgeApp` in
   *bridge mode* (no menu, title bar or tabs; one copy per user, signed in
   as the Vaadin user), and tells it which module to show through interop:
   `SwtBridge.interop(…).of(BridgeAppBridge.class)
   .onReady(BridgeAppBridge::showProducts)`. Moving between routes keeps the
   user's state in the SWT application.
4. **Stage 3 — interoperability (`/suppliers`).** A native Vaadin toolbar and
   dialogs drive the SWT suppliers table, both ways (below).
5. **Stage 4 — native Vaadin rebuild (`/dashboard-modern`).** The same
   dashboard rebuilt natively with Vaadin components and charts, reading the
   same database.

## Stage 3 — interoperability

`BridgeApp`'s methods marked `@ExposedMethod` are scanned at build time by
SwingBridge's `swing-bridge-codegen-maven-plugin`, which generates the typed
`BridgeAppBridge` interface (in `vaadin-app/target/generated-sources/swing-bridge`,
not checked in). `@InstanceProvider BridgeApp.current()` gives the bridge the
running instance; each user has their own copy of the application, so one per
user.

**Vaadin → SWT (method calls)**

| Vaadin action | SWT method called |
|---|---|
| View opens | `showSuppliersTable()` (suppliers module, its own toolbar hidden) |
| Refresh button | `refreshSuppliers()` |
| Search field | `setSupplierFilter(String)` |
| After save / delete | `refreshSuppliers()` |

**Export CSV** runs entirely in Vaadin: `SuppliersView` reads the suppliers
through `SupplierService` and serves the file with a `DownloadHandler`.

**SWT → Vaadin (callbacks)**

`SuppliersView`'s `@VaadinCallback(observerFor = …Listener.class)` methods are
registered with the application in `onAttach`; `onDetach` stops them and
unhooks the listeners in the application (setters with `null`).

| User action in the SWT table | Event | Vaadin reaction |
|---|---|---|
| Row selected | `SupplierSelectedEvent` (with id) | Enable Edit + Delete, remember the selection |
| Row deselected | `SupplierSelectedEvent` (id = null) | Disable Edit + Delete |
| Double-click a row | `SupplierEditRequestEvent` | Open the Vaadin edit form, filled in |

The listener and event types live in `warehouse-core`
(`com.example.warehouse.interop`), which both the host and the application
have on their class path. The application runs in the same JVM, and
`AppLibsClassLoader` loads these types from the host for both sides, so the
views receive the application's events as they are.

**What to point out:** the Vaadin toolbar and the SWT table react to each
other in real time — selecting a row enables the Vaadin buttons, an edit saved
in the Vaadin dialog shows up in the SWT table, and the search field filters
the SWT table as you type.

## Modules

| Module | What |
|---|---|
| `warehouse-core` | Domain model (JPA entities), DAOs, services, seed data, `AppSession` and the interop types. Hibernate 6.4 on an embedded H2 file database (`./warehousedb` in the working directory, or `-Dwarehouse.db=<path>`; `AUTO_SERVER`, so the desktop app and a running Vaadin app can share it). No UI. |
| `swt-app` | The SWT desktop app, `com.example.warehouse.swt.WarehouseApp`, and `swt.bridge.BridgeApp`, the bridge-mode main window with the `@ExposedMethod` methods stages 2 and 3 call. Shaded into one jar that contains `warehouse-core`, Hibernate, H2 and slf4j-simple, but **not SWT**: SWT is a `provided` dependency, supplied by whoever runs the jar. |
| `vaadin-app` | Spring Boot 4.1 + Vaadin 25.3 host with the Aura theme, SwingBridge `1.4.0-alpha1` and the SWT extension, on port 8890. Runs on Windows (`start-vaadin.bat`), every user's SWT application in its JVM. Playwright ITs walk all four stages. |

SWT is `org.eclipse.platform:org.eclipse.swt.${swt.platform}:${swt.version}`
(3.135.0). OS-activated profiles in the parent POM pick `swt.platform`:
`cocoa.macosx.aarch64`, `cocoa.macosx.x86_64`, `gtk.linux.x86_64`,
`gtk.linux.aarch64` or `win32.win32.x86_64`. The dependency excludes all
transitive dependencies: the platform fragment's POM points back at the
`org.eclipse.swt` host bundle, whose own POM is not needed.

## Building and running

Prerequisites: JDK 21+ and Maven 3.9.

```bash
mvn install -DskipTests                 # builds swt-app/target/warehouse-swt-app-3.5.0.jar
./start-swt.sh                          # builds, then runs the desktop app
MVN=/path/to/mvn ./start-swt.sh         # use a specific Maven
./start-swt.sh -Dwarehouse.autologin=admin   # extra JVM options are passed through
mvn -pl swt-app exec:exec               # alternative (after mvn install); runs in swt-app/
```

`start-swt.sh` resolves the SWT jar with `mvn dependency:build-classpath`
and runs

```bash
java --enable-native-access=ALL-UNNAMED -XstartOnFirstThread \
     -cp swt-app/target/warehouse-swt-app-3.5.0.jar:<org.eclipse.swt.cocoa.macosx.aarch64-3.135.0.jar> \
     com.example.warehouse.swt.WarehouseApp
```

`-XstartOnFirstThread` is added on macOS only (Cocoa requires the SWT
`Display` on the process's first thread).

Credentials: `admin / admin`, `manager / manager`, `viewer / viewer`.

The hidden system property `-Dwarehouse.autologin=<user>` signs in with the
seed credentials (`<user> / <user>`) and skips the login window.

## Window conventions

A web bridge will host this app and relies on these:

1. Exactly one `Display`, created on the main thread in `WarehouseApp.main`,
   which runs the standard `readAndDispatch` / `sleep` loop and disposes the
   display at the end.
2. The login window is a parentless `Shell` with `SWT.DIALOG_TRIM` (not
   resizable), centered on the primary monitor.
3. The main window is a parentless `Shell` with `SWT.SHELL_TRIM`, 1280x800
   by default (minimum 900x600), centered. It is the only resizable
   parentless shell.
4. All dialogs are modal child shells of the main window
   (`DIALOG_TRIM | APPLICATION_MODAL | RESIZE`), centered on their parent.
   The "Add Line Item" chooser and message boxes opened from a dialog are
   children of that dialog.
5. Closing the main window or File > Exit ends the loop; `main` shuts the
   database down, disposes the display and returns. Logout closes the main window and shows the login window again.
6. No system tray. The shell has an `SWT.BAR` menu (File, View, Help), but
   the module tabs inside the window are the primary navigation; the View
   menu only mirrors them.

## What was ported

Everything in the JavaFX `javafx-app`, screen by screen:

- **Login** (`ui/LoginView`): banner, Sign In form prefilled with
  `admin / admin`, inline error messages, Enter in the user name moves to
  the password, Enter signs in, credentials hint.
- **Main window** (`ui/MainView`): menu bar (File: Logout, Exit with
  confirmation; View: the five modules; Help: About), gradient title bar
  with the user, role and a Logout button, a `TabFolder` with the five
  modules, and the status bar.
- **Dashboard** (`ui/dashboard`): four KPI cards (products, inventory value,
  pending orders, low-stock alerts, with value colors), "Stock Quantity by
  Category" bar chart and "Purchase Orders by Status" pie chart (both drawn
  on a `Canvas` with `GC`: axes, ticks, grid, labels, legend, status colors),
  and the low-stock alert table (critical rows red, low rows yellow).
- **Products** (`ui/products`): toolbar (Add, Edit, Delete, Stock Adjust,
  Refresh, Export CSV, Search), sortable table with centered numeric columns,
  colored Status column, row tints, double-click to edit, row context menu,
  product dialog (validation, category / supplier drop-downs with
  "— None —", spinners) and the stock adjustment dialog.
- **Suppliers** (`ui/suppliers`): toolbar, sortable table, regex search,
  double-click / context menu, supplier dialog.
- **Orders** (`ui/orders`): orders table with status colors and row tints
  above a line-items table (`SashForm`), Edit / View, Delete, Export; the
  order dialog with supplier, dates (`yyyy-MM-dd`), status and notes, and an
  editable line-items table (click a Quantity or Unit Price cell; Enter
  commits, Esc cancels), Add Item chooser, Remove and the running total.
  Added and removed line items are saved on existing orders, as in JavaFX.
- **Stock Log** (`ui/stock`): read-only, regex-searchable log with type
  colors and IN / OUT row tints.
- **Utilities** (`ui/util`): `Ui` (palette, cached fonts and colors disposed
  with the display, form helpers, message boxes, modal dialog loop),
  `DataTable` (object-bound `Table`: header-click sorting, filtering,
  striping, row tints, last column fills the width), `ObjectCombo`,
  `Toolbar`, `StatusBar` (message, user, clock via `timerExec`) and
  `TableExporter` (CSV of the visible, sorted rows via `FileDialog`).

Not ported: the `fx/bridge/*App` single-panel launchers (JavaFX-bridge
specific). `BridgeApp` is ported (as `swt/bridge/BridgeApp`), and so is the
`vaadin-app` module (with `SwtBridge` instead of `JavaFXBridge`).

## Differences from the JavaFX version

- Login and main window are two shells, not two scenes in one stage; the
  login window is its own small, fixed-size window.
- Native SWT widgets cannot be restyled like JavaFX CSS: the colored primary
  and "Add" buttons, the table header colors and the accent-colored row
  selection are native here; the KPI cards have a border instead of a drop
  shadow; section titles are `Group` titles.
- Tables sort ascending / descending on header clicks (no third "unsorted"
  state) and show no "No records" placeholder. Empty "Expected" dates sort
  first.
- Charts are static drawings (no hover effects or animations); like the
  JavaFX dashboard they are loaded when the main window opens.
- Dialog button order follows the platform (OK first on Windows, last
  elsewhere) through a small helper rather than JavaFX's `ButtonBar`.
- Line-item cells start editing on a single click instead of JavaFX's
  click-on-selected-cell.

## Running in the browser

The SWT bridge runs on Windows only: every user's copy of WarehousePro runs in
the Vaadin app's JVM, with its native Win32 widgets, on a hidden desktop of its
own (see the extension's README). Clone this repository, e.g. into `C:\dev` (a
shared folder of a VM works too, but builds slower), then double-click
`start-vaadin.bat`, or:

```bat
start-vaadin.bat
rem with input logging: start-vaadin.bat -Dswingbridge.swt.debugInput=true
rem http://localhost:8890/          stage 1 (the app's own login: admin / admin)
rem http://localhost:8890/login     stages 2-4 (Vaadin login: admin / admin)
```

It builds this repository with the Maven
wrapper (no Maven install needed; a JDK 21 is), and runs
`vaadin-app/target/app.jar` on http://localhost:8890. The database is
`%LOCALAPPDATA%\warehouse-demo-swt\warehousedb` (H2 needs a local disk).

Prerequisites: JDK 21+, and a SwingBridge license or trial. `vaadin-app`
declares the SWT extension `1.0.0-alpha1`, from the Vaadin Directory's add-on
repository, and SwingBridge `1.4.0-alpha1`, from Vaadin's pre-release
repository. The Vaadin app excludes
SwingBridge's Log4j2 binding, which clashes with Spring Boot's logging.

Integration tests, against the running app:

```bat
cd vaadin-app && ..\mvnw.cmd -Pit test-compile failsafe:integration-test failsafe:verify
rem screenshots of every step in vaadin-app/target/it-screenshots
```

### How the Vaadin app runs the SWT app

- `vaadin-app/pom.xml` copies the Win32 SWT jar for the
  CPU of the JDK that runs the build and JNA to `target/swt-bridge`, which the bridge's
  shared SWT class loader loads, and the SWT app jar to `target/applibs`. The
  app jar is not a dependency of the Vaadin app.
- `swing-bridge-codegen-maven-plugin` runs on the app jar right after it is
  copied to `applibs`, and generates `BridgeAppBridge` for the views.
- `AppLibsClassLoader` loads the app's jars **child-first**, as in the JavaFX
  demo. The host has `warehouse-core` on its class path too, for the native
  views; with the usual parent-first loading every user would share its
  classes, and so one `AppSession` (the signed-in user) and one Hibernate
  factory. With child-first loading each user gets their own; only the JDK,
  SWT, the bridge, SwingBridge's interop annotations and the interop types in
  `com.example.warehouse.interop` are shared. Every route of a user talks to
  the same copy of the application.
- The host and every user's application share one H2 database
  (`-Dwarehouse.db`, a system property of the server, which the applications
  see too), so a supplier saved in the Vaadin form is what the SWT table
  reloads.
- `BridgeApp`'s window is undecorated (`SWT.NO_TRIM`) and marked as the main
  window (`swingbridge.swt.mainWindow`), so the bridge sizes it to the page.
