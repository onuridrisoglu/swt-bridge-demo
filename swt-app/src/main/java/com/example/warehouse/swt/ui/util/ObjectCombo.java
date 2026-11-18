package com.example.warehouse.swt.ui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

/**
 * A read-only drop-down of objects (the JavaFX {@code ComboBox<T>}): SWT's
 * {@link Combo} only holds strings, so the objects are kept alongside. A
 * {@code null} item is allowed and rendered by the label function.
 */
public final class ObjectCombo<T> {

    private final Combo combo;
    private final Function<T, String> label;
    private final List<T> items = new ArrayList<>();

    public ObjectCombo(Composite parent, Function<T, String> label) {
        this.combo = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
        this.label = label;
        combo.setVisibleItemCount(15);
    }

    public void setItems(List<T> values) {
        items.clear();
        items.addAll(values);
        combo.removeAll();
        for (T value : values) {
            combo.add(label.apply(value));
        }
    }

    public void selectFirst() {
        if (!items.isEmpty()) {
            combo.select(0);
        }
    }

    public void select(T value) {
        selectFirstMatch(item -> item == value);
    }

    public void selectFirstMatch(Predicate<T> predicate) {
        for (int i = 0; i < items.size(); i++) {
            if (predicate.test(items.get(i))) {
                combo.select(i);
                return;
            }
        }
    }

    public T getValue() {
        int index = combo.getSelectionIndex();
        return index >= 0 ? items.get(index) : null;
    }

    public List<T> getItems() {
        return items;
    }

    public Combo getControl() {
        return combo;
    }
}
