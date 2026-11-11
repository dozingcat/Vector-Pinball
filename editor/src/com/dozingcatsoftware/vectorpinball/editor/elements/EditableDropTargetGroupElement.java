package com.dozingcatsoftware.vectorpinball.editor.elements;

import static com.dozingcatsoftware.vectorpinball.util.MathUtils.TAU;
import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asDouble;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.dozingcatsoftware.vectorpinball.editor.IEditableFieldRenderer;
import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.editor.Point;

public class EditableDropTargetGroupElement extends EditableFieldElement {

    public static final String POSITIONS_PROPERTY = "positions";
    public static final String WALL_START_PROPERTY = "wallStart";
    public static final String WALL_END_PROPERTY = "wallEnd";
    public static final String GAP_FROM_WALL_PROPERTY = "gapFromWall";
    public static final String START_DISTANCE_ALONG_WALL_PROPERTY = "startDistanceAlongWall";
    public static final String TARGET_WIDTH_PROPERTY = "targetWidth";
    public static final String GAP_BETWEEN_TARGETS_PROPERTY = "gapBetweenTargets";
    public static final String RESET_DELAY_PROPERTY = "reset";
    public static final String NUM_TARGETS_PROPERTY = "numTargets";

    static final int DEFAULT_COLOR = Color.fromRGB(0, 255, 0);

    // Targets can be specified directly in the "positions" list, or computed from a wall segment.
    private boolean usesDirectPositions() {
        List<?> positionList = getListProperty(POSITIONS_PROPERTY);
        return positionList != null && !positionList.isEmpty();
    }

    // Computes each target's endpoints as {x1, y1, x2, y2}, derived from the element's properties.
    // The property map is the source of truth; this is recomputed on demand rather than cached.
    private double[][] computePositions() {
        List<List<Object>> positionList = (List) getProperty(POSITIONS_PROPERTY);
        if (positionList != null && !positionList.isEmpty()) {
            double[][] positions = new double[positionList.size()][];
            for (int i = 0; i < positionList.size(); i++) {
                List<Object> coords = positionList.get(i);
                positions[i] = new double[] {asDouble(coords.get(0)), asDouble(coords.get(1)),
                        asDouble(coords.get(2)), asDouble(coords.get(3))};
            }
            return positions;
        }

        Point wallStart = getPointProperty(WALL_START_PROPERTY);
        Point wallEnd = getPointProperty(WALL_END_PROPERTY);
        double gapFromWall = getDoubleProperty(GAP_FROM_WALL_PROPERTY);
        double startDistanceAlongWall = getDoubleProperty(START_DISTANCE_ALONG_WALL_PROPERTY);
        double targetWidth = getDoubleProperty(TARGET_WIDTH_PROPERTY);
        double gapBetweenTargets = getDoubleProperty(GAP_BETWEEN_TARGETS_PROPERTY);
        int numTargets = getIntProperty(NUM_TARGETS_PROPERTY);

        double[][] positions = new double[numTargets][];
        double wallAngle = Math.atan2(wallEnd.y - wallStart.y, wallEnd.x - wallStart.x);
        double perpToWallAngle = wallAngle + TAU/4;
        for (int i = 0; i < numTargets; i++) {
            double alongWallStart = startDistanceAlongWall + i * (targetWidth + gapBetweenTargets);
            double alongWallEnd = alongWallStart + targetWidth;
            double x1 = (wallStart.x + (alongWallStart * Math.cos(wallAngle)) +
                    (gapFromWall * Math.cos(perpToWallAngle)));
            double y1 = (wallStart.y + (alongWallStart * Math.sin(wallAngle)) +
                    (gapFromWall * Math.sin(perpToWallAngle)));
            double x2 = (wallStart.x + (alongWallEnd * Math.cos(wallAngle)) +
                    (gapFromWall * Math.cos(perpToWallAngle)));
            double y2 = (wallStart.y + (alongWallEnd * Math.sin(wallAngle)) +
                    (gapFromWall * Math.sin(perpToWallAngle)));
            positions[i] = new double[] {x1, y1, x2, y2};
        }
        return positions;
    }

    @Override protected void addPropertiesForNewElement(Map<String, Object> props, EditableField field) {
        props.put(POSITIONS_PROPERTY, Arrays.asList(
                Arrays.asList(-0.5, 0.0, -0.5, 0.8),
                Arrays.asList(-0.5, 1.0, -0.5, 1.8),
                Arrays.asList(-0.5, 2.0, -0.5, 2.8)
                ));
        props.put(RESET_DELAY_PROPERTY, "2");
    }

    @Override public void drawForEditor(IEditableFieldRenderer renderer, boolean isSelected) {
        double[][] positions = computePositions();
        // draw line for each target
        int color = currentColor(DEFAULT_COLOR);
        for(double[] pos : positions) {
            renderer.drawLine(pos[0], pos[1], pos[2], pos[3], color);
        }
        if (isSelected) {
            // Draw a translucent rectangle around each target.
            int colorWithAlpha = Color.withAlpha(color, Color.getAlpha(color) / 2);
            double dist = 0.1 * renderer.getRelativeScale();
            double[] xPoints = new double[4];
            double[] yPoints = new double[4];
            for(double[] pos : positions) {
                double angle = Math.atan2(pos[3]-pos[1], pos[2]-pos[0]);
                double perpAngle = angle + TAU/4;
                // Extend past each endpoint of target, then go perpendicular to get polygon vertices.
                xPoints[0] = pos[0] - dist*Math.cos(angle) + dist*Math.cos(perpAngle);
                yPoints[0] = pos[1] - dist*Math.sin(angle) + dist*Math.sin(perpAngle);
                xPoints[1] = pos[2] + dist*Math.cos(angle) + dist*Math.cos(perpAngle);
                yPoints[1] = pos[3] + dist*Math.sin(angle) + dist*Math.sin(perpAngle);
                xPoints[2] = pos[2] + dist*Math.cos(angle) - dist*Math.cos(perpAngle);
                yPoints[2] = pos[3] + dist*Math.sin(angle) - dist*Math.sin(perpAngle);
                xPoints[3] = pos[0] - dist*Math.cos(angle) - dist*Math.cos(perpAngle);
                yPoints[3] = pos[1] - dist*Math.sin(angle) - dist*Math.sin(perpAngle);

                renderer.fillPolygon(xPoints, yPoints, colorWithAlpha);
            }
        }
    }

    @Override public boolean isPointWithinDistance(Point point, double distance) {
        for (double[] pos : computePositions()) {
            double actualDist = point.distanceToLineSegment(pos[0], pos[1], pos[2], pos[3]);
            if (actualDist <= distance) {
                return true;
            }
        }
        return false;
    }

    @Override public void translate(Point offset) {
        // Update either individual target positions, or the position of the line
        // the targets are positioned relative to.
        if (usesDirectPositions()) {
            List<List<Number>> newPositions = new ArrayList<>();
            for (double[] pos : computePositions()) {
                newPositions.add(Arrays.asList(
                        pos[0] + offset.x,
                        pos[1] + offset.y,
                        pos[2] + offset.x,
                        pos[3] + offset.y));
            }
            setProperty(POSITIONS_PROPERTY, newPositions);
        }
        else {
            setPointProperty(WALL_START_PROPERTY, getPointProperty(WALL_START_PROPERTY).add(offset));
            setPointProperty(WALL_END_PROPERTY, getPointProperty(WALL_END_PROPERTY).add(offset));
        }
    }

}
