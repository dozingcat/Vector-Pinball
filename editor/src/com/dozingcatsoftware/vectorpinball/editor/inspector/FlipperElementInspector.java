package com.dozingcatsoftware.vectorpinball.editor.inspector;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableFieldElement;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableFlipperElement;

import static com.dozingcatsoftware.vectorpinball.util.Localization.localizedString;

public class FlipperElementInspector extends ElementInspector {

    @Override public String getLabel() {
        return "Flipper";
    }

    @Override void drawInPane(Pane pane) {
        VBox box = new VBox();
        box.getChildren().add(createStringFieldWithLabel(EditableFlipperElement.ID_PROPERTY, "ID"));
        box.getChildren().add(createIntegerFieldWithLabel(EditableFieldElement.LAYER_PROPERTY, "Layer"));
        box.getChildren().add(createColorSelectorWithLabel(EditableFlipperElement.COLOR_PROPERTY, "Color"));
        box.getChildren().add(createColorSelectorWithLabel(
                EditableFieldElement.INACTIVE_LAYER_COLOR_PROPERTY, localizedString("Inactive layer")));
        box.getChildren().add(createPositionStringFieldsWithLabel(EditableFlipperElement.POSITION_PROPERTY, "Center"));
        box.getChildren().add(createDecimalStringFieldWithLabel(EditableFlipperElement.LENGTH_PROPERTY, "Length"));
        box.getChildren().add(createDecimalStringFieldWithLabel(EditableFlipperElement.MIN_ANGLE_PROPERTY, "Min angle"));
        box.getChildren().add(createDecimalStringFieldWithLabel(EditableFlipperElement.MAX_ANGLE_PROPERTY, "Max angle"));
        box.getChildren().add(createDecimalStringFieldWithLabel(EditableFlipperElement.UP_SPEED_PROPERTY, "Up speed"));
        box.getChildren().add(createDecimalStringFieldWithLabel(EditableFlipperElement.DOWN_SPEED_PROPERTY, "Down speed"));
        pane.getChildren().add(box);
    }

}
