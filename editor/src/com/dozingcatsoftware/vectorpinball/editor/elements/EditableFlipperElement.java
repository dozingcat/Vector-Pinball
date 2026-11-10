package com.dozingcatsoftware.vectorpinball.editor.elements;

import static com.dozingcatsoftware.vectorpinball.util.MathUtils.TAU;

import java.util.Arrays;
import java.util.Map;

import com.dozingcatsoftware.vectorpinball.editor.IEditableFieldRenderer;
import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.editor.Point;

public class EditableFlipperElement extends EditableFieldElement {

    public static final String POSITION_PROPERTY = "position";
    public static final String LENGTH_PROPERTY = "length";
    public static final String MIN_ANGLE_PROPERTY = "minangle";
    public static final String MAX_ANGLE_PROPERTY = "maxangle";
    public static final String UP_SPEED_PROPERTY = "upspeed";
    public static final String DOWN_SPEED_PROPERTY = "downspeed";

    static final int DEFAULT_COLOR = Color.fromRGB(0, 255, 0);

    @Override protected void addPropertiesForNewElement(Map<String, Object> props, EditableField field) {
        props.put(POSITION_PROPERTY, Arrays.asList(0, 0));
        props.put(LENGTH_PROPERTY, "2.5");
        props.put(MIN_ANGLE_PROPERTY, "-20");
        props.put(MAX_ANGLE_PROPERTY, "20");
        props.put(UP_SPEED_PROPERTY, "7");
        props.put(DOWN_SPEED_PROPERTY, "3");
    }

    private Point position() {
        return getPointProperty(POSITION_PROPERTY);
    }

    // Negative if flipper rotates around its right end.
    private double flipperLength() {
        return getDoubleProperty(LENGTH_PROPERTY);
    }

    private double minAngle() {
        return Math.toRadians(getDoubleProperty(MIN_ANGLE_PROPERTY));
    }

    Point endPoint() {
        Point position = position();
        double length = flipperLength();
        if (length > 0) {
            return Point.fromXY(
                    position.x + length*Math.cos(minAngle()),
                    position.y + length*Math.sin(minAngle()));
        }
        else {
            return Point.fromXY(
                    position.x - length*Math.cos(TAU/2 - minAngle()),
                    position.y - length*Math.sin(TAU/2 - minAngle()));
        }
    }

    @Override public void drawForEditor(IEditableFieldRenderer renderer, boolean isSelected) {
        Point position = position(), end = endPoint();
        int color = currentColor(DEFAULT_COLOR);
        renderer.drawLine(position.x, position.y, end.x, end.y, color);
        if (isSelected) {
            int colorWithAlpha = Color.withAlpha(color, Color.getAlpha(color) / 2);
            renderer.fillCircle(position.x, position.y, 0.35*renderer.getRelativeScale(), colorWithAlpha);
            renderer.fillCircle(end.x, end.y, 0.15*renderer.getRelativeScale(), colorWithAlpha);
        }
    }

    @Override public boolean isPointWithinDistance(Point point, double distance) {
        return point.distanceToLineSegment(position(), endPoint()) <= distance;
    }

    @Override public void translate(Point offset) {
        setPointProperty(POSITION_PROPERTY, position().add(offset));
    }

}
