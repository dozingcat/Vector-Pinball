package com.dozingcatsoftware.vectorpinball.editor.inspector;

import java.text.DecimalFormat;

public class Utils {

    static DecimalFormat DEFAULT_DECIMAL_FORMATTER = new DecimalFormat("#.####");

    public static String toFormattedNumber(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Double || value instanceof Float) {
            return DEFAULT_DECIMAL_FORMATTER.format(value);
        }
        return value.toString();
    }
}
