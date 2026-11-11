package com.dozingcatsoftware.vectorpinball.editor.inspector;

import java.util.Arrays;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableFieldElement;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableSensorElement;

public class SensorElementInspector extends ElementInspector {

    @Override public String getLabel() {
        return "Sensor";
    }

    @Override void drawInPane(Pane pane) {
        VBox box = new VBox();
        box.getChildren().add(createStringFieldWithLabel(EditableFieldElement.ID_PROPERTY, "ID"));
        box.getChildren().add(createMultiRowDecimalArrayFieldWithLabels(
                EditableSensorElement.RECT_PROPERTY,
                Arrays.asList("Left/Bottom", "Right/Top"),
                2, 2));
        box.getChildren().add(new Label("Layers:"));
        box.getChildren().add(createIntegerFieldWithLabel(
                EditableSensorElement.BALL_LAYER_FROM_PROPERTY, "Trigger at"));
        box.getChildren().add(createIntegerFieldWithLabel(
                EditableSensorElement.BALL_LAYER_TO_PROPERTY, "Move ball to"));
        box.getChildren().add(createBooleanCheckBoxFieldWithLabel(
                EditableSensorElement.RECORD_BALL_TIMES_PROPERTY, "Record times"));
        pane.getChildren().add(box);
    }

}
