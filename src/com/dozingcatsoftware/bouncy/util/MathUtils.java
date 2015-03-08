package com.dozingcatsoftware.bouncy.util;

import java.util.ArrayList;
import java.util.List;

public class MathUtils {

    public static final double TAU = 2 * Math.PI;

    public static float asFloat(Object obj, float defvalue) {
        if (obj instanceof Number) return ((Number)obj).floatValue();
        if (obj instanceof String) {
            try {
                return Float.parseFloat((String)obj);
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
        List<Float> converted = new ArrayList<Float>();
        for (int i=0; i<values.size(); i++) {
            converted.add(asFloat(values.get(i)));
        }
        return converted;
    }

    public static double asDouble(Object obj, double defvalue) {
        if (obj instanceof Number) return ((Number)obj).doubleValue();
        if (obj instanceof String) {
            try {
                return Double.parseDouble((String)obj);
            }
            catch (NumberFormatException ex) {
                // log?
            }
        }
        return defvalue;
    }

    public static double asDouble(Object obj) {
        return asDouble(obj, 0);
    }

    public static List<Double> asDoubleList(List<?> values) {
        if (values == null) return null;
        List<Double> converted = new ArrayList<Double>();
        for (int i=0; i<values.size(); i++) {
            converted.add(asDouble(values.get(i)));
        }
        return converted;
    }

    public static int asInt(Object obj, int defvalue) {
        if (obj instanceof Number) return ((Number)obj).intValue();
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String)obj);
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

    public static List<Integer> asIntList(List<?> values) {
        if (values == null) return null;
        List<Integer> converted = new ArrayList<Integer>();
        for (int i=0; i<values.size(); i++) {
            converted.add(asInt(values.get(i)));
        }
        return converted;
    }

    public static float toRadians(float degrees) {
        return (float)(TAU/360) * degrees;
    }

    public static float toDegrees(float radians) {
        return (float)(radians * 360/TAU);
    }

    public static double toRadians(double degrees) {
        return (TAU/360) * degrees;
    }

    public static double toDegrees(double radians) {
        return (radians * 360/TAU);
    }

    public static float clamp(float x, float min, float max) {
        if (x < min) return min;
        if (x > max) return max;
        return x;
    }
}
