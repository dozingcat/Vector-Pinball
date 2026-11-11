package com.dozingcatsoftware.vectorpinball.editor.inspector;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import com.dozingcatsoftware.vectorpinball.editor.elements.PropertyContainer;

public abstract class ElementInspector {

    public static final int DEFAULT_HBOX_SPACING = 5;
    public static final Insets DEFAULT_HBOX_INSETS = new Insets(4, 0, 4, 0);
    public static final int DEFAULT_LABEL_WIDTH = 95;

    PropertyContainer propertyContainer;
    Runnable changeCallback;
    boolean updatingFromExternalChange = false;

    Map<String, PropertyEditor> propertyToEditor = new HashMap<>();

    /** Returns the unlocalized title for the element. */
    public abstract String getLabel();

    public void initialize(Pane pane, PropertyContainer propertyContainer, Runnable changeCallback) {
        this.propertyContainer = propertyContainer;
        this.changeCallback = changeCallback;
        drawInPane(pane);
        updateControlValuesFromElement();
    }

    abstract void drawInPane(Pane pane);

    public PropertyContainer getPropertyContainer() {
        return propertyContainer;
    }

    protected void notifyChanged() {
        if (changeCallback != null && !updatingFromExternalChange) {
            changeCallback.run();
        }
    }

    public void updateControlValuesFromElement() {
        updatingFromExternalChange = true;
        for (String prop : propertyToEditor.keySet()) {
            Object value = getPropertyContainer().getProperty(prop);
            propertyToEditor.get(prop).updateFromValue(value);
        }
        updateCustomControlValues();
        updatingFromExternalChange = false;
    }

    // Override to update UI components that aren't handled by the standard
    // cases in updateControlValuesFromElement.
    protected void updateCustomControlValues() {
    }

    static HBox createHBoxWithLabel(String text) {
        HBox box = new HBox(DEFAULT_HBOX_SPACING);
        box.setPadding(DEFAULT_HBOX_INSETS);
        box.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label(text);
        label.setPrefWidth(DEFAULT_LABEL_WIDTH);
        box.getChildren().add(label);
        return box;
    }

    void setOrRemoveProperty(String propertyName, Object value) {
        if (updatingFromExternalChange) return;
        getPropertyContainer().setOrRemoveProperty(propertyName, value);
        notifyChanged();
    }

    HBox createHBoxWithLabelAndEditor(String propertyName, String label, PropertyEditor editor) {
        HBox box = createHBoxWithLabel(label);
        editor.setOnChange(() -> setOrRemoveProperty(propertyName, editor.getValue()));
        box.getChildren().add(editor.getContainer());
        propertyToEditor.put(propertyName, editor);
        return box;
    }

    HBox createStringFieldWithLabel(String propertyName, String label) {
        return createHBoxWithLabelAndEditor(propertyName, label, new StringPropertyEditor());
    }

    HBox createDecimalStringFieldWithLabel(String propertyName, String label) {
        return createHBoxWithLabelAndEditor(propertyName, label, new DecimalStringPropertyEditor());
    }

    HBox createIntegerFieldWithLabel(String propertyName, String label) {
        return createHBoxWithLabelAndEditor(propertyName, label, new IntegerPropertyEditor());
    }

    HBox createBooleanCheckBoxFieldWithLabel(String propertyName, String label) {
        return createHBoxWithLabelAndEditor(propertyName, label, new BooleanPropertyEditor());
    }

    HBox createColorSelectorWithLabel(String propertyName, String label) {
        return createHBoxWithLabelAndEditor(propertyName, label, new ColorPropertyEditor());
    }

    // For multiple rows, the editor manages the labels as well as the text fields.
    Pane createMultiRowDecimalArrayFieldWithLabels(
            String propertyName, List<String> rowLabels, int numFieldsPerRow, int numRows) {
        MultiRowDecimalArrayEditor editor = new MultiRowDecimalArrayEditor(rowLabels, numFieldsPerRow, numRows);
        editor.setOnChange(() -> setOrRemoveProperty(propertyName, editor.getValue()));
        propertyToEditor.put(propertyName, editor);
        return editor.getContainer();
    }

    // "Position" properties can be a special case of multirow fields, with just one row.
    Pane createPositionStringFieldsWithLabel(String propertyName, String label) {
        return createMultiRowDecimalArrayFieldWithLabels(propertyName, Collections.singletonList(label), 2, 1);
    }

    static Region createVerticalSpacer(double height) {
        Region reg = new Region();
        reg.setMinHeight(height);
        return reg;
    }
}
