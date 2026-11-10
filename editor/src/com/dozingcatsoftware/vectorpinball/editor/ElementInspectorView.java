package com.dozingcatsoftware.vectorpinball.editor;

import static com.dozingcatsoftware.vectorpinball.util.Localization.localizedString;

import java.util.Set;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableField;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableFieldElement;
import com.dozingcatsoftware.vectorpinball.editor.inspector.ElementInspector;
import com.dozingcatsoftware.vectorpinball.editor.inspector.ElementInspectorRegistry;
import com.dozingcatsoftware.vectorpinball.editor.inspector.GlobalPropertiesInspector;

public class ElementInspectorView extends VBox {

    EditableField editableField;
    UndoStack undoStack;
    Runnable changeCallback;

    Label selectionLabel;
    HBox selectionRow;
    ElementInspector currentInspector;
    Pane inspectorPane;
    Button deleteElementButton;

    public ElementInspectorView() {
        selectionLabel = new Label("");
        selectionLabel.setFont(new Font(16));

        selectionRow = new HBox(10);
        selectionRow.setAlignment(Pos.CENTER_LEFT);
        selectionRow.setPadding(new Insets(0, 0, 10, 0));
        selectionRow.getChildren().add(selectionLabel);
        this.getChildren().add(selectionRow);

        deleteElementButton = new Button(localizedString("Delete"));
        deleteElementButton.setOnAction((event) -> deleteSelectedElements());
        update();
    }

    public void setEditableField(EditableField field) {
        editableField = field;
    }

    public void setUndoStack(UndoStack stack) {
        undoStack = stack;
    }

    public void setChangeCallback(Runnable callback) {
        changeCallback = callback;
    }

    public void updateInspectorValues() {
        if (currentInspector != null) {
            currentInspector.updateControlValuesFromElement();
        }
    }

    public void deleteSelectedElements() {
        Set<EditableFieldElement> selected = editableField.getSelectedElements();
        editableField.clearSelection();
        editableField.removeElements(selected);
        if (changeCallback != null) {
            changeCallback.run();
        }
    }

    public void update() {
        if (editableField == null) {
            // Shouldn't happen.
            return;
        }
        if (editableField.hasSelection()) {
            EditableFieldElement elem = editableField.getSelectedElements().iterator().next();
            if (currentInspector==null || currentInspector.getPropertyContainer()!=elem) {
                if (inspectorPane!=null) this.getChildren().remove(inspectorPane);
                selectionRow.getChildren().remove(deleteElementButton);
                this.getChildren().add(inspectorPane = new Pane());

                currentInspector = ElementInspectorRegistry.createInspector(elem);
                selectionLabel.setText(localizedString(currentInspector.getLabel()));
                currentInspector.initialize(inspectorPane, elem, changeCallback);
                selectionRow.getChildren().add(deleteElementButton);
            }
        }
        else {
            if (inspectorPane!=null) this.getChildren().remove(inspectorPane);
            selectionRow.getChildren().remove(deleteElementButton);
            this.getChildren().add(inspectorPane = new Pane());
            currentInspector = new GlobalPropertiesInspector();
            currentInspector.initialize(inspectorPane, editableField, changeCallback);
            selectionLabel.setText(localizedString(currentInspector.getLabel()));
        }
    }
}
