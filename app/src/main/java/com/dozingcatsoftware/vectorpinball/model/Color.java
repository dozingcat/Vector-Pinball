package com.dozingcatsoftware.vectorpinball.model;

import java.util.List;

/**
 * Functions for working with colors, represented as 32-bit ints with byte order ARGB.
 */
public class Color {

    private Color() {}

    public static int fromRGB(int r, int g, int b) {
        return fromRGBA(r, g, b, 255);
    }

    public static int fromRGBA(int r, int g, int b, int a) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int fromList(List<Number> rgb) {
        if (rgb.size() == 3) {
            return fromRGB(rgb.get(0).intValue(), rgb.get(1).intValue(), rgb.get(2).intValue());
        }
        else if (rgb.size() == 4) {
            return fromRGBA(
                    rgb.get(0).intValue(), rgb.get(1).intValue(),
                    rgb.get(2).intValue(), rgb.get(3).intValue());
        }
        else {
            throw new IllegalArgumentException("Invalid color size: " + rgb.size());
        }
    }

    public static int getRed(int color) {
        return (color >> 16 ) & 0xff;
    }

    public static int getGreen(int color) {
        return (color >> 8) & 0xff;
    }

    public static int getBlue(int color) {
        return color & 0xff;
    }

    public static int getAlpha(int color) {
        return (color >> 24) & 0xff;
    }

    public static int inverse(int color) {
        return withAlpha(~color, getAlpha(color));
    }

    public static int withAlpha(int color, int a) {
        return (color & 0xffffff) | (a << 24);
    }

    public static int blend(int start, int end, double fraction) {
        if (fraction <= 0) {
            return start;
        }
        if (fraction >= 1) {
            return end;
        }
        return fromRGBA(
                (int) (getRed(start) + (getRed(end) - getRed(start)) * fraction),
                (int) (getGreen(start) + (getGreen(end) - getGreen(start)) * fraction),
                (int) (getBlue(start) + (getBlue(end) - getBlue(start)) * fraction),
                (int) (getAlpha(start) + (getAlpha(end) - getAlpha(start)) * fraction));
    }

    public static int toARGB(int color) {
        return color;
    }

    public static int toAGBR(int color) {
        int blue = (color & 0xff) << 16;
        int red = (color >> 16) & 0xff;
        return (color & 0xff00ff00) | blue | red;
    }

    public static int toRGBA(int color) {
        int alpha = (color >> 24) & 0xff;
        return (color << 8) | alpha;
    }
}
