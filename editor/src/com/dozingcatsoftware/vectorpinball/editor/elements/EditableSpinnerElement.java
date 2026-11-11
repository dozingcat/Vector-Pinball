package com.dozingcatsoftware.vectorpinball.editor.elements;

import com.dozingcatsoftware.vectorpinball.editor.IEditableFieldRenderer;
import com.dozingcatsoftware.vectorpinball.editor.Point;
import com.dozingcatsoftware.vectorpinball.model.Color;

import java.util.Arrays;
import java.util.Map;

public class EditableSpinnerElement extends EditableFieldElement {
    public static final String CENTER_PROPERTY = "position";
    public static final String RADIUS_PROPERTY = "radius";
    public static final String MIN_ACTIVATION_SPEED_PROPERTY = "minActivationSpeed";
    public static final String BASE_CYCLES_PER_SECOND_PROPERTY = "baseCyclesPerSecond";
    public static final String MIN_CYCLES_PER_SECOND_PROPERTY = "minCyclesPerSecond";
    public static final String SPEED_DECAY_RATE_PROPERTY = "speedDecayRate";

    @Override
    protected void addPropertiesForNewElement(Map<String, Object> props, EditableField field) {
        props.put(CENTER_PROPERTY, Arrays.asList("-0.5", "-0.5"));
        props.put(RADIUS_PROPERTY, "0.5");
        props.put(COLOR_PROPERTY, Arrays.asList(255, 255, 255));
    }

    private Point center() {
        return getPointProperty(CENTER_PROPERTY);
    }

    private double radius() {
        return getDoubleProperty(RADIUS_PROPERTY);
    }

    @Override
    public void drawForEditor(IEditableFieldRenderer renderer, boolean isSelected) {
        int color = currentColor(0);
        Point center = center();
        double r = radius();
        renderer.frameCircle(center.x, center.y, r, color);
        if (isSelected) {
            int colorWithAlpha = Color.withAlpha(color, Color.getAlpha(color) / 2);
            renderer.fillCircle(center.x, center.y, r, colorWithAlpha);
        }
    }

    @Override
    public boolean isPointWithinDistance(Point point, double distance) {
        return point.distanceTo(center()) <= radius();
    }

    @Override
    public void translate(Point offset) {
        setPointProperty(CENTER_PROPERTY, center().add(offset));
    }
}
