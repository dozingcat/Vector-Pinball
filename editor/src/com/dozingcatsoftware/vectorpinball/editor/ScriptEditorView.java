package com.dozingcatsoftware.vectorpinball.editor;

import static com.dozingcatsoftware.vectorpinball.util.Localization.localizedString;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableField;

public class ScriptEditorView extends VBox {

    EditableField editableField;
    Runnable changeHandler;
    TextArea scriptArea;

    public ScriptEditorView() {
        super(10);
        this.setPadding(new Insets(20, 20, 20, 20));
        setBackground(new Background(new BackgroundFill(Color.rgb(224, 255, 224), null, null)));

        Label title = new Label(localizedString("Game script"));
        title.setFont(new Font(16));
        scriptArea = new ScriptTextArea();
        scriptArea.setFont(Font.font("Courier New", 14));
        VBox.setVgrow(scriptArea, Priority.ALWAYS);
        this.getChildren().addAll(title, scriptArea);
    }

    public void setEditableField(EditableField field) {
        editableField = field;
    }

    public void setChangeHandler(Runnable handler) {
        changeHandler = handler;
    }

    public void updateScriptText() {
        Object script = editableField.getProperty(EditableField.SCRIPT_PROPERTY);
        scriptArea.setText((script instanceof String) ? (String)script : "");
    }

    public void scriptTextChanged() {
        editableField.setProperty(EditableField.SCRIPT_PROPERTY, scriptArea.getText());
        // TODO: call parent change handler, debounced to every second or so.
    }

    class ScriptTextArea extends TextArea {
        @Override public void replaceText(int start, int end, String text) {
            super.replaceText(start, end, text);
            scriptTextChanged();
        }

        @Override public void replaceSelection(String text) {
            super.replaceSelection(text);
            scriptTextChanged();
        }
    }
}
