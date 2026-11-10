package com.dozingcatsoftware.vectorpinball.editor.elements;

import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asDouble;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.dozingcatsoftware.vectorpinball.editor.IEditableFieldRenderer;
import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.editor.Point;

public class EditableWallElement extends EditableFieldElement {

    public static final String POSITION_PROPERTY = "position";
    public static final String RESTITUTION_PROPERTY = "restitution";
    public static final String FRICTION_PROPERTY = "friction";
    public static final String KICK_PROPERTY = "kick";
    public static final String KILL_PROPERTY = "kill";
    public static final String RETRACT_WHEN_HIT_PROPERTY = "retractWhenHit";
    public static final String DISABLED_PROPERTY = "disabled";
    public static final String IGNORE_BALL_PROPERTY = "ignoreBall";

    enum DragType {
        START, END, ALL,
    }
    DragType dragType;

    @Override public void drawForEditor(IEditableFieldRenderer renderer, boolean isSelected) {
        int color = currentColor(DEFAULT_WALL_COLOR);
        double selectionCircleRadius = 0.25 / renderer.getRelativeScale();
        List<?> pos = (List<?>)getProperty(POSITION_PROPERTY);
        double x1 = asDouble(pos.get(0));
        double y1 = asDouble(pos.get(1));
        double x2 = asDouble(pos.get(2));
        double y2 = asDouble(pos.get(3));
        renderer.drawLine(x1, y1, x2, y2, color);
        if (isSelected) {
            // TODO: adjust selection size for zoom level, needs method on IFieldRenderer.
            int colorWithAlpha = Color.withAlpha(color, Color.getAlpha(color) / 2);
            renderer.fillCircle(x1, y1, selectionCircleRadius, colorWithAlpha);
            renderer.fillCircle(x2, y2, selectionCircleRadius, colorWithAlpha);
        }
    }

    @Override protected void addPropertiesForNewElement(Map<String, Object> props, EditableField field) {
        props.put(POSITION_PROPERTY, Arrays.asList("-0.5", "0", "-0.5", "2"));
    }

    @Override public boolean isPointWithinDistance(Point point, double distance) {
        List<?> pos = (List<?>)getProperty(POSITION_PROPERTY);
        Point start = Point.fromXY(asDouble(pos.get(0)), asDouble(pos.get(1)));
        Point end = Point.fromXY(asDouble(pos.get(2)), asDouble(pos.get(3)));
        return point.distanceToLineSegment(start, end) <= distance;
    }

    @Override public void startDrag(Point point) {
        List<?> pos = (List<?>)getProperty(POSITION_PROPERTY);
        double toStart = point.distanceTo(asDouble(pos.get(0)), asDouble(pos.get(1)));
        double toEnd = point.distanceTo(asDouble(pos.get(2)), asDouble(pos.get(3)));
        if (5*toStart < toEnd) {
            dragType = DragType.START;
        }
        else if (5*toEnd < toStart) {
            dragType = DragType.END;
        }
        else {
            dragType = DragType.ALL;
        }
    }
    @Override public void handleDrag(Point point, Point deltaFromStart, Point deltaFromPrevious) {
        List<?> pos = (List<?>)getProperty(POSITION_PROPERTY);
        List<Object> newPos = new ArrayList<>(pos);
        switch (dragType) {
        case START:
            newPos.set(0, asDouble(pos.get(0)) + deltaFromPrevious.x);
            newPos.set(1, asDouble(pos.get(1)) + deltaFromPrevious.y);
            break;
        case END:
            newPos.set(2, asDouble(pos.get(2)) + deltaFromPrevious.x);
            newPos.set(3, asDouble(pos.get(3)) + deltaFromPrevious.y);
            break;
        case ALL:
            newPos.set(0, asDouble(pos.get(0)) + deltaFromPrevious.x);
            newPos.set(1, asDouble(pos.get(1)) + deltaFromPrevious.y);
            newPos.set(2, asDouble(pos.get(2)) + deltaFromPrevious.x);
            newPos.set(3, asDouble(pos.get(3)) + deltaFromPrevious.y);
            break;
        default:
            throw new AssertionError("Unknown drag type: " + dragType);
        }
        setProperty(POSITION_PROPERTY, newPos);
    }

    @Override public void translate(Point offset) {
        List<?> pos = (List<?>)getProperty(POSITION_PROPERTY);
        List<Object> newPos = new ArrayList<>(pos);
        newPos.set(0, asDouble(pos.get(0)) + offset.x);
        newPos.set(1, asDouble(pos.get(1)) + offset.y);
        newPos.set(2, asDouble(pos.get(2)) + offset.x);
        newPos.set(3, asDouble(pos.get(3)) + offset.y);
        setProperty(POSITION_PROPERTY, newPos);
    }

}
