package com.dozingcatsoftware.vectorpinball.editor.inspector;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

public class ColorPropertyEditor extends PropertyEditor<List<Number>> {
    public static int DEFAULT_TEXTFIELD_WIDTH = 100;

    Pane container;
    HexColorTextField textField;
    Region colorBox;

    // Allows lengths of 3 to 8, but only 3, 4, 6, and 8 are valid.
    static final Pattern COLOR_REGEX = Pattern.compile("[0-9a-fA-F]{3,8}");
    static final Pattern COLOR_REGEX_PARTIAL = Pattern.compile("[0-9a-fA-F]{1,8}");

    public ColorPropertyEditor() {
        HBox box = new HBox(5);
        box.setAlignment(Pos.CENTER_LEFT);
        this.textField = new HexColorTextField();
        this.textField.setPrefWidth(DEFAULT_TEXTFIELD_WIDTH);
        this.textField.setChangeHandler(this::runChangeHandler);
        box.getChildren().add(textField);

        colorBox = new Region();
        colorBox.setPrefWidth(20);
        colorBox.setPrefHeight(20);
        colorBox.setMaxHeight(20);
        colorBox.setBorder(new Border(
                new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1))));
        colorBox.setVisible(false);
        box.getChildren().add(colorBox);

        setContainer(box);
    }

    @Override public List<Number> getValue() {
        String text = textField.getText();
        if (text == null || !COLOR_REGEX.matcher(text).matches()) {
            return null;
        }

        switch (text.length()) {
            case 3:
                return Arrays.asList(
                        17 * Integer.parseInt(text.substring(0, 1), 16),
                        17 * Integer.parseInt(text.substring(1, 2), 16),
                        17 * Integer.parseInt(text.substring(2, 3), 16));
            case 4:
                return Arrays.asList(
                        17 * Integer.parseInt(text.substring(0, 1), 16),
                        17 * Integer.parseInt(text.substring(1, 2), 16),
                        17 * Integer.parseInt(text.substring(2, 3), 16),
                        17 * Integer.parseInt(text.substring(3, 4), 16));
            case 6:
                return Arrays.asList(
                        Integer.parseInt(text.substring(0, 2), 16),
                        Integer.parseInt(text.substring(2, 4), 16),
                        Integer.parseInt(text.substring(4, 6), 16));
            case 8:
                return Arrays.asList(
                        Integer.parseInt(text.substring(0, 2), 16),
                        Integer.parseInt(text.substring(2, 4), 16),
                        Integer.parseInt(text.substring(4, 6), 16),
                        Integer.parseInt(text.substring(6, 8), 16));
            default:
                return null;
        }
    }

    @Override public void updateFromValue(List<Number> value) {
        if (value==null) {
            textField.setText("");
            colorBox.setVisible(false);
        }
        else if (value.size() == 3) {
            textField.setText(String.format("%02x%02x%02x",
                    value.get(0), value.get(1), value.get(2)));
            Color color = Color.rgb(value.get(0).intValue(), value.get(1).intValue(), value.get(2).intValue());
            colorBox.setBackground(new Background(new BackgroundFill(color, null, null)));
            colorBox.setVisible(true);
        }
        else if (value.size() == 4) {
            textField.setText(String.format("%02x%02x%02x%02x",
                    value.get(0), value.get(1), value.get(2), value.get(3)));
            Color color = Color.rgb(value.get(0).intValue(), value.get(1).intValue(), value.get(2).intValue(),
                    value.get(3).intValue() / 255.0);
            colorBox.setBackground(new Background(new BackgroundFill(color, null, null)));
            colorBox.setVisible(true);
        }
        else {
            textField.setText("");
            colorBox.setVisible(false);
        }
    }

    static class HexColorTextField extends ConstrainedTextField {
        @Override boolean isTextValid(String text) {
            if (text==null || text.length()==0) return true;
            return COLOR_REGEX_PARTIAL.matcher(text).matches();
        }
    }
}
