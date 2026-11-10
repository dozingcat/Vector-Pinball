package com.dozingcatsoftware.vectorpinball.editor.inspector;

import javafx.scene.layout.HBox;

import com.dozingcatsoftware.vectorpinball.util.MathUtils;

/**
 * Editor for a floating point value which shows a text field. Accepts strings or numbers in
 * {@link updateFromValue}, but always returns strings in {@link getValue} to avoid
 * floating point rounding issues.
 */
public class DecimalStringPropertyEditor extends PropertyEditor<Object> {

    DecimalTextField textField;

    public DecimalStringPropertyEditor() {
        HBox box = new HBox();
        this.textField = new DecimalTextField();
        this.textField.setChangeHandler(this::runChangeHandler);
        box.getChildren().add(textField);
        setContainer(box);
    }

    @Override public String getValue() {
        String text = textField.getText();
        return (text!=null && text.length()>0 && textField.isTextValid(text)) ? text : null;
    }

    @Override void updateFromValue(Object value) {
        textField.setText(Utils.toFormattedNumber(value));
    }
}
