package com.dozingcatsoftware.vectorpinball.editor.elements;

import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asDouble;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.dozingcatsoftware.vectorpinball.editor.IEditableFieldRenderer;
import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.editor.Point;

public class EditableSensorElement extends EditableFieldElement {

    public static final String RECT_PROPERTY = "rect";
    public static final String BALL_LAYER_TO_PROPERTY = "ballLayer";
    public static final String BALL_LAYER_FROM_PROPERTY = "ballLayerFrom";
    public static final String RECORD_BALL_TIMES_PROPERTY = "recordBallTimes";

    static final int EDITOR_OUTLINE_COLOR = Color.fromRGB(128, 128, 128);
    static final int EDITOR_FILL_COLOR = Color.fromRGBA(128, 128, 128, 128);

    // Maximum world-space distance from a corner for a drag to grab that corner and resize,
    // rather than move the whole rectangle. Clamped to a fraction of the rect size below so
    // corners stay distinct from the center on small sensors.
    static final double CORNER_GRAB_DISTANCE = 0.4;

    // The corner being resized, expressed as the indices into the rect property [xa, ya, xb, yb]
    // whose x and y values that corner uses. -1 means a drag moves the whole element.
    private int dragXIndex = -1;
    private int dragYIndex = -1;

    @Override protected void addPropertiesForNewElement(Map<String, Object> props, EditableField field) {
        props.put(RECT_PROPERTY, Arrays.asList("-0.5", "-0.5", "0", "0"));
    }

    /** Returns the normalized bounds as {xmin, ymin, xmax, ymax}. */
    private double[] bounds() {
        double xa = getListDoubleProperty(RECT_PROPERTY, 0);
        double ya = getListDoubleProperty(RECT_PROPERTY, 1);
        double xb = getListDoubleProperty(RECT_PROPERTY, 2);
        double yb = getListDoubleProperty(RECT_PROPERTY, 3);
        return new double[] {
                Math.min(xa, xb), Math.min(ya, yb), Math.max(xa, xb), Math.max(ya, yb)};
    }

    @Override public void drawForEditor(IEditableFieldRenderer renderer, boolean isSelected) {
        double[] b = bounds();
        double xmin = b[0], ymin = b[1], xmax = b[2], ymax = b[3];
        renderer.drawLine(xmin, ymin, xmax, ymin, EDITOR_OUTLINE_COLOR);
        renderer.drawLine(xmax, ymin, xmax, ymax, EDITOR_OUTLINE_COLOR);
        renderer.drawLine(xmax, ymax, xmin, ymax, EDITOR_OUTLINE_COLOR);
        renderer.drawLine(xmin, ymax, xmin, ymin, EDITOR_OUTLINE_COLOR);

        if (isSelected) {
            renderer.fillPolygon(
                    new double[] {xmin, xmin, xmax, xmax},
                    new double[] {ymin, ymax, ymax, ymin},
                    EDITOR_FILL_COLOR);
            double selectionCircleRadius = 0.25 / renderer.getRelativeScale();
            renderer.fillCircle(xmin, ymin, selectionCircleRadius, EDITOR_OUTLINE_COLOR);
            renderer.fillCircle(xmin, ymax, selectionCircleRadius, EDITOR_OUTLINE_COLOR);
            renderer.fillCircle(xmax, ymin, selectionCircleRadius, EDITOR_OUTLINE_COLOR);
            renderer.fillCircle(xmax, ymax, selectionCircleRadius, EDITOR_OUTLINE_COLOR);
        }
    }

    @Override public boolean isPointWithinDistance(Point point, double distance) {
        // Always treat as rectangle, require click inside and ignore distance.
        double[] b = bounds();
        return (point.x>=b[0] && point.x<=b[2] && point.y>=b[1] && point.y<=b[3]);
    }

    @Override public void startDrag(Point point) {
        dragXIndex = -1;
        dragYIndex = -1;
        double xa = getListDoubleProperty(RECT_PROPERTY, 0);
        double ya = getListDoubleProperty(RECT_PROPERTY, 1);
        double xb = getListDoubleProperty(RECT_PROPERTY, 2);
        double yb = getListDoubleProperty(RECT_PROPERTY, 3);
        // Keep the grab radius below a third of each side so the center stays grabbable for moving.
        double grab = Math.min(CORNER_GRAB_DISTANCE,
                Math.min(Math.abs(xb - xa), Math.abs(yb - ya)) / 3);
        // Each corner uses one x value (index 0 or 2) and one y value (index 1 or 3) from the rect.
        int[] xIndices = {0, 0, 2, 2};
        int[] yIndices = {1, 3, 1, 3};
        double bestDist = grab;
        for (int i = 0; i < 4; i++) {
            double cx = (xIndices[i] == 0) ? xa : xb;
            double cy = (yIndices[i] == 1) ? ya : yb;
            double dist = point.distanceTo(cx, cy);
            if (dist <= bestDist) {
                bestDist = dist;
                dragXIndex = xIndices[i];
                dragYIndex = yIndices[i];
            }
        }
    }

    @Override public void handleDrag(Point point, Point deltaFromStart, Point deltaFromPrevious) {
        if (dragXIndex < 0 || dragYIndex < 0) {
            translate(deltaFromPrevious);
            return;
        }
        List<?> rect = (List<?>)getProperty(RECT_PROPERTY);
        List<Object> newRect = new ArrayList<>(Arrays.asList(
                asDouble(rect.get(0)), asDouble(rect.get(1)),
                asDouble(rect.get(2)), asDouble(rect.get(3))));
        newRect.set(dragXIndex, asDouble(rect.get(dragXIndex)) + deltaFromPrevious.x);
        newRect.set(dragYIndex, asDouble(rect.get(dragYIndex)) + deltaFromPrevious.y);
        setProperty(RECT_PROPERTY, newRect);
    }

    @Override public void translate(Point offset) {
        List<Object> rect = (List<Object>)getProperty(RECT_PROPERTY);
        setProperty(RECT_PROPERTY, Arrays.asList(
                asDouble(rect.get(0)) + offset.x,
                asDouble(rect.get(1)) + offset.y,
                asDouble(rect.get(2)) + offset.x,
                asDouble(rect.get(3)) + offset.y));
    }

}
