package com.dozingcatsoftware.bouncy;

import java.util.List;

/**
 * An immutable RGB color.
 */
// TODO: cache instances.
public class Color {
    public final int red, green, blue, alpha;
    private Color inverse;

    private Color(int r, int g, int b, int a, Color inverse) {
        this.red = r;
        this.green = g;
        this.blue = b;
        this.alpha = a;
        if (inverse == null) {
            inverse = new Color(255 - r, 255 - g, 255 - b, a, this);
        }
        this.inverse = inverse;
    }

    public static Color fromRGB(int r, int g, int b) {
        return new Color(r, g, b, 255, null);
    }

    public static Color fromRGB(int r, int g, int b, int a) {
        return new Color(r, g, b, a, null);
    }

    public static Color fromList(List<Number> rgb) {
        if (rgb.size() == 3) {
            return fromRGB(rgb.get(0).intValue(), rgb.get(1).intValue(), rgb.get(2).intValue());
        }
        else if (rgb.size() == 4) {
            return fromRGB(rgb.get(0).intValue(), rgb.get(1).intValue(),
                    rgb.get(2).intValue(), rgb.get(3).intValue());
        }
        else {
            throw new IllegalArgumentException("Invalid color size: " + rgb.size());
        }
    }

    public Color inverted() {
        return inverse;
    }

    public Color blendedWith(Color other, double fraction) {
        if (fraction < 0) fraction = 0;
        if (fraction > 1) fraction = 1;
        return fromRGB(
                (int) (this.red + (other.red - this.red) * fraction),
                (int) (this.green + (other.green - this.green) * fraction),
                (int) (this.blue + (other.blue - this.blue) * fraction),
                (int) (this.alpha + (other.alpha - this.alpha) * fraction));
    }

    @Override public boolean equals(Object obj) {
        if (obj instanceof Color) {
            Color other = (Color)obj;
            return (red==other.red && green==other.green && blue==other.blue && alpha==other.alpha);
        }
        return false;
    }

    @Override public int hashCode() {
        return (red<<24) | (green<<16) | (blue<<8) | alpha;
    }
}
