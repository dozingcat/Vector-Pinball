package com.dozingcatsoftware.vectorpinball.editor.inspector;

public class IntegerTextField extends ConstrainedTextField {

    public static int DEFAULT_WIDTH = 100;

    Long minValue = null;
    Long maxValue = null;

    public IntegerTextField() {
        super();
        this.setPrefWidth(DEFAULT_WIDTH);
    }

    public void setMinValue(Long value) {
        minValue = value;
    }

    public void setMaxValue(Long value) {
        maxValue = value;
    }

    boolean isValidInteger(String text) {
        if (text==null || text.length()==0) return true;
        try {
            long value = Long.valueOf(text);
            return ((minValue==null || value>=minValue) && (maxValue==null || value<=maxValue));
        }
        catch (NumberFormatException ex) {
            return false;
        }
    }

    @Override boolean isTextValid(String text) {
        return isValidInteger(text);
    }

}
