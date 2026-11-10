package com.dozingcatsoftware.vectorpinball.editor.elements;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.dozingcatsoftware.vectorpinball.editor.IEditableFieldRenderer;
import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.editor.Point;

public class EditableBumperElement extends EditableFieldElement {
    public static final String POSITION_PROPERTY = "position";
    public static final String RADIUS_PROPERTY = "radius";
    public static final String KICK_PROPERTY = "kick";
    public static final String OUTER_RADIUS_PROPERTY = "outerRadius";
    public static final String OUTER_COLOR_PROPERTY = "outerColor";
    public static final String INACTIVE_LAYER_OUTER_COLOR_PROPERTY = "inactiveLayerOuterColor";

    static final int DEFAULT_COLOR = Color.fromRGB(0, 0, 255);
    static final int DEFAULT_OUTER_COLOR = Color.fromRGBA(0, 0, 255, 128);

    @Override protected void addPropertiesForNewElement(Map<String, Object> props, EditableField field) {
        props.put(POSITION_PROPERTY, Arrays.asList("-0.5", "-0.5"));
        props.put(RADIUS_PROPERTY, "0.5");
        props.put(KICK_PROPERTY, "1.0");
    }

    private Point center() {
        return getPointProperty(POSITION_PROPERTY);
    }

    private double radius() {
        return getDoubleProperty(RADIUS_PROPERTY);
    }

    private double outerRadius() {
        return getDoubleProperty(OUTER_RADIUS_PROPERTY);
    }

    private int outerColor() {
        return colorForDisplay(getProperty(OUTER_COLOR_PROPERTY) != null ?
                Color.fromList((List<Number>)getProperty(OUTER_COLOR_PROPERTY)) :
                DEFAULT_OUTER_COLOR);
    }

    @Override public void drawForEditor(IEditableFieldRenderer renderer, boolean isSelected) {
        Point c = center();
        int color = currentColor(DEFAULT_COLOR);
        double maxRad = Math.max(radius(), outerRadius());
        if (outerRadius() > 0) {
            renderer.fillCircle(c.x, c.y, outerRadius(), outerColor());
        }
        renderer.fillCircle(c.x, c.y, radius(), color);
        if (isSelected) {
            renderer.drawLine(c.x - maxRad, c.y - maxRad, c.x + maxRad, c.y - maxRad, color);
            renderer.drawLine(c.x + maxRad, c.y - maxRad, c.x + maxRad, c.y + maxRad, color);
            renderer.drawLine(c.x + maxRad, c.y + maxRad, c.x - maxRad, c.y + maxRad, color);
            renderer.drawLine(c.x - maxRad, c.y + maxRad, c.x - maxRad, c.y - maxRad, color);
        }
    }

    @Override public boolean isPointWithinDistance(Point point, double distance) {
        // Ignore distance, just require clicking on circle.
        double dist = point.distanceTo(center());
        return dist <= radius() || dist <= outerRadius();
    }

    @Override public void translate(Point offset) {
         // TODO: handle resizing as well as moving.
        setPointProperty(POSITION_PROPERTY, center().add(offset));
    }
}
