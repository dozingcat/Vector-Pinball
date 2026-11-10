package com.dozingcatsoftware.vectorpinball.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * simplejson doesn't support pretty printing, and it's important that the JSON output
 * be human readable and editable.
 */
public class JsonPrettyPrinter {
    // Use US format symbols to make sure the decimal point is "."
    private static DecimalFormat floatFormat = new DecimalFormat(
            "#.######", DecimalFormatSymbols.getInstance(Locale.US));

    public static String prettyPrint(Object obj, int indent) {
        StringBuilder buffer = new StringBuilder();
        writeValue(obj, buffer, 0, indent);
        return buffer.toString();
    }

    static void writeIndent(StringBuilder buffer, int level, int indent) {
        int spaces = level * indent;
        for (int i=0; i<spaces; i++) {
            buffer.append(' ');
        }
    }

    static void writeMap(Map<String, Object> map, StringBuilder buffer, int level, int indent) {
        buffer.append("{\n");
        int i = 0;
        List<String> sortedKeys = new ArrayList<>();
        sortedKeys.addAll(map.keySet());
        Collections.sort(sortedKeys);
        // TODO: maybe write primitive values first, then nested lists and maps.
        for (String key : sortedKeys) {
            writeIndent(buffer, level+1, indent);
            buffer.append('"');
            buffer.append(key);
            buffer.append('"');
            buffer.append(": ");
            writeValue(map.get(key), buffer, level+1, indent);
            i++;
            if (i < map.size()) buffer.append(",");
            buffer.append("\n");
        }
        writeIndent(buffer, level, indent);
        buffer.append("}");
    }

    static void writeString(String s, StringBuilder buffer) {
        // Escape backslashes and double quotes, and control characters less than U+0020.
        int len = s.length();
        for (int i=0; i<len; i++) {
            char ch = s.charAt(i);
            if (ch == '\\') {
                // Replace one backslash with two.
                buffer.append("\\\\");
            }
            else if (ch == '"') {
                // Escape quotes.
                buffer.append("\\\"");
            }
            else if (ch < 0x20) {
                // backslash-u followed by 4 digit hex value.
                buffer.append("\\u00");
                buffer.append(String.format("%02x", (int) ch));
            }
            else {
                buffer.append(ch);
            }
        }
    }

    static void writeValue(Object value, StringBuilder buffer, int level, int indent) {
        if (value instanceof Map) {
            writeMap((Map<String, Object>) value, buffer, level, indent);
        }
        else if (value instanceof List) {
            writeArray((List<Object>) value, buffer, level, indent);
        }
        else if (value instanceof String) {
            buffer.append('"');
            writeString((String) value, buffer);
            buffer.append('"');
        }
        else if ((value instanceof Float) || (value instanceof Double)) {
            buffer.append(floatFormat.format(((Number) value).doubleValue()));
        }
        else {
            buffer.append(String.valueOf(value));
        }
    }

    static void writeArray(List<Object> array, StringBuilder buffer, int level, int indent) {
        buffer.append('[');
        int len = array.size();
        if (isPrimitiveArray(array)) {
            for (int i=0; i<len; i++) {
                writeValue(array.get(i), buffer, level+1, indent);
                if (i < len-1) {
                    buffer.append(", ");
                }
            }
            buffer.append(']');
        }
        else {
            buffer.append("\n");
            for (int i=0; i<len; i++) {
                writeIndent(buffer, level+1, indent);
                writeValue(array.get(i), buffer, level+1, indent);
                if (i < len-1) {
                    buffer.append(", ");
                }
                buffer.append("\n");
            }
            writeIndent(buffer, level, indent);
            buffer.append(']');
        }
    }

    static boolean isPrimitiveArray(List<Object> array) {
        for (Object item : array) {
            if (item instanceof List || item instanceof Map) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        Map<String, Object> test = new HashMap<>();
        test.put("foo", "\"Hi\", she \\said");
        test.put("baz", 42);
        test.put("qqq", 42.0);
        test.put("yes", true);
        test.put("no", null);
        test.put("maybe", 0.5);
        test.put("primes", Arrays.asList(2, 3.0, 5, "seven", Arrays.asList(11, 13, 17)));

        Map<String, Object> nested = new HashMap<>();
        nested.put("array", Arrays.asList("1", 2, 3));
        nested.put("hi", "there");
        test.put("dict", nested);

        System.out.println(prettyPrint(test, 4));
    }
}
