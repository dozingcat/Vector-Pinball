package com.dozingcatsoftware.vectorpinball.editor.elements;

import java.util.Arrays;
import java.util.Map;

import com.dozingcatsoftware.vectorpinball.editor.IEditableFieldRenderer;
import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.editor.Point;

public class EditableWallArcElement extends EditableFieldElement {

    public static final String CENTER_PROPERTY = "center";
    public static final String RADIUS_PROPERTY = "radius";
    public static final String X_RADIUS_PROPERTY = "xradius";
    public static final String Y_RADIUS_PROPERTY = "yradius";
    public static final String NUM_SEGMENTS_PROPERTY = "segments";
    public static final String MIN_ANGLE_PROPERTY = "minangle";
    public static final String MAX_ANGLE_PROPERTY = "maxangle";
    public static final String IGNORE_BALL_PROPERTY = "ignoreBall";
    public static final String RESTITUTION_PROPERTY = "restitution";
    public static final String FRICTION_PROPERTY = "friction";

    // Computes line segments approximating the circular arc, derived from the element's
    // properties (center, radii, angles, segment count). The property map is the source of truth;
    // this is recomputed on demand rather than cached.
    private double[][] computeLineSegments() {
        Point center = getPointProperty(CENTER_PROPERTY);
        double centerX = center.x;
        double centerY = center.y;
        double minAngle = Math.toRadians(getDoubleProperty(MIN_ANGLE_PROPERTY));
        double maxAngle = Math.toRadians(getDoubleProperty(MAX_ANGLE_PROPERTY));
        int numSegments = getIntProperty(NUM_SEGMENTS_PROPERTY, 5);

        double xRadius, yRadius;
        if (hasProperty(RADIUS_PROPERTY)) {
            xRadius = yRadius = getDoubleProperty(RADIUS_PROPERTY);
        }
        else {
            xRadius = getDoubleProperty(X_RADIUS_PROPERTY);
            yRadius = getDoubleProperty(Y_RADIUS_PROPERTY);
        }

        double diff = maxAngle - minAngle;
        // Create line segments to approximate circular arc.
        double[][] lineSegments = new double[numSegments][];
        for(int i=0; i<numSegments; i++) {
            double angle1 = minAngle + i * diff / numSegments;
            double angle2 = minAngle + (i+1) * diff / numSegments;
            double x1 = centerX + xRadius * (float)Math.cos(angle1);
            double y1 = centerY + yRadius * (float)Math.sin(angle1);
            double x2 = centerX + xRadius * (float)Math.cos(angle2);
            double y2 = centerY + yRadius * (float)Math.sin(angle2);
            lineSegments[i] = (new double[] {x1, y1, x2, y2});
        }
        return lineSegments;
    }

    @Override protected void addPropertiesForNewElement(Map<String, Object> props, EditableField field) {
        props.put(RADIUS_PROPERTY, "0.5");
        props.put(CENTER_PROPERTY, Arrays.asList("-0.5", "0"));
        props.put(MIN_ANGLE_PROPERTY, "0");
        props.put(MAX_ANGLE_PROPERTY, "135");
    }

    @Override public void drawForEditor(IEditableFieldRenderer renderer, boolean isSelected) {
        double[][] lineSegments = computeLineSegments();
        int color = currentColor(DEFAULT_WALL_COLOR);
        for (double[] segment : lineSegments) {
            renderer.drawLine(segment[0], segment[1], segment[2], segment[3], color);
        }
        if (isSelected) {
            double endpointRadius = 0.25 / renderer.getRelativeScale();
            renderer.fillCircle(lineSegments[0][0], lineSegments[0][1], endpointRadius, color);
            double[] last = lineSegments[lineSegments.length-1];
            renderer.fillCircle(last[2], last[3], endpointRadius, color);

            int colorWithAlpha = Color.withAlpha(color, Color.getAlpha(color) / 2);
            Point center = getPointProperty(CENTER_PROPERTY);
            renderer.fillCircle(center.x, center.y, endpointRadius, colorWithAlpha);
            renderer.drawLine(center.x, center.y, lineSegments[0][0], lineSegments[0][1], colorWithAlpha);
            renderer.drawLine(center.x, center.y, last[2], last[3], colorWithAlpha);
        }
    }

    @Override public boolean isPointWithinDistance(Point point, double distance) {
        for (double[] segment : computeLineSegments()) {
            if (point.distanceToLineSegment(segment[0], segment[1], segment[2], segment[3]) <= distance) {
                return true;
            }
        }
        return false;
    }

    @Override public void translate(Point offset) {
        setPointProperty(CENTER_PROPERTY, getPointProperty(CENTER_PROPERTY).add(offset));
    }

}
