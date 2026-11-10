package com.dozingcatsoftware.vectorpinball.editor.inspector;

import java.util.regex.Pattern;

public class DecimalTextField extends ConstrainedTextField {

    public static int DEFAULT_WIDTH = 100;

    static Pattern DECIMAL_PATTERN = Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)");

    Double minValue = null;
    Double maxValue = null;

    public DecimalTextField() {
        super();
        this.setPrefWidth(DEFAULT_WIDTH);
    }

    public void setMinValue(Double value) {
        minValue = value;
    }

    public void setMaxValue(Double value) {
        maxValue = value;
    }

    boolean isValidDecimal(String text) {
        if (text==null || text.length()==0) return true;
        if (!DECIMAL_PATTERN.matcher(text).matches()) return false;
        try {
            double value = Double.valueOf(text);
            return ((minValue==null || value>=minValue) && (maxValue==null || value<=maxValue));
        }
        catch (NumberFormatException ex) {
            return false;
        }
    }

    @Override boolean isTextValid(String text) {
        return isValidDecimal(text);
    }
}
