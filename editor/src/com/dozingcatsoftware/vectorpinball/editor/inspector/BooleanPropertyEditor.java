package com.dozingcatsoftware.vectorpinball.editor.inspector;

import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;

public class BooleanPropertyEditor extends PropertyEditor<Boolean> {

    CheckBox checkbox;

    public BooleanPropertyEditor() {
        HBox box = new HBox();
        this.checkbox = new CheckBox();
        this.checkbox.selectedProperty().addListener((event) -> runChangeHandler());
        box.getChildren().add(checkbox);
        setContainer(box);
    }

    @Override public Boolean getValue() {
        return Boolean.valueOf(checkbox.isSelected());
    }

    @Override void updateFromValue(Boolean value) {
        checkbox.setSelected(Boolean.TRUE.equals(value));
    }
}
