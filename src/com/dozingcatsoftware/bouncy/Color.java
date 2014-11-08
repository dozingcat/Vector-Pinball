package com.dozingcatsoftware.bouncy;

import java.util.List;

/**
 * An immutable RGB color.
 */
// TODO: cache instances.
public class Color {
    public final int red, green, blue;
    private Color inverse;

    private Color(int r, int g, int b, Color inverse) {
        this.red = r;
        this.green = g;
        this.blue = b;
        if (inverse == null) {
            inverse = new Color(255 - r, 255 - g, 255 - b, this);
        }
        this.inverse = inverse;
    }

    public static Color fromRGB(int r, int g, int b) {
        return new Color(r, g, b, null);
    }

    public static Color fromList(List<Integer> rgb) {
        return fromRGB(rgb.get(0), rgb.get(1), rgb.get(2));
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
                (int) (this.blue + (other.blue - this.blue) * fraction));
    }
}
