package com.dozingcatsoftware.vectorpinball.elements;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class WallPathGeometryTest {

    static final double EPSILON = 1e-6;

    static List<List<?>> positions(List<?>... points) {
        return Arrays.asList(points);
    }

    static void assertPoint(WallPathGeometry g, int index, double x, double y) {
        assertEquals(x, g.getX(index), EPSILON);
        assertEquals(y, g.getY(index), EPSILON);
    }

    @Test public void testNoCurves() {
        WallPathGeometry g = WallPathGeometry.fromPositions(positions(
                Arrays.asList(0, 0), Arrays.asList("0", "10"), Arrays.asList(10.0, 10.0)));
        assertFalse(g.hasCurves());
        assertEquals(3, g.numPoints());
        assertPoint(g, 0, 0, 0);
        assertPoint(g, 1, 0, 10);
        assertPoint(g, 2, 10, 10);
    }

    @Test public void testEqualDistancesMakeCircularArc() {
        // Up the y axis, then right. The arc should have center (1, 9) and radius 1.
        WallPathGeometry g = WallPathGeometry.fromPositions(positions(
                Arrays.asList(0, 0), Arrays.asList(0, 10, 1), Arrays.asList(10, 10)));
        assertTrue(g.hasCurves());
        // 90 degrees at 5 degrees per segment is 18 segments, so 19 points, plus the endpoints.
        assertEquals(21, g.numPoints());
        assertPoint(g, 0, 0, 0);
        assertPoint(g, 1, 0, 9);
        assertPoint(g, 19, 1, 10);
        assertPoint(g, 20, 10, 10);
        for (int i = 1; i <= 19; i++) {
            double r = Math.hypot(g.getX(i) - 1, g.getY(i) - 9);
            assertEquals(1.0, r, EPSILON);
        }
        // Points are evenly spaced around the arc.
        double chord = 2 * Math.sin(Math.toRadians(5) / 2);
        for (int i = 2; i <= 19; i++) {
            double d = Math.hypot(g.getX(i) - g.getX(i - 1), g.getY(i) - g.getY(i - 1));
            assertEquals(chord, d, EPSILON);
        }
    }

    @Test public void testUnequalDistancesAreTangentToSegments() {
        WallPathGeometry g = WallPathGeometry.fromPositions(positions(
                Arrays.asList(0, 0), Arrays.asList(0, 10, 2, 0.5), Arrays.asList(10, 10)));
        int n = g.numPoints();
        assertPoint(g, 1, 0, 8);
        assertPoint(g, n - 2, 0.5, 10);
        // The curve starts heading along the incoming segment (+y) and ends heading along the
        // outgoing segment (+x). The direction of the first and last line segments should be close.
        double startAngle = Math.atan2(g.getY(2) - g.getY(1), g.getX(2) - g.getX(1));
        double endAngle = Math.atan2(g.getY(n - 2) - g.getY(n - 3), g.getX(n - 2) - g.getX(n - 3));
        assertEquals(Math.PI / 2, startAngle, Math.toRadians(5));
        assertEquals(0, endAngle, Math.toRadians(5));
        // All curve points are inside the triangle formed by the corner and the tangent points.
        for (int i = 1; i < n - 1; i++) {
            assertTrue(g.getX(i) >= -EPSILON && g.getY(i) <= 10 + EPSILON);
            // Line from (0, 8) to (0.5, 10): points are on or above it.
            assertTrue(g.getY(i) >= 8 + 4 * g.getX(i) - EPSILON);
        }
    }

    @Test public void testDistancesAreScaledToFitSegment() {
        // Both corners want 8 units of the middle segment, which is only 10 long.
        WallPathGeometry g = WallPathGeometry.fromPositions(positions(
                Arrays.asList(0, 0), Arrays.asList(0, 10, 1, 8), Arrays.asList(10, 10, 8, 1),
                Arrays.asList(10, 0)));
        int n = g.numPoints();
        // Curves meet in the middle of the top segment with no duplicate point.
        int middleCount = 0;
        for (int i = 0; i < n; i++) {
            if (Math.abs(g.getX(i) - 5) < EPSILON && Math.abs(g.getY(i) - 10) < EPSILON) {
                middleCount++;
            }
        }
        assertEquals(1, middleCount);
        for (int i = 1; i < n; i++) {
            assertTrue(Math.hypot(g.getX(i) - g.getX(i - 1), g.getY(i) - g.getY(i - 1)) > EPSILON);
        }
    }

    @Test public void testDistancesIgnoredAtEndpointsAndStraightCorners() {
        WallPathGeometry g = WallPathGeometry.fromPositions(positions(
                Arrays.asList(0, 0, 1), Arrays.asList(0, 5, 1), Arrays.asList(0, 10, 1)));
        assertFalse(g.hasCurves());
        assertEquals(3, g.numPoints());
    }

    @Test public void testZeroDistanceOnOneSideLeavesSharpCorner() {
        WallPathGeometry g = WallPathGeometry.fromPositions(positions(
                Arrays.asList(0, 0), Arrays.asList(0, 10, 0, 1), Arrays.asList(10, 10)));
        assertFalse(g.hasCurves());
        assertEquals(3, g.numPoints());
    }
}
