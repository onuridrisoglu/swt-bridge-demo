package com.example.warehouse.swt.ui.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

/** Exports the rows a table currently shows (sorted and filtered) as CSV. */
public final class TableExporter {

    private TableExporter() {
    }

    public static <T> void exportToCsv(DataTable<T> table, String suggestedName, Shell owner) {
        FileDialog chooser = new FileDialog(owner, SWT.SAVE);
        chooser.setText("Export to CSV");
        chooser.setFileName(suggestedName);
        chooser.setFilterExtensions(new String[] { "*.csv" });
        chooser.setFilterNames(new String[] { "CSV files (*.csv)" });
        chooser.setOverwrite(true);
        String path = chooser.open();
        if (path == null) {
            return;
        }
        File file = new File(path);
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getAbsolutePath() + ".csv");
        }
        try (PrintWriter pw = new PrintWriter(file, StandardCharsets.UTF_8)) {
            StringBuilder row = new StringBuilder();
            for (DataTable<T>.Column column : table.getColumns()) {
                if (!row.isEmpty()) {
                    row.append(',');
                }
                row.append(escape(column.getTitle()));
            }
            pw.println(row);
            for (T item : table.getVisibleItems()) {
                row.setLength(0);
                boolean first = true;
                for (DataTable<T>.Column column : table.getColumns()) {
                    if (!first) {
                        row.append(',');
                    }
                    first = false;
                    row.append(escape(column.text(item)));
                }
                pw.println(row);
            }
        } catch (IOException ex) {
            Ui.error(owner, "Export Error", "Export failed: " + ex.getMessage());
        }
    }

    private static String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
