package com.dozingcatsoftware.vectorpinball.editor.inspector;

import static com.dozingcatsoftware.vectorpinball.util.Localization.localizedString;
import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asDouble;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableFieldElement;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableWallPathElement;
import com.dozingcatsoftware.vectorpinball.util.MathUtils;

public class WallPathElementInspector extends ElementInspector {

    static class PointRow {
        Pane region;
        List<TextField> textFields;
    }

    Pane pointRegion;
    List<PointRow> pointRows = new ArrayList<>();

    @Override public String getLabel() {
        return "Wall path";
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
        box.getChildren().add(createBooleanCheckBoxFieldWithLabel(
                EditableWallPathElement.IGNORE_BALL_PROPERTY, localizedString("Ignore ball")));
        box.getChildren().add(createDecimalStringFieldWithLabel(
                EditableWallPathElement.RESTITUTION_PROPERTY, localizedString("Restitution")));
        box.getChildren().add(createDecimalStringFieldWithLabel(
                EditableWallPathElement.FRICTION_PROPERTY, localizedString("Friction")));

        box.getChildren().add(createHBoxWithLabel(localizedString("Points")));

        // Positions: array of 2-element decimal arrays
        pointRegion = new VBox(5);
        box.getChildren().add(pointRegion);

        pane.getChildren().add(box);
    }

    @Override protected void updateCustomControlValues() {
        List<List<Object>> positions =
                (List)this.getPropertyContainer().getProperty(EditableWallPathElement.POSITIONS_PROPERTY);
        if (positions == null) positions = Collections.emptyList();

        while (pointRows.size() < positions.size()) {
            addPointRow();
        }
        while (pointRows.size() < positions.size()) {
            removePointRow(pointRows.get(pointRows.size()-1));
        }
        for (int i=0; i<positions.size(); i++) {
            List<Object> pos = positions.get(i);
            List<TextField> textFields = pointRows.get(i).textFields;
            for (int j=0; j<2; j++) {
                textFields.get(j).setText(Utils.toFormattedNumber(pos.get(j)));
            }
        }
    }

    void addPointRow() {
        PointRow row = new PointRow();
        row.region = new HBox(5);
        row.textFields = new ArrayList<>();
        for (int i=0; i<2; i++) {
            DecimalTextField field = new DecimalTextField();
            field.setPrefWidth(80);
            field.setChangeHandler(this::updatePointsList);

            row.region.getChildren().add(field);
            row.textFields.add(field);
        }
        Button insertButton = new Button(localizedString("Insert"));
        insertButton.setOnAction((event) -> insertPointRowBelow(row));
        row.region.getChildren().add(insertButton);

        Button removeButton = new Button(localizedString("Remove"));
        removeButton.setOnAction((event) -> removePointRow(row));
        row.region.getChildren().add(removeButton);

        pointRows.add(row);
        pointRegion.getChildren().add(row.region);
    }

    void removePointRow(PointRow row) {
        if (pointRows.size() <= 2) return;

        pointRegion.getChildren().remove(row.region);
        pointRows.remove(row);
        updatePointsList();
    }

    void insertPointRowBelow(PointRow row) {
        int index = pointRows.indexOf(row);
        if (index < 0) return;

        List<List<Object>> newPositions = null;
        List<List<Object>> positions =
                (List)this.getPropertyContainer().getProperty(EditableWallPathElement.POSITIONS_PROPERTY);
        if (positions==null || positions.size()==0) {
            // This shouldn't happen; add two points so there's a line.
            newPositions = Arrays.asList(
                    Arrays.asList("-0.5", "-0.5"),
                    Arrays.asList("-0.5", "0.5"));
        }
        else if (positions.size()==1) {
            List<Object> pos = positions.get(0);
            newPositions = Arrays.asList(
                    Arrays.asList(pos.get(0).toString(), pos.get(1).toString()),
                    Arrays.asList(pos.get(0), 1 + String.valueOf(asDouble(pos.get(1)))));
        }
        else if (index < positions.size() - 1) {
            // Split at midpoint of row to insert after and the following point.
            newPositions = new ArrayList<>();
            for (int i=0; i<=index; i++) {
                newPositions.add(positions.get(i));
            }
            List<Object> start = positions.get(index);
            List<Object> end = positions.get(index+1);
            newPositions.add(Arrays.asList(
                    String.valueOf((asDouble(start.get(0)) + asDouble(end.get(0))) / 2),
                    String.valueOf((asDouble(start.get(1)) + asDouble(end.get(1))) / 2)));
            // Add remainder.
            for (int i=index+1; i<positions.size(); i++) {
                newPositions.add(positions.get(i));
            }
        }
        else {
            // Extend past last point in the same direction as from the second-to-last.
            List<Object> start = positions.get(positions.size()-2);
            List<Object> midpoint = positions.get(positions.size()-1);
            double dx = asDouble(midpoint.get(0)) - asDouble(start.get(0));
            double dy = asDouble(midpoint.get(1)) - asDouble(start.get(1));
            newPositions = new ArrayList<>(positions);
            newPositions.add(Arrays.asList(asDouble(midpoint.get(0)) + dx, asDouble(midpoint.get(1)) + dy));
        }
        getPropertyContainer().setProperty(EditableWallPathElement.POSITIONS_PROPERTY, newPositions);
        notifyChanged();
    }

    void updatePointsList() {
        List<List<String>> newPositions = new ArrayList<>();
        for (int i=0; i<pointRows.size(); i++) {
            List<TextField> fields = pointRows.get(i).textFields;
            newPositions.add(Arrays.asList(fields.get(0).getText(), fields.get(1).getText()));
        }
        getPropertyContainer().setProperty(EditableWallPathElement.POSITIONS_PROPERTY, newPositions);
        notifyChanged();
    }
}
