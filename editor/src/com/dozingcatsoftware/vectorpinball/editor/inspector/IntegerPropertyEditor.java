package com.dozingcatsoftware.vectorpinball.editor.inspector;

import javafx.scene.layout.HBox;

public class IntegerPropertyEditor extends PropertyEditor<Number> {

    IntegerTextField textField;

    public IntegerPropertyEditor() {
        HBox box = new HBox();
        this.textField = new IntegerTextField();
        this.textField.setChangeHandler(this::runChangeHandler);
        box.getChildren().add(textField);
        setContainer(box);
    }

    @Override Long getValue() {
        String text = textField.getText();
        return (text!=null && text.length()>0 && textField.isTextValid(text)) ? Long.valueOf(text) : null;
    }

    @Override void updateFromValue(Number value) {
        textField.setText((value!=null) ? value.toString() : "");
    }
}
