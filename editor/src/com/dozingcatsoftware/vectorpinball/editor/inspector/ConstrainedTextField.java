package com.dozingcatsoftware.vectorpinball.editor.inspector;

import javafx.scene.control.IndexRange;
import javafx.scene.control.TextField;

public abstract class ConstrainedTextField extends TextField {

    abstract boolean isTextValid(String text);

    @Override public void replaceText(int start, int end, String text) {
        String origText = this.getText();
        String newText = origText.substring(0, start) + text + origText.substring(end);
        if (isTextValid(newText)) {
            super.replaceText(start, end, text);
        }
    }

    @Override public void replaceSelection(String text) {
        String origText = this.getText();
        IndexRange selection = this.getSelection();
        String newText = origText.substring(0, selection.getStart()) +
                text + origText.substring(selection.getEnd());
        if (isTextValid(newText)) {
            super.replaceSelection(text);
        }
    }

    public void setChangeHandler(Runnable action) {
        setOnAction((event) -> action.run());
        focusedProperty().addListener((target, wasFocused, isFocused) -> {
            if (!isFocused) action.run();
        });
    }

}
