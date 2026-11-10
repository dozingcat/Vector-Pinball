package com.dozingcatsoftware.vectorpinball.editor.inspector;

import javafx.scene.layout.HBox;

public class StringPropertyEditor extends PropertyEditor<String> {

    StringTextField textField;

    public StringPropertyEditor() {
        HBox box = new HBox();
        this.textField = new StringTextField();
        this.textField.setChangeHandler(this::runChangeHandler);
        box.getChildren().add(textField);
        setContainer(box);
    }

    @Override String getValue() {
        String text = textField.getText();
        return (text!=null && text.length()>0) ? text : null;
    }

    @Override void updateFromValue(String value) {
        textField.setText((value!=null) ? value : "");
    }

    // Just so we can use setChangeHandler for consistency with other editors.
    static class StringTextField extends ConstrainedTextField {
        @Override boolean isTextValid(String text) {
            return true;
        }
    }
}
