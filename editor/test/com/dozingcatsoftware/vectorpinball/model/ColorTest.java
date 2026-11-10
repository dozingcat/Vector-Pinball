package com.dozingcatsoftware.vectorpinball.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ColorTest {

    @Test void testCreate() {
        int c = Color.fromRGB(10, 20, 30);
        assertEquals(10, Color.getRed(c));
        assertEquals(20, Color.getGreen(c));
        assertEquals(30, Color.getBlue(c));
        assertEquals(255, Color.getAlpha(c));
    }

    @Test void testCreateWithAlpha() {
        int c = Color.fromRGBA(10, 20, 30, 40);
        assertEquals(10, Color.getRed(c));
        assertEquals(20, Color.getGreen(c));
        assertEquals(30, Color.getBlue(c));
        assertEquals(40, Color.getAlpha(c));
    }

    @Test void testCreateFromList() {
        int c = Color.fromList(Arrays.asList(100, 150, 200));
        assertEquals(100, Color.getRed(c));
        assertEquals(150, Color.getGreen(c));
        assertEquals(200, Color.getBlue(c));
        assertEquals(255, Color.getAlpha(c));
    }

    @Test void testCreateFromListWithAlpha() {
        int c = Color.fromList(Arrays.asList(100, 150, 200, 222));
        assertEquals(100, Color.getRed(c));
        assertEquals(150, Color.getGreen(c));
        assertEquals(200, Color.getBlue(c));
        assertEquals(222, Color.getAlpha(c));
    }

    @Test void testInverse() {
        int c = Color.fromRGBA(0, 100, 200, 111);
        int inv = Color.inverse(c);
        assertEquals(Color.fromRGBA(255, 155, 55, 111), inv);
        assertEquals(c, Color.inverse(inv));
    }

    @Test void testWithAlpha() {
        int c = Color.fromRGBA(0, 100, 200, 111);
        int ca = Color.withAlpha(c, 123);
        assertEquals(Color.fromRGBA(0, 100, 200, 123), ca);
    }

    @Test void testBlend() {
        int start = Color.fromRGBA(0, 100, 200, 255);
        int end = Color.fromRGBA(100, 110, 190, 155);
        int b0 = Color.blend(start, end, 0);
        assertEquals(start, b0);
        int b1 = Color.blend(start, end, 1);
        assertEquals(end, b1);
        int b20 = Color.blend(start, end, 0.2);
        assertEquals(Color.fromRGBA(20, 102, 198, 235), b20);
    }
}
