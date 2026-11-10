package com.dozingcatsoftware.vectorpinball.editor.elements;

import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asDouble;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dozingcatsoftware.vectorpinball.editor.IEditableFieldRenderer;
import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.editor.Point;

public class EditableRolloverGroupElement extends EditableFieldElement {

    public static final String TOGGLE_OFF_PROPERTY = "toggleOff";
    public static final String CYCLE_ON_FLIPPER_PROPERTY = "cycleOnFlipper";
    public static final String IGNORE_BALL_PROPERTY = "ignoreBall";
    public static final String RADIUS_PROPERTY = "radius";
    public static final String RESET_DELAY_PROPERTY = "reset";
    public static final String ROLLOVERS_PROPERTY = "rollovers";
    // For individual rollovers.
    public static final String POSITION_PROPERTY = "position";
    public static final String COLOR_PROPERTY = "color";
    public static final String SCORE_PROPERTY = "score";

    static final int DEFAULT_COLOR = Color.fromRGB(0, 255, 0);

    @Override protected void addPropertiesForNewElement(Map<String, Object> props, EditableField field) {
        props.put(RADIUS_PROPERTY, "0.5");
        Map<String, Object> rollover = new HashMap<>();
        rollover.put(POSITION_PROPERTY, Arrays.asList("-0.5", "-0.5"));
        props.put(ROLLOVERS_PROPERTY, Arrays.asList(rollover));
    }

    @Override public void drawForEditor(IEditableFieldRenderer renderer, boolean isSelected) {
        int groupColor = currentColor(DEFAULT_COLOR);
        double groupRadius = asDouble(getProperty(RADIUS_PROPERTY));
        List<Map<String, Object>> rolloverMaps = (List<Map<String, Object>>)getProperty(ROLLOVERS_PROPERTY);
        for (Map<String, Object> rmap : rolloverMaps) {
            List<Object> pos = (List<Object>)rmap.get(POSITION_PROPERTY);
            int color = rmap.containsKey(COLOR_PROPERTY) ?
                    Color.fromList((List<Number>)rmap.get(COLOR_PROPERTY)) : groupColor;
            double radius = rmap.containsKey(RADIUS_PROPERTY) ?
                    asDouble(rmap.get(RADIUS_PROPERTY)) : groupRadius;
            renderer.frameCircle(asDouble(pos.get(0)), asDouble(pos.get(1)), radius, color);
            if (isSelected) {
                int colorWithAlpha = Color.withAlpha(color, Color.getAlpha(color) / 2);
                renderer.fillCircle(asDouble(pos.get(0)), asDouble(pos.get(1)), radius, colorWithAlpha);
            }
        }
    }

    @Override public boolean isPointWithinDistance(Point point, double distance) {
        double groupRadius = asDouble(getProperty(RADIUS_PROPERTY));
        List<Map<String, Object>> rolloverMaps = (List<Map<String, Object>>)getProperty(ROLLOVERS_PROPERTY);
        for (Map<String, Object> rmap : rolloverMaps) {
            List<Number> pos = (List<Number>)rmap.get(POSITION_PROPERTY);
            double rx = asDouble(pos.get(0));
            double ry = asDouble(pos.get(1));
            double radius = rmap.containsKey(RADIUS_PROPERTY) ?
                    asDouble(rmap.get(RADIUS_PROPERTY)) : groupRadius;
            if (point.distanceTo(rx, ry) <= radius) {
                return true;
            }
        }
        return false;
    }

    @Override public void translate(Point offset) {
        List<Map<String, Object>> rolloverMaps = (List<Map<String, Object>>)getProperty(ROLLOVERS_PROPERTY);
        for (Map<String, Object> rmap : rolloverMaps) {
            List<Number> pos = (List<Number>)rmap.get(POSITION_PROPERTY);
            rmap.put(POSITION_PROPERTY, Arrays.asList(
                    asDouble(pos.get(0)) + offset.x,
                    asDouble(pos.get(1)) + offset.y));
        }
        // This is needed to signal the change handler.
        setProperty(ROLLOVERS_PROPERTY, rolloverMaps);
    }

}
