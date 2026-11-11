package com.dozingcatsoftware.vectorpinball.editor.inspector;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import com.dozingcatsoftware.vectorpinball.util.MathUtils;

/**
 * Editor with possibly multiple rows of text fields. Its value is an array of decimal strings.
 * Unlike most editors, this handles adding the labels itself because its container spans
 * multiple inspector rows.
 */
public class MultiRowDecimalArrayEditor extends PropertyEditor<List<Object>> {

    List<DecimalTextField> textFields;

    public MultiRowDecimalArrayEditor (List<String> rowLabels, int numFieldsPerRow, int numRows) {
        VBox vbox = new VBox();
        textFields = new ArrayList<DecimalTextField>(numFieldsPerRow * numRows);

        for (int row=0; row<numRows; row++) {
            HBox hbox = ElementInspector.createHBoxWithLabel(rowLabels.get(row));
            for (int i=0; i<numFieldsPerRow; i++) {
                DecimalTextField field = new DecimalTextField();
                field.setChangeHandler(this::runChangeHandler);
                textFields.add(field);
                hbox.getChildren().add(field);
            }
            vbox.getChildren().add(hbox);
        }
        setContainer(vbox);
    }

    @Override List<Object> getValue() {
        // Should check for valid values.
        return textFields.stream().map(DecimalTextField::getText).collect(Collectors.toList());
    }

    @Override void updateFromValue(List<Object> value) {
        if (value == null) return;
        for (int i=0; i<value.size(); i++) {
            Object val = (value!=null && value.size()>i) ? value.get(i) : "";
            textFields.get(i).setText(Utils.toFormattedNumber(val));
        }
    }
}
