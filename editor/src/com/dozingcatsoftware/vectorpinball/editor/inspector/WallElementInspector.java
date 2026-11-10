package com.dozingcatsoftware.vectorpinball.editor.inspector;

import static com.dozingcatsoftware.vectorpinball.util.Localization.localizedString;

import java.util.Arrays;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableFieldElement;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableWallElement;

public class WallElementInspector extends ElementInspector {

    @Override public String getLabel() {
        return "Wall";
    }

    @Override void drawInPane(Pane pane) {
        VBox box = new VBox();
        box.getChildren().add(createStringFieldWithLabel(
                EditableFieldElement.ID_PROPERTY, localizedString("ID")));
        box.getChildren().add(createIntegerFieldWithLabel(
                EditableFieldElement.LAYER_PROPERTY, localizedString("Layer")));
        box.getChildren().add(createColorSelectorWithLabel(
                EditableFieldElement.COLOR_PROPERTY, localizedString("Color")));
        box.getChildren().add(createColorSelectorWithLabel(
                EditableFieldElement.INACTIVE_LAYER_COLOR_PROPERTY,
                localizedString("Inactive layer")));
        // For position, use 2x2 grid of text fields which produce a value of a 4-element list.
        box.getChildren().add(createMultiRowDecimalArrayFieldWithLabels(
                EditableWallElement.POSITION_PROPERTY,
                Arrays.asList(localizedString("Start"), localizedString("End")),
                2, 2));

        box.getChildren().add(createIntegerFieldWithLabel(
                EditableWallElement.SCORE_PROPERTY, localizedString("Score")));
        box.getChildren().add(createDecimalStringFieldWithLabel(
                EditableWallElement.KICK_PROPERTY, localizedString("Kick")));
        box.getChildren().add(createDecimalStringFieldWithLabel(
                EditableWallElement.RESTITUTION_PROPERTY, localizedString("Restitution")));
        box.getChildren().add(createDecimalStringFieldWithLabel(
                EditableWallElement.FRICTION_PROPERTY, localizedString("Friction")));

        box.getChildren().add(createBooleanCheckBoxFieldWithLabel(
                EditableWallElement.IGNORE_BALL_PROPERTY, localizedString("Ignore ball")));
        box.getChildren().add(createBooleanCheckBoxFieldWithLabel(
                EditableWallElement.RETRACT_WHEN_HIT_PROPERTY, localizedString("Retract when hit")));
        box.getChildren().add(createBooleanCheckBoxFieldWithLabel(
                EditableWallElement.KILL_PROPERTY, localizedString("Kill ball")));

        pane.getChildren().add(box);
    }

}
