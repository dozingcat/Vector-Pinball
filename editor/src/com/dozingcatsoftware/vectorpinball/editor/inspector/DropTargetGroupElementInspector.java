package com.dozingcatsoftware.vectorpinball.editor.inspector;

import static com.dozingcatsoftware.vectorpinball.util.Localization.localizedString;
import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asDouble;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableFieldElement;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableDropTargetGroupElement;

public class DropTargetGroupElementInspector extends ElementInspector {

    static class PositionRow {
        Pane region;
        List<TextField> textFields;
    }

    Pane positionRegion;
    List<PositionRow> positionRows = new ArrayList<>();

    @Override public String getLabel() {
        return "Drop targets";
    }

    @Override void drawInPane(Pane pane) {
        VBox box = new VBox(5);
        box.getChildren().add(createStringFieldWithLabel(
                EditableDropTargetGroupElement.ID_PROPERTY, "ID"));
        box.getChildren().add(createIntegerFieldWithLabel(
                EditableFieldElement.LAYER_PROPERTY, "Layer"));
        box.getChildren().add(createColorSelectorWithLabel(
                EditableDropTargetGroupElement.COLOR_PROPERTY, "Color"));
        box.getChildren().add(createColorSelectorWithLabel(
                EditableFieldElement.INACTIVE_LAYER_COLOR_PROPERTY, "Inactive layer"));
        box.getChildren().add(createIntegerFieldWithLabel(
                EditableDropTargetGroupElement.SCORE_PROPERTY, "Score"));

        box.getChildren().add(createDecimalStringFieldWithLabel(
                EditableDropTargetGroupElement.RESET_DELAY_PROPERTY, "Reset delay"));
        // Positions: array of 4-element decimal arrays
        box.getChildren().add(new Label("Specific positions"));
        positionRegion = new VBox(5);
        box.getChildren().add(positionRegion);

        Button addButton = new Button("Add target");
        addButton.setOnAction((event) -> addNewTargetWithPosition());
        box.getChildren().add(addButton);

        // TODO: Properties relative to wall.
        box.getChildren().add(new Label("Relative to wall"));
        box.getChildren().add(new Label("(Only if no positions)"));

        box.getChildren().add(createPositionStringFieldsWithLabel(
                EditableDropTargetGroupElement.WALL_START_PROPERTY, "Wall start"));
        box.getChildren().add(createPositionStringFieldsWithLabel(
                EditableDropTargetGroupElement.WALL_END_PROPERTY, "Wall end"));
        box.getChildren().add(createDecimalStringFieldWithLabel(
                EditableDropTargetGroupElement.GAP_FROM_WALL_PROPERTY, "Gap from wall"));
        box.getChildren().add(createDecimalStringFieldWithLabel(
                EditableDropTargetGroupElement.START_DISTANCE_ALONG_WALL_PROPERTY, "Start along wall"));
        box.getChildren().add(createDecimalStringFieldWithLabel(
                EditableDropTargetGroupElement.TARGET_WIDTH_PROPERTY, "Target width"));
        box.getChildren().add(createDecimalStringFieldWithLabel(
                EditableDropTargetGroupElement.GAP_BETWEEN_TARGETS_PROPERTY, "Gap between targets"));
        box.getChildren().add(createIntegerFieldWithLabel(
                EditableDropTargetGroupElement.NUM_TARGETS_PROPERTY, "# of targets"));

        pane.getChildren().add(box);
    }

    void addPositionRow() {
        PositionRow row = new PositionRow();

        row.region = new HBox(5);
        row.textFields = new ArrayList<>();
        for (int i=0; i<4; i++) {
            DecimalTextField field = new DecimalTextField();
            field.setPrefWidth(50);
            field.setChangeHandler(this::updatePositionsList);

            row.region.getChildren().add(field);
            row.textFields.add(field);
        }
        Button removeButton = new Button("Remove");
        removeButton.setOnAction((event) -> removePositionRow(row));
        row.region.getChildren().add(removeButton);

        positionRows.add(row);
        positionRegion.getChildren().add(row.region);
    }

    void removePositionRow(PositionRow row) {
        positionRows.remove(row);
        positionRegion.getChildren().remove(row.region);
        updatePositionsList();
    }

    void addNewTargetWithPosition() {
        List<List<Object>> newPositions = null;

        @SuppressWarnings("unchecked")
        List<List<Object>> positions =
        (List<List<Object>>)this.getPropertyContainer().getProperty(EditableDropTargetGroupElement.POSITIONS_PROPERTY);
        if (positions==null || positions.size()==0) {
            newPositions = Arrays.asList(Arrays.asList("-0.5", "-0.5", "-0.5", "0.5"));
        }
        else if (positions.size()==1) {
            List<Object> pos = positions.get(0);
            double x1 = asDouble(pos.get(0));
            double y1 = asDouble(pos.get(1));
            double x2 = asDouble(pos.get(2));
            double y2 = asDouble(pos.get(3));
            newPositions = new ArrayList<>(positions);
            newPositions.add(Arrays.asList(
                    String.valueOf(x2 + (x2 - x1)),
                    String.valueOf(y2 + (y2 - y1)),
                    String.valueOf(x2 + 2*(x2 - x1)),
                    String.valueOf(y2 + 2*(y2 - y1))));
        }
        else {
            List<Object> last1 = positions.get(positions.size()-1);
            double last1_0 = asDouble(last1.get(0));
            double last1_1 = asDouble(last1.get(1));
            double last1_2 = asDouble(last1.get(2));
            double last1_3 = asDouble(last1.get(3));
            List<Object> last2 = positions.get(positions.size()-2);
            double last2_0 = asDouble(last2.get(0));
            double last2_1 = asDouble(last2.get(1));
            double last2_2 = asDouble(last2.get(2));
            double last2_3 = asDouble(last2.get(3));

            newPositions = new ArrayList<>(positions);
            newPositions.add(Arrays.asList(
                    String.valueOf(last1_0 + (last1_0 - last2_0)),
                    String.valueOf(last1_1 + (last1_1 - last2_1)),
                    String.valueOf(last1_2 + (last1_2 - last2_2)),
                    String.valueOf(last1_3 + (last1_3 - last2_3))));
        }
        getPropertyContainer().setProperty(EditableDropTargetGroupElement.POSITIONS_PROPERTY, newPositions);
        notifyChanged();
    }

    @Override protected void updateCustomControlValues() {
        @SuppressWarnings("unchecked")
        List<List<Object>> positions =
        (List<List<Object>>)this.getPropertyContainer().getProperty(EditableDropTargetGroupElement.POSITIONS_PROPERTY);
        if (positions == null) positions = Collections.emptyList();

        while (positionRows.size() < positions.size()) {
            addPositionRow();
        }
        while (positionRows.size() < positions.size()) {
            removePositionRow(positionRows.get(positionRows.size()-1));
        }
        for (int i=0; i<positions.size(); i++) {
            List<Object> pos = positions.get(i);
            List<TextField> textFields = positionRows.get(i).textFields;
            for (int j=0; j<4; j++) {
                Object value = pos.get(j);
                textFields.get(j).setText(value!=null ? value.toString() : "");
            }
        }
    }

    void updatePositionsList() {
        List<List<String>> newPositions = new ArrayList<>();
        for (PositionRow row : positionRows) {
            List<TextField> fields = row.textFields;
            newPositions.add(Arrays.asList(fields.get(0).getText(), fields.get(1).getText(),
                    fields.get(2).getText(), fields.get(3).getText()));
        }
        getPropertyContainer().setProperty(EditableDropTargetGroupElement.POSITIONS_PROPERTY, newPositions);
        notifyChanged();
    }
}
