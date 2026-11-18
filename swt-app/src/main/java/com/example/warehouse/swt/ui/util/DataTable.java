package com.example.warehouse.swt.ui.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * A single-selection SWT {@link Table} bound to a list of objects: the SWT
 * counterpart of the JavaFX {@code TableView} with a {@code FilteredList}
 * and {@code SortedList}. Columns sort on a header click (ascending, then
 * descending), rows can be filtered and tinted, alternating rows are
 * striped, and the last column takes up the remaining width.
 */
public final class DataTable<T> {

    /** One column: a title, a preferred width and how to get and show its value. */
    public final class Column {
        private final String title;
        private final int width;
        private final Function<T, ?> value;
        private Function<Object, String> format = v -> v == null ? "" : v.toString();
        private Function<T, Integer> foreground;
        private boolean bold;
        private boolean sortable = true;
        private Comparator<T> comparator;
        private TableColumn tableColumn;

        private Column(String title, int width, Function<T, ?> value) {
            this.title = title;
            this.width = width;
            this.value = value;
        }

        public Column center() {
            tableColumn.setAlignment(SWT.CENTER);
            return this;
        }

        /** How the value is shown (and exported and searched); sorting uses the raw value. */
        public Column format(Function<Object, String> format) {
            this.format = format;
            return this;
        }

        /** Text color (0xRRGGBB) per row, or {@code null} for the default; drawn bold. */
        public Column foreground(Function<T, Integer> foreground) {
            this.foreground = foreground;
            this.bold = true;
            return this;
        }

        public Column notSortable() {
            this.sortable = false;
            return this;
        }

        public Column comparator(Comparator<T> comparator) {
            this.comparator = comparator;
            return this;
        }

        public String getTitle() {
            return title;
        }

        /** The text this column shows for a row. */
        public String text(T row) {
            String text = format.apply(value.apply(row));
            return text == null ? "" : text;
        }

        private Comparator<T> sortComparator() {
            if (comparator != null) {
                return comparator;
            }
            return (a, b) -> compareValues(value.apply(a), value.apply(b));
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private int compareValues(Object a, Object b) {
            if (a == null || b == null) {
                return a == null ? (b == null ? 0 : -1) : 1;
            }
            if (a instanceof Comparable && a.getClass() == b.getClass()) {
                return ((Comparable) a).compareTo(b);
            }
            return format.apply(a).compareToIgnoreCase(format.apply(b));
        }
    }

    private final Table table;
    private final List<Column> columns = new ArrayList<>();
    private final List<T> items = new ArrayList<>();
    private final List<T> visible = new ArrayList<>();
    private Predicate<T> filter;
    private Column sortColumn;
    private boolean ascending = true;
    private Function<T, Integer> rowBackground = row -> null;
    private Runnable selectionListener = () -> { };
    private Consumer<T> openListener;

    public DataTable(Composite parent) {
        table = new Table(parent, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION
                | SWT.V_SCROLL | SWT.H_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(false);
        table.setLayoutData(Ui.fill());
        table.addListener(SWT.Selection, e -> selectionListener.run());
        table.addListener(SWT.DefaultSelection, e -> {
            T row = getSelected();
            if (row != null && openListener != null) {
                openListener.accept(row);
            }
        });
        table.addListener(SWT.Resize, e -> fitLastColumn());
    }

    public Column column(String title, int width, Function<T, ?> value) {
        Column column = new Column(title, width, value);
        TableColumn tc = new TableColumn(table, SWT.LEFT);
        tc.setText(title);
        tc.setWidth(width);
        tc.setMoveable(false);
        tc.addListener(SWT.Selection, e -> sortBy(column));
        column.tableColumn = tc;
        columns.add(column);
        return column;
    }

    public List<Column> getColumns() {
        return columns;
    }

    /** Background color (0xRRGGBB) per row, or {@code null} to keep the striping. */
    public void setRowBackground(Function<T, Integer> rowBackground) {
        this.rowBackground = rowBackground;
    }

    public void onSelectionChanged(Runnable listener) {
        this.selectionListener = listener;
    }

    /** Called with the row on a double click or Enter. */
    public void onOpen(Consumer<T> listener) {
        this.openListener = listener;
    }

    /**
     * Adds a context menu, shown only when the pointer is on a row (which
     * gets selected first), like the JavaFX row context menus.
     */
    public Menu contextMenu() {
        Menu menu = new Menu(table);
        table.setMenu(menu);
        table.addListener(SWT.MenuDetect, e -> {
            if (e.detail == SWT.MENU_KEYBOARD) {
                e.doit = getSelected() != null;
                return;
            }
            Point point = table.toControl(e.x, e.y);
            if (point.y < table.getHeaderHeight()) {
                e.doit = false;
                return;
            }
            TableItem item = table.getItem(point);
            if (item == null) {
                e.doit = false;
                return;
            }
            table.setSelection(item);
            selectionListener.run();
        });
        return menu;
    }

    public static MenuItem menuItem(Menu menu, String text, Runnable action) {
        MenuItem item = new MenuItem(menu, SWT.PUSH);
        item.setText(text);
        item.addListener(SWT.Selection, e -> action.run());
        return item;
    }

    public void setItems(List<T> newItems) {
        items.clear();
        items.addAll(newItems);
        refresh();
    }

    public List<T> getItems() {
        return items;
    }

    /** Shows only the rows the predicate accepts; {@code null} shows all. */
    public void setFilter(Predicate<T> filter) {
        this.filter = filter;
        refresh();
    }

    /** Whether the text shown in any column of the row matches. */
    public boolean anyColumnMatches(T row, Predicate<String> matcher) {
        for (Column column : columns) {
            if (matcher.test(column.text(row))) {
                return true;
            }
        }
        return false;
    }

    /** The rows currently shown, filtered and in display order. */
    public List<T> getVisibleItems() {
        return visible;
    }

    public T getSelected() {
        int index = table.getSelectionIndex();
        return index >= 0 && index < visible.size() ? visible.get(index) : null;
    }

    public void clearSelection() {
        table.deselectAll();
        selectionListener.run();
    }

    /** Re-applies filter and sort and redraws every row, keeping the selection. */
    public void refresh() {
        T selected = getSelected();
        visible.clear();
        for (T item : items) {
            if (filter == null || filter.test(item)) {
                visible.add(item);
            }
        }
        if (sortColumn != null) {
            Comparator<T> comparator = sortColumn.sortComparator();
            visible.sort(ascending ? comparator : comparator.reversed());
        }

        table.setRedraw(false);
        try {
            table.removeAll();
            for (int i = 0; i < visible.size(); i++) {
                T row = visible.get(i);
                TableItem item = new TableItem(table, SWT.NONE);
                item.setData(row);
                String[] texts = new String[columns.size()];
                for (int c = 0; c < columns.size(); c++) {
                    Column column = columns.get(c);
                    texts[c] = column.text(row);
                    if (column.foreground != null) {
                        Integer fg = column.foreground.apply(row);
                        if (fg != null) {
                            item.setForeground(c, Ui.color(fg));
                        }
                    }
                    if (column.bold) {
                        item.setFont(c, Ui.bold());
                    }
                }
                item.setText(texts);
                Integer bg = rowBackground.apply(row);
                if (bg != null) {
                    item.setBackground(Ui.color(bg));
                } else {
                    item.setBackground(Ui.color(i % 2 == 1 ? Ui.ROW_ALT : Ui.WHITE));
                }
            }
            int index = selected == null ? -1 : indexOf(selected);
            if (index >= 0) {
                table.setSelection(index);
            }
        } finally {
            table.setRedraw(true);
        }
        fitLastColumn();
        selectionListener.run();
    }

    private int indexOf(T row) {
        for (int i = 0; i < visible.size(); i++) {
            if (visible.get(i) == row || Objects.equals(visible.get(i), row)) {
                return i;
            }
        }
        return -1;
    }

    private void sortBy(Column column) {
        if (!column.sortable) {
            return;
        }
        if (sortColumn == column) {
            ascending = !ascending;
        } else {
            sortColumn = column;
            ascending = true;
        }
        table.setSortColumn(column.tableColumn);
        table.setSortDirection(ascending ? SWT.UP : SWT.DOWN);
        refresh();
    }

    /** The last column takes the width the others leave free. */
    private void fitLastColumn() {
        if (columns.isEmpty() || table.isDisposed()) {
            return;
        }
        int client = table.getClientArea().width;
        int others = 0;
        for (int i = 0; i < columns.size() - 1; i++) {
            others += columns.get(i).tableColumn.getWidth();
        }
        Column last = columns.get(columns.size() - 1);
        last.tableColumn.setWidth(Math.max(last.width, client - others));
    }

    public Table getTable() {
        return table;
    }
}
