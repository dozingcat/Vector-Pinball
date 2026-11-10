package com.dozingcatsoftware.vectorpinball.editor.inspector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableFieldElement;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableRolloverGroupElement;

import static com.dozingcatsoftware.vectorpinball.util.Localization.localizedString;

public class RolloverGroupElementInspector extends ElementInspector {

    static class RolloverRegion {
        Pane container;
        MultiRowDecimalArrayEditor positionEditor;
        DecimalStringPropertyEditor radiusEditor;
        ColorPropertyEditor colorEditor;
        IntegerPropertyEditor scoreEditor;

        @SuppressWarnings("unchecked")
        void updateFromRollover(Map<String, Object> rollover) {
            positionEditor.updateFromValue((List<Object>) rollover.get(EditableRolloverGroupElement.POSITION_PROPERTY));
            radiusEditor.updateFromValue(rollover.get(EditableRolloverGroupElement.RADIUS_PROPERTY));
            colorEditor.updateFromValue((List<Number>) rollover.get(EditableRolloverGroupElement.COLOR_PROPERTY));
            scoreEditor.updateFromValue((Number) rollover.get(EditableRolloverGroupElement.SCORE_PROPERTY));
        }

        static void putIfNotNull(Map<String, Object> map, String key, Object value) {
            if (value != null) map.put(key, value);
        }

        Map<String, Object> toRolloverMap() {
            Map<String, Object> rollover = new HashMap<>();
            putIfNotNull(rollover, EditableRolloverGroupElement.POSITION_PROPERTY, positionEditor.getValue());
            putIfNotNull(rollover, EditableRolloverGroupElement.RADIUS_PROPERTY, radiusEditor.getValue());
            putIfNotNull(rollover, EditableRolloverGroupElement.COLOR_PROPERTY, colorEditor.getValue());
            putIfNotNull(rollover, EditableRolloverGroupElement.SCORE_PROPERTY, scoreEditor.getValue());
            return rollover;
        }
    }

    Pane rolloverContainer;
    List<RolloverRegion> rolloverRegions = new ArrayList<>();

    @Override public String getLabel() {
        return "Rollovers";
    }

    @Override void drawInPane(Pane pane) {
        VBox box = new VBox();
        box.getChildren().add(createStringFieldWithLabel(
                EditableRolloverGroupElement.ID_PROPERTY, "ID"));
        box.getChildren().add(createIntegerFieldWithLabel(
                EditableFieldElement.LAYER_PROPERTY, "Layer"));
        box.getChildren().add(createColorSelectorWithLabel(
                EditableRolloverGroupElement.COLOR_PROPERTY, "Default color"));
        box.getChildren().add(createColorSelectorWithLabel(
                EditableFieldElement.INACTIVE_LAYER_COLOR_PROPERTY, "Inactive layer"));
        box.getChildren().add(createDecimalStringFieldWithLabel(
                EditableRolloverGroupElement.RADIUS_PROPERTY, "Default radius"));
        box.getChildren().add(createIntegerFieldWithLabel(
                EditableRolloverGroupElement.SCORE_PROPERTY, "Score"));
        box.getChildren().add(createBooleanCheckBoxFieldWithLabel(
                EditableRolloverGroupElement.TOGGLE_OFF_PROPERTY, "Toggle on/off"));
        box.getChildren().add(createBooleanCheckBoxFieldWithLabel(
                EditableRolloverGroupElement.CYCLE_ON_FLIPPER_PROPERTY, "Cycle on flipper"));
        box.getChildren().add(createBooleanCheckBoxFieldWithLabel(
                EditableRolloverGroupElement.IGNORE_BALL_PROPERTY, "Ignore ball"));
        box.getChildren().add(createDecimalStringFieldWithLabel(
                EditableRolloverGroupElement.RESET_DELAY_PROPERTY, "Reset delay"));

        box.getChildren().add(createVerticalSpacer(10));

        Label rolloversLabel = new Label("Rollovers");
        rolloversLabel.setFont(new Font(14));
        box.getChildren().add(rolloversLabel);
        rolloverContainer = new VBox();
        box.getChildren().add(rolloverContainer);

        Button addButton = new Button("Add rollover");
        addButton.setOnAction((event) -> addNewRollover());
        box.getChildren().add(addButton);
        pane.getChildren().add(box);
    }

    @Override protected void updateCustomControlValues() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rollovers =
        (List<Map<String, Object>>) this.getPropertyContainer().getProperty(EditableRolloverGroupElement.ROLLOVERS_PROPERTY);
        if (rollovers == null) rollovers = Collections.emptyList();

        while (rollovers.size() > rolloverRegions.size()) {
            createRolloverRegion();
        }
        while (rollovers.size() < rolloverRegions.size()) {
            removeRolloverRegion(rolloverRegions.get(rolloverRegions.size()-1));
        }

        for (int i=0; i<rollovers.size(); i++) {
            rolloverRegions.get(i).updateFromRollover(rollovers.get(i));
        }
    }

    void addNewRollover() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rollovers =
        (List<Map<String, Object>>) this.getPropertyContainer().getProperty(EditableRolloverGroupElement.ROLLOVERS_PROPERTY);
        List<Map<String, Object>> newRollovers = new ArrayList<>();
        if (rollovers != null) {
            newRollovers.addAll(rollovers);
        }
        Map<String, Object> newRollover = new HashMap<>();
        newRollover.put(EditableRolloverGroupElement.POSITION_PROPERTY, Arrays.asList("0", "0"));
        newRollovers.add(newRollover);

        this.getPropertyContainer().setProperty(EditableRolloverGroupElement.ROLLOVERS_PROPERTY, newRollovers);
        notifyChanged();
    }

    void removeRolloverRegion(RolloverRegion region) {
        rolloverRegions.remove(region);
        rolloverContainer.getChildren().remove(region);
        updateRolloversList();
    }

    HBox createRolloverHBoxWithLabelAndEditor(String label, PropertyEditor<?> editor) {
        HBox box = createHBoxWithLabel(label);
        editor.setOnChange(this::updateRolloversList);
        box.getChildren().add(editor.getContainer());
        return box;
    }

    void createRolloverRegion() {
        RolloverRegion region = new RolloverRegion();
        VBox vbox = new VBox();
        region.container = vbox;

        region.colorEditor = new ColorPropertyEditor();
        vbox.getChildren().add(createRolloverHBoxWithLabelAndEditor("Color", region.colorEditor));

        region.positionEditor = new MultiRowDecimalArrayEditor(Arrays.asList("Position"), 2, 1);
        region.positionEditor.setOnChange(this::updateRolloversList);
        vbox.getChildren().add(region.positionEditor.getContainer());

        region.radiusEditor = new DecimalStringPropertyEditor();
        vbox.getChildren().add(createRolloverHBoxWithLabelAndEditor("Radius", region.radiusEditor));

        region.scoreEditor = new IntegerPropertyEditor();
        vbox.getChildren().add(createRolloverHBoxWithLabelAndEditor("Score", region.scoreEditor));

        Button removeButton = new Button("Remove");
        removeButton.setOnAction((event) -> removeRolloverRegion(region));
        vbox.getChildren().add(removeButton);

        vbox.getChildren().add(createVerticalSpacer(20));

        rolloverContainer.getChildren().add(vbox);
        rolloverRegions.add(region);
    }

    void updateRolloversList() {
        List<Map<String, Object>> newRollovers =
                rolloverRegions.stream().map(RolloverRegion::toRolloverMap).collect(Collectors.toList());
        getPropertyContainer().setProperty(EditableRolloverGroupElement.ROLLOVERS_PROPERTY, newRollovers);
        notifyChanged();
    }
}
