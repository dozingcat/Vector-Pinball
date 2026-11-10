package com.dozingcatsoftware.vectorpinball.editor.elements;

import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asDouble;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.dozingcatsoftware.vectorpinball.editor.IEditableFieldRenderer;
import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.editor.Point;

public class EditableWallPathElement extends EditableFieldElement {

    public static final String POSITIONS_PROPERTY = "positions";
    public static final String IGNORE_BALL_PROPERTY = "ignoreBall";
    public static final String RESTITUTION_PROPERTY = "restitution";
    public static final String FRICTION_PROPERTY = "friction";

    static final double POINT_DRAG_MAX_DISTANCE_SQUARED = 0.3 * 0.3;

    int dragPointIndex = -1;

    @Override protected void addPropertiesForNewElement(Map<String, Object> props, EditableField field) {
        props.put(POSITIONS_PROPERTY, Arrays.asList(
                Arrays.asList("-0.5", "0"),
                Arrays.asList("-0.5", "2"),
                Arrays.asList("-0.5", "4")
                ));
    }

    double getSegmentX(int index) {
        List<List<Object>> positions = (List<List<Object>>)getProperty(POSITIONS_PROPERTY);
        return asDouble(positions.get(index).get(0));
    }

    double getSegmentY(int index) {
        List<List<Object>> positions = (List<List<Object>>)getProperty(POSITIONS_PROPERTY);
        return asDouble(positions.get(index).get(1));
    }

    int numPoints() {
        return ((List<?>)getProperty(POSITIONS_PROPERTY)).size();
    }

    @Override public void drawForEditor(IEditableFieldRenderer renderer, boolean isSelected) {
        double selectionCircleRadius = 0.25 / renderer.getRelativeScale();
        int color = currentColor(DEFAULT_WALL_COLOR);
        int size = numPoints();
        for (int i=1; i<size; i++) {
            renderer.drawLine(getSegmentX(i-1), getSegmentY(i-1),
                    getSegmentX(i), getSegmentY(i), color);
        }
        if (isSelected) {
            int colorWithAlpha = Color.withAlpha(color, Color.getAlpha(color) / 2);
            for (int i = 0; i < size; i++) {
                renderer.fillCircle(getSegmentX(i), getSegmentY(i), selectionCircleRadius, colorWithAlpha);
            }
        }
    }

    @Override public boolean isPointWithinDistance(Point point, double distance) {
        int size = numPoints();
        for (int i=1; i<size; i++) {
            if (point.distanceToLineSegment(getSegmentX(i-1), getSegmentY(i-1), getSegmentX(i), getSegmentY(i)) <= distance) {
                return true;
            }
        }
        return false;
    }

    @Override public void startDrag(Point point) {
        dragPointIndex = -1;
        int size = numPoints();
        for (int i=0; i<size; i++) {
            // Need zoom scale to accurately determine if point is clicked.
            if (point.squaredDistanceTo(getSegmentX(i), getSegmentY(i)) < POINT_DRAG_MAX_DISTANCE_SQUARED) {
                dragPointIndex = i;
                break;
            }
        }
    }

    @Override public void translate(Point offset) {
        int size = numPoints();
        List<List<Double>> newPositions = new ArrayList<>();
        for (int i=0; i<size; i++) {
            double dx = (dragPointIndex==-1 || dragPointIndex==i) ? offset.x : 0;
            double dy = (dragPointIndex==-1 || dragPointIndex==i) ? offset.y : 0;
            newPositions.add(Arrays.asList(getSegmentX(i) + dx, getSegmentY(i) + dy));
        }
        setProperty(POSITIONS_PROPERTY, newPositions);
    }

}
