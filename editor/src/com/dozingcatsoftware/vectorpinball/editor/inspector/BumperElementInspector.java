package com.dozingcatsoftware.vectorpinball.editor.inspector;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableFieldElement;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableBumperElement;

import static com.dozingcatsoftware.vectorpinball.util.Localization.localizedString;

public class BumperElementInspector extends ElementInspector {

    @Override public String getLabel() {
        return "Bumper";
    }

    @Override void drawInPane(Pane pane) {
        VBox box = new VBox();
        box.getChildren().add(createStringFieldWithLabel(EditableFieldElement.ID_PROPERTY, "ID"));
        box.getChildren().add(createIntegerFieldWithLabel(EditableFieldElement.LAYER_PROPERTY, "Layer"));
        box.getChildren().add(createColorSelectorWithLabel(EditableFieldElement.COLOR_PROPERTY, "Color"));
        box.getChildren().add(createColorSelectorWithLabel(
                EditableFieldElement.INACTIVE_LAYER_COLOR_PROPERTY, localizedString("Inactive layer")));
        box.getChildren().add(createPositionStringFieldsWithLabel(EditableBumperElement.POSITION_PROPERTY, "Center"));
        box.getChildren().add(createDecimalStringFieldWithLabel(EditableBumperElement.RADIUS_PROPERTY, "Radius"));
        box.getChildren().add(
                createDecimalStringFieldWithLabel(EditableBumperElement.OUTER_RADIUS_PROPERTY, "Outer radius"));
        box.getChildren().add(
                createColorSelectorWithLabel(EditableBumperElement.OUTER_COLOR_PROPERTY, "Outer color"));
        box.getChildren().add(createColorSelectorWithLabel(
                EditableBumperElement.INACTIVE_LAYER_OUTER_COLOR_PROPERTY, localizedString("Inactive layer outer")));
        box.getChildren().add(createIntegerFieldWithLabel(EditableBumperElement.SCORE_PROPERTY, "Score"));
        box.getChildren().add(createDecimalStringFieldWithLabel(EditableBumperElement.KICK_PROPERTY, "Kick"));

        pane.getChildren().add(box);
    }

}
