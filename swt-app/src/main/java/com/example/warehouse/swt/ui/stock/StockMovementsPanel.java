package com.example.warehouse.swt.ui.stock;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.example.warehouse.dao.StockMovementDAO;
import com.example.warehouse.model.StockMovement;
import com.example.warehouse.swt.ui.util.DataTable;
import com.example.warehouse.swt.ui.util.StatusBar;
import com.example.warehouse.swt.ui.util.TableExporter;
import com.example.warehouse.swt.ui.util.Toolbar;
import com.example.warehouse.swt.ui.util.Ui;

/** The Stock Log tab: a read-only, searchable log of stock movements. */
public class StockMovementsPanel extends Composite {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm");

    private final StockMovementDAO dao = new StockMovementDAO();
    private final StatusBar statusBar;
    private DataTable<StockMovement> table;

    public StockMovementsPanel(Composite parent, StatusBar statusBar) {
        super(parent, SWT.NONE);
        this.statusBar = statusBar;
        setBackground(Ui.color(Ui.WHITE));
        setLayout(Ui.grid(1, 8, 0));

        buildToolbar();
        buildTable();

        refresh();
    }

    private void buildToolbar() {
        Toolbar toolbar = new Toolbar(this);
        toolbar.button("↺ Refresh", this::refresh);
        toolbar.separator();
        toolbar.button("⬇ Export CSV",
                () -> TableExporter.exportToCsv(table, "stock-movements.csv", getShell()));
        toolbar.gap(12);
        Label note = toolbar.label(
                "Read-only log. Use Products → Stock Adjust to record movements.");
        note.setFont(Ui.font(-1, SWT.ITALIC));
        note.setForeground(Ui.color(Ui.HINT));
        toolbar.fill();
        toolbar.label("Search:");
        toolbar.searchField(this::filterTable);
        toolbar.done();
    }

    private void buildTable() {
        table = new DataTable<>(this);
        // Sorted by the date-time itself, shown formatted
        table.column("Date / Time", 130, StockMovement::getMovementDate)
                .format(v -> v == null ? "" : ((LocalDateTime) v).format(DATE_FMT));
        table.column("SKU", 80, m -> m.getProduct().getSku());
        table.column("Product Name", 200, m -> m.getProduct().getName());
        table.column("Category", 130, m -> m.getProduct().getCategory() != null
                ? m.getProduct().getCategory().getName() : "");
        table.column("Type", 90, m -> m.getMovementType().toString())
                .foreground(m -> switch (m.getMovementType()) {
                    case IN -> Ui.SUCCESS;
                    case OUT -> Ui.DANGER;
                    case ADJUSTMENT -> Ui.WARNING;
                });
        table.column("Quantity", 75, StockMovement::getQuantity).center();
        table.column("Reason", 250, m -> m.getReason() != null ? m.getReason() : "—");
        table.column("Created By", 100,
                m -> m.getCreatedBy() != null ? m.getCreatedBy() : "system");

        // IN / OUT rows get a green / red tint; adjustments keep the stripes.
        table.setRowBackground(m -> switch (m.getMovementType()) {
            case IN -> 0xebfceb;
            case OUT -> 0xfcf0eb;
            case ADJUSTMENT -> null;
        });
    }

    /**
     * Shows only rows where any column matches the text, as a case-insensitive
     * regular expression (like Swing's {@code RowFilter.regexFilter}).
     */
    private void filterTable(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            table.setFilter(null);
            return;
        }
        Pattern pattern;
        try {
            pattern = Pattern.compile(trimmed, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException ex) {
            pattern = Pattern.compile(Pattern.quote(trimmed), Pattern.CASE_INSENSITIVE);
        }
        Pattern p = pattern;
        table.setFilter(m -> table.anyColumnMatches(m, v -> p.matcher(v).find()));
    }

    public void refresh() {
        table.setItems(dao.findAll());
        statusBar.setRowCount(table.getItems().size(), "stock movement");
    }
}
