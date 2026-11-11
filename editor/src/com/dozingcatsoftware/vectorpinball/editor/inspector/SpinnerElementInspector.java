package com.dozingcatsoftware.vectorpinball.editor.inspector;

import static com.dozingcatsoftware.vectorpinball.util.Localization.localizedString;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableFieldElement;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableSpinnerElement;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableWallElement;

import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class SpinnerElementInspector extends ElementInspector {

    @Override public String getLabel() {
        return "Spinner";
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

        box.getChildren().add(createPositionStringFieldsWithLabel(EditableSpinnerElement.CENTER_PROPERTY, "Positions"));
        box.getChildren().add(createDecimalStringFieldWithLabel(EditableSpinnerElement.RADIUS_PROPERTY, "Radius"));

        box.getChildren().add(createIntegerFieldWithLabel(
                EditableWallElement.SCORE_PROPERTY, localizedString("Score")));

        box.getChildren().add(createDecimalStringFieldWithLabel(
                EditableSpinnerElement.MIN_ACTIVATION_SPEED_PROPERTY, "Min activation speed"));
        box.getChildren().add(createDecimalStringFieldWithLabel(
                EditableSpinnerElement.BASE_CYCLES_PER_SECOND_PROPERTY, "Base cycles/sec"));
        box.getChildren().add(createDecimalStringFieldWithLabel(
                EditableSpinnerElement.MIN_CYCLES_PER_SECOND_PROPERTY, "Min cycles/sec"));
        box.getChildren().add(createDecimalStringFieldWithLabel(
                EditableSpinnerElement.SPEED_DECAY_RATE_PROPERTY, "Speed decay rate"));

        pane.getChildren().add(box);
    }
}
