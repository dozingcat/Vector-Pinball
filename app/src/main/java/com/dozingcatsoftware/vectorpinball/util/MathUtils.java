package com.dozingcatsoftware.vectorpinball.util;

import java.util.ArrayList;
import java.util.List;

public class MathUtils {

    public static final double TAU = 2 * Math.PI;

    public static float asFloat(Object obj, float defvalue) {
        if (obj instanceof Number) return ((Number) obj).floatValue();
        if (obj instanceof String) {
            try {
                return Float.parseFloat((String) obj);
            }
            catch (NumberFormatException ex) {
                // log?
            }
        }
        return defvalue;
    }

    public static float asFloat(Object obj) {
        return asFloat(obj, 0);
    }

    public static List<Float> asFloatList(List<?> values) {
        if (values == null) return null;
        List<Float> converted = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            converted.add(asFloat(values.get(i)));
        }
        return converted;
    }

    public static double asDouble(Object obj, double defvalue) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            }
            catch (NumberFormatException ex) {
                // log?
            }
        }
        return defvalue;
    }

    public static int asInt(Object obj, int defvalue) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            }
            catch (NumberFormatException ex) {
                // log?
            }
        }
        return defvalue;
    }

    public static int asInt(Object obj) {
        return asInt(obj, 0);
    }

    public static float toRadiansF(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    public static float clamp(float x, float min, float max) {
        if (x < min) return min;
        if (x > max) return max;
        return x;
    }
}
