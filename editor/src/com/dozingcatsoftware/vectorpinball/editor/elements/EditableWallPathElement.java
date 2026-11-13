package com.dozingcatsoftware.vectorpinball.editor.elements;

import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asDouble;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.dozingcatsoftware.vectorpinball.editor.IEditableFieldRenderer;
import com.dozingcatsoftware.vectorpinball.elements.WallPathGeometry;
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

    /** Returns the path as drawn, with curves replacing corners where specified. */
    WallPathGeometry getGeometry() {
        return WallPathGeometry.fromPositions((List<List<Object>>)getProperty(POSITIONS_PROPERTY));
    }

    @Override public void drawForEditor(IEditableFieldRenderer renderer, boolean isSelected) {
        double selectionCircleRadius = 0.25 / renderer.getRelativeScale();
        int color = currentColor(DEFAULT_WALL_COLOR);
        WallPathGeometry geometry = getGeometry();
        for (int i=1; i<geometry.numPoints(); i++) {
            renderer.drawLine(geometry.getX(i-1), geometry.getY(i-1),
                    geometry.getX(i), geometry.getY(i), color);
        }
        if (isSelected) {
            int colorWithAlpha = Color.withAlpha(color, Color.getAlpha(color) / 2);
            int size = numPoints();
            // With curves, the draggable points may not be on the path, so show the straight
            // segments between them.
            if (geometry.hasCurves()) {
                int lineColor = Color.withAlpha(color, Color.getAlpha(color) / 4);
                for (int i = 1; i < size; i++) {
                    renderer.drawLine(getSegmentX(i-1), getSegmentY(i-1),
                            getSegmentX(i), getSegmentY(i), lineColor);
                }
            }
            for (int i = 0; i < size; i++) {
                renderer.fillCircle(getSegmentX(i), getSegmentY(i), selectionCircleRadius, colorWithAlpha);
            }
        }
    }

    @Override public boolean isPointWithinDistance(Point point, double distance) {
        WallPathGeometry geometry = getGeometry();
        // Accept clicks that are close to any of the points (which may not be on the path if there
        // are curves), or close to the path itself.
        double sqDist = distance * distance;
        for (int i = 0; i < numPoints(); i++) {
            if (point.squaredDistanceTo(getSegmentX(i), getSegmentY(i)) <= sqDist) {
                return true;
            }
        }

        for (int i=1; i<geometry.numPoints(); i++) {
            if (point.distanceToLineSegment(geometry.getX(i-1), geometry.getY(i-1),
                    geometry.getX(i), geometry.getY(i)) <= distance) {
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
        List<List<Object>> positions = (List<List<Object>>)getProperty(POSITIONS_PROPERTY);
        List<List<Object>> newPositions = new ArrayList<>();
        for (int i=0; i<positions.size(); i++) {
            double dx = (dragPointIndex==-1 || dragPointIndex==i) ? offset.x : 0;
            double dy = (dragPointIndex==-1 || dragPointIndex==i) ? offset.y : 0;
            // Keep any curve distances after x and y.
            List<Object> newPos = new ArrayList<>(positions.get(i));
            newPos.set(0, getSegmentX(i) + dx);
            newPos.set(1, getSegmentY(i) + dy);
            newPositions.add(newPos);
        }
        setProperty(POSITIONS_PROPERTY, newPositions);
    }

}
