package com.dozingcatsoftware.vectorpinball.elements;

import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asDouble;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the points of a wall path, optionally replacing its corners with curves. Used by
 * WallPathElement, and by the editor so that it draws paths the same way as the game.
 *
 * Each position is a list of [x, y], [x, y, distance], or [x, y, distanceBefore, distanceAfter].
 * For an interior point with nonzero distances, the corner is replaced by a curve that starts
 * `distanceBefore` units before the point on the incoming segment and ends `distanceAfter` units
 * after it on the outgoing segment. [x, y, distance] uses the same distance on both sides.
 * Distances on the first and last points are ignored, since they don't have corners, unless the
 * path is closed.
 *
 * The curve is a rational quadratic Bezier curve whose control point is the corner, which is
 * tangent to both segments where it meets them. Its weight is cos(turnAngle / 2), which makes it
 * an exact circular arc when both distances are equal. With unequal distances it's an elliptical
 * arc. The curve is approximated by line segments, about one for every DEGREES_PER_SEGMENT degrees
 * of turning.
 *
 * If the distances at both ends of a segment add up to more than its length, they're scaled down
 * so the curves meet in the middle of the segment.
 */
public class WallPathGeometry {

    static final double DEGREES_PER_SEGMENT = 5;
    // Corners that turn less than this (in radians) are left as is.
    static final double MIN_TURN_ANGLE = 1e-4;
    // Consecutive points closer than this are merged, so there are no zero-length walls.
    static final double MIN_POINT_DISTANCE = 1e-5;

    private final double[] xs;
    private final double[] ys;
    private final boolean hasCurves;

    private WallPathGeometry(double[] xs, double[] ys, boolean hasCurves) {
        this.xs = xs;
        this.ys = ys;
        this.hasCurves = hasCurves;
    }

    /** Number of points in the path. N points form N-1 line segments. */
    public int numPoints() {
        return xs.length;
    }

    public double getX(int index) {
        return xs[index];
    }

    public double getY(int index) {
        return ys[index];
    }

    /** Returns true if any corners were replaced by curves. */
    public boolean hasCurves() {
        return hasCurves;
    }

    public float[] xPointsAsFloats() {
        return toFloats(xs);
    }

    public float[] yPointsAsFloats() {
        return toFloats(ys);
    }

    private static float[] toFloats(double[] values) {
        float[] result = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (float) values[i];
        }
        return result;
    }

    /** Returns the curve distance before the corner for a position entry. */
    public static double curveDistanceBefore(List<?> position) {
        return (position.size() > 2) ? asDouble(position.get(2)) : 0;
    }

    /** Returns the curve distance after the corner for a position entry. */
    public static double curveDistanceAfter(List<?> position) {
        if (position.size() > 3) return asDouble(position.get(3));
        return (position.size() > 2) ? asDouble(position.get(2)) : 0;
    }

    public static WallPathGeometry fromPositions(List<? extends List<?>> positions) {
        return fromPositions(positions, false);
    }

    /**
     * If `closed` is true, the path has an additional segment from the last point back to the
     * first, and the first and last points are corners that can have curves like any other.
     * The returned points end with the first point repeated, so that consecutive points still
     * form the segments.
     */
    public static WallPathGeometry fromPositions(List<? extends List<?>> positions, boolean closed) {
        int n = positions.size();
        double[] px = new double[n];
        double[] py = new double[n];
        double[] distBefore = new double[n];
        double[] distAfter = new double[n];
        for (int i = 0; i < n; i++) {
            List<?> pos = positions.get(i);
            px[i] = asDouble(pos.get(0));
            py[i] = asDouble(pos.get(1));
        }
        // If a closed path explicitly ends at its first point, ignore the duplicate.
        if (closed && n > 1 &&
                Math.hypot(px[n - 1] - px[0], py[n - 1] - py[0]) < MIN_POINT_DISTANCE) {
            n--;
        }
        // A closed path needs at least 3 points to have any area.
        closed = closed && n >= 3;
        for (int i = 0; i < n; i++) {
            // Endpoints of open paths don't have corners.
            if (closed || (i > 0 && i < n - 1)) {
                List<?> pos = positions.get(i);
                distBefore[i] = Math.max(0, curveDistanceBefore(pos));
                distAfter[i] = Math.max(0, curveDistanceAfter(pos));
            }
        }
        // Scale down distances that don't fit within their segment.
        int numSegments = closed ? n : n - 1;
        for (int i = 0; i < numSegments; i++) {
            int next = (i + 1) % n;
            double length = Math.hypot(px[next] - px[i], py[next] - py[i]);
            double total = distAfter[i] + distBefore[next];
            if (total > length) {
                double scale = length / total;
                distAfter[i] *= scale;
                distBefore[next] *= scale;
            }
        }

        PointList points = new PointList();
        boolean hasCurves = false;
        for (int i = 0; i < n; i++) {
            boolean isCorner = closed || (i > 0 && i < n - 1);
            int prev = (i + n - 1) % n;
            int next = (i + 1) % n;
            if (isCorner && addCurve(
                    points, px[prev], py[prev], px[i], py[i], px[next], py[next],
                    distBefore[i], distAfter[i])) {
                hasCurves = true;
            }
            else {
                points.add(px[i], py[i]);
            }
        }
        if (closed) {
            // Close the path by returning to the first point, which is the start of the first
            // corner's curve if it has one.
            points.add(points.xs.get(0), points.ys.get(0));
        }
        return new WallPathGeometry(points.xArray(), points.yArray(), hasCurves);
    }

    /**
     * Adds points for a curve replacing the corner at (cx, cy), between the segments from
     * (x0, y0) and to (x2, y2). Returns false without adding points if there shouldn't be a curve.
     */
    private static boolean addCurve(
            PointList points, double x0, double y0, double cx, double cy, double x2, double y2,
            double distBefore, double distAfter) {
        if (distBefore <= 0 || distAfter <= 0) return false;
        double inLength = Math.hypot(cx - x0, cy - y0);
        double outLength = Math.hypot(x2 - cx, y2 - cy);
        if (inLength == 0 || outLength == 0) return false;
        double inX = (cx - x0) / inLength;
        double inY = (cy - y0) / inLength;
        double outX = (x2 - cx) / outLength;
        double outY = (y2 - cy) / outLength;
        double cosTurn = Math.max(-1, Math.min(1, inX * outX + inY * outY));
        double turnAngle = Math.acos(cosTurn);
        if (turnAngle < MIN_TURN_ANGLE) return false;

        double startX = cx - inX * distBefore;
        double startY = cy - inY * distBefore;
        double endX = cx + outX * distAfter;
        double endY = cy + outY * distAfter;
        double weight = Math.cos(turnAngle / 2);
        int numSegments = Math.max(2, (int) Math.ceil(Math.toDegrees(turnAngle) / DEGREES_PER_SEGMENT));
        // Choose t values so that the curve's direction turns by the same angle between each pair
        // of consecutive points. (Evenly spaced t values would put too few points where the curve
        // is tightest when the distances are unequal.)
        double aX = cx - startX, aY = cy - startY;
        double bX = endX - cx, bY = endY - cy;
        double orientation = Math.signum(aX * bY - aY * bX);
        for (int i = 0; i <= numSegments; i++) {
            double t = (i == 0) ? 0 : (i == numSegments) ? 1 : tForTangentAngle(
                    aX, aY, bX, bY, weight, orientation, turnAngle * i / numSegments);
            double b0 = (1 - t) * (1 - t);
            double b1 = 2 * t * (1 - t) * weight;
            double b2 = t * t;
            double denom = b0 + b1 + b2;
            points.add(
                    (b0 * startX + b1 * cx + b2 * endX) / denom,
                    (b0 * startY + b1 * cy + b2 * endY) / denom);
        }
        return true;
    }

    /**
     * Returns the t value where the curve's tangent has turned by `targetAngle` from its starting
     * direction. `a` is the vector from the start point to the corner and `b` is from the corner to
     * the end point. The tangent of the rational Bezier curve at t is proportional to
     * w(1-t)^2 a + t(1-t)(a+b) + w t^2 b, and its angle increases monotonically with t, so
     * this does a binary search.
     */
    private static double tForTangentAngle(
            double aX, double aY, double bX, double bY, double weight, double orientation,
            double targetAngle) {
        double aLength = Math.hypot(aX, aY);
        double uX = aX / aLength, uY = aY / aLength;
        double lo = 0, hi = 1;
        for (int iter = 0; iter < 50; iter++) {
            double t = (lo + hi) / 2;
            double c0 = weight * (1 - t) * (1 - t);
            double c1 = t * (1 - t);
            double c2 = weight * t * t;
            double dX = c0 * aX + c1 * (aX + bX) + c2 * bX;
            double dY = c0 * aY + c1 * (aY + bY) + c2 * bY;
            double angle = Math.atan2(orientation * (uX * dY - uY * dX), uX * dX + uY * dY);
            if (angle < targetAngle) {
                lo = t;
            }
            else {
                hi = t;
            }
        }
        return (lo + hi) / 2;
    }

    private static class PointList {
        final List<Double> xs = new ArrayList<>();
        final List<Double> ys = new ArrayList<>();

        void add(double x, double y) {
            int last = xs.size() - 1;
            if (last >= 0 && Math.hypot(x - xs.get(last), y - ys.get(last)) < MIN_POINT_DISTANCE) {
                return;
            }
            xs.add(x);
            ys.add(y);
        }

        double[] xArray() {
            return toArray(xs);
        }

        double[] yArray() {
            return toArray(ys);
        }

        private static double[] toArray(List<Double> values) {
            double[] result = new double[values.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = values.get(i);
            }
            return result;
        }
    }
}
