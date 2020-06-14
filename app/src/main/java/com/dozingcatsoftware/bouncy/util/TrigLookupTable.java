package com.dozingcatsoftware.bouncy.util;

import java.util.Arrays;

public class TrigLookupTable {
    private static final double TAU = 2 *  Math.PI;

    public static class SinCosValues {
        private float[] sinValues;
        private float[] cosValues;

        SinCosValues(int n) {
            sinValues = new float[n];
            cosValues = new float[n];
            for (int i = 0; i < n; i++) {
                sinValues[i] = (float) Math.sin(TAU * i / n);
                cosValues[i] = (float) Math.cos(TAU * i / n);
            }
        }

        public int size() {
            return sinValues.length;
        }

        public float sinAtIndex(int i) {
            return sinValues[i];
        }

        public float cosAtIndex(int i) {
            return cosValues[i];
        }
    }

    private int[] sizes;
    private SinCosValues[] values;

    public TrigLookupTable(int... sizes) {
        Arrays.sort(sizes);
        this.sizes = sizes;
        this.values = new SinCosValues[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            this.values[i] = new SinCosValues(sizes[i]);
        }
    }

    public SinCosValues valuesWithSizeAtLeast(int minSize) {
        for (int i = 0; i < sizes.length; i++) {
            if (sizes[i] >= minSize) {
                return values[i];
            }
        }
        return values[values.length - 1];
    }
}
