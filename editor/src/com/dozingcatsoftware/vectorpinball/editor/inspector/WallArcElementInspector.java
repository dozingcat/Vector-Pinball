package com.dozingcatsoftware.vectorpinball.editor.inspector;

import static com.dozingcatsoftware.vectorpinball.util.Localization.localizedString;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableFieldElement;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableWallArcElement;

public class WallArcElementInspector extends ElementInspector {

    @Override public String getLabel() {
        return "Wall arc";
    }

    @Override void drawInPane(Pane pane) {
        VBox box = new VBox();
        box.getChildren().add(createStringFieldWithLabel(
                EditableFieldElement.ID_PROPERTY, localizedString("ID")));
        box.getChildren().add(createIntegerFieldWithLabel(
                EditableFieldElement.LAYER_PROPERTY, "Layer"));
        box.getChildren().add(createColorSelectorWithLabel(
                EditableFieldElement.COLOR_PROPERTY, localizedString("Color")));
        box.getChildren().add(createColorSelectorWithLabel(
                EditableFieldElement.INACTIVE_LAYER_COLOR_PROPERTY,
                localizedString("Inactive layer")));
        box.getChildren().add(createPositionStringFieldsWithLabel(
                EditableWallArcElement.CENTER_PROPERTY, localizedString("Center")));
        box.getChildren().add(createDecimalStringFieldWithLabel(
                EditableWallArcElement.RADIUS_PROPERTY, localizedString("Radius")));
        box.getChildren().add(createDecimalStringFieldWithLabel(
                EditableWallArcElement.X_RADIUS_PROPERTY, localizedString("X Radius")));
        box.getChildren().add(createDecimalStringFieldWithLabel(
                EditableWallArcElement.Y_RADIUS_PROPERTY, localizedString("Y Radius")));
        box.getChildren().add(createIntegerFieldWithLabel(
                EditableWallArcElement.NUM_SEGMENTS_PROPERTY, localizedString("# Segments")));
        box.getChildren().add(createDecimalStringFieldWithLabel(
                EditableWallArcElement.MIN_ANGLE_PROPERTY, localizedString("Min angle")));
        box.getChildren().add(createDecimalStringFieldWithLabel(
                EditableWallArcElement.MAX_ANGLE_PROPERTY, localizedString("Max angle")));
        box.getChildren().add(createDecimalStringFieldWithLabel(
                EditableWallArcElement.RESTITUTION_PROPERTY, localizedString("Restitution")));
        box.getChildren().add(createDecimalStringFieldWithLabel(
                EditableWallArcElement.FRICTION_PROPERTY, localizedString("Friction")));
        box.getChildren().add(createBooleanCheckBoxFieldWithLabel(
                EditableWallArcElement.IGNORE_BALL_PROPERTY, localizedString("Ignore ball")));
        pane.getChildren().add(box);
    }

}
