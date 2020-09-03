package com.dozingcatsoftware.vectorpinball.elements;

import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asFloat;
import static com.dozingcatsoftware.vectorpinball.util.MathUtils.toRadiansF;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.dozingcatsoftware.vectorpinball.model.Field;
import com.dozingcatsoftware.vectorpinball.model.IFieldRenderer;

/**
 * This FieldElement subclass approximates a circular wall with a series of straight wall segments
 * whose endpoints lie on a circle or ellipse. It is defined in the layout JSON as follows:
 * {
 *     "class": "WallArcElement",
 *     "center": [5.5, 10], // Center of circle or ellipse.
 *     "xradius": 2.5, // Radius in the horizontal direction.
 *     "yradius": 2, // Radius in the y direction.
 *     "minangle": 45, // Starting angle in degrees, 0 is to the right of the center, 90 is up.
 *     "maxangle": 135, // Ending angle in degrees.
 *     "segments": 10, // Number of straight wall segments to use to approximate the arc.
 *     "color": [255,0,0] // Optional RGB values for the arc's color.
 * }
 *
 * For circular walls, the "radius" attribute can be used instead of xradius and yradius.
 */
public class WallArcElement extends FieldElement {

    public static final String CENTER_PROPERTY = "center";
    public static final String RADIUS_PROPERTY = "radius";
    public static final String X_RADIUS_PROPERTY = "xradius";
    public static final String Y_RADIUS_PROPERTY = "yradius";
    public static final String NUM_SEGMENTS_PROPERTY = "segments";
    public static final String MIN_ANGLE_PROPERTY = "minangle";
    public static final String MAX_ANGLE_PROPERTY = "maxangle";
    public static final String IGNORE_BALL_PROPERTY = "ignoreBall";

    private List<Body> wallBodies = new ArrayList<>();
    private float centerX;
    private float centerY;
    private float radiusX;
    private float radiusY;
    private float startAngle;
    private float endAngle;
    private float[] xEndpoints;
    private float[] yEndpoints;

    @Override public void finishCreateElement(
            Map<String, ?> params, FieldElementCollection collection) {
        List<?> centerPos = (List<?>) params.get(CENTER_PROPERTY);
        centerX = asFloat(centerPos.get(0));
        centerY = asFloat(centerPos.get(1));

        // Can specify "radius" for circle, or "xradius" and "yradius" for ellipse.
        if (params.containsKey(RADIUS_PROPERTY)) {
            this.radiusX = this.radiusY = asFloat(params.get(RADIUS_PROPERTY));
        }
        else {
            this.radiusX = asFloat(params.get(X_RADIUS_PROPERTY));
            this.radiusY = asFloat(params.get(Y_RADIUS_PROPERTY));
        }

        Number segments = (Number) params.get(NUM_SEGMENTS_PROPERTY);
        int numsegments = (segments != null) ? segments.intValue() : 5;
        this.startAngle = toRadiansF(asFloat(params.get(MIN_ANGLE_PROPERTY)));
        this.endAngle = toRadiansF(asFloat(params.get(MAX_ANGLE_PROPERTY)));
        float diff = endAngle - startAngle;
        // Create `numsegments` line segments to approximate circular arc.
        this.xEndpoints = new float[numsegments + 1];
        this.yEndpoints = new float[numsegments + 1];
        for (int i = 0; i <= numsegments; i++) {
            float angle = startAngle + i * diff / numsegments;
            this.xEndpoints[i] = centerX + radiusX * (float) Math.cos(angle);
            this.yEndpoints[i] = centerY + radiusY * (float) Math.sin(angle);
        }
    }

    @Override public void createBodies(World world) {
        if (getBooleanParameterValueForKey(IGNORE_BALL_PROPERTY)) {
            return;
        }
        for (int i = 1; i < xEndpoints.length; i++) {
            Body wall = Box2DFactory.createThinWall(
                    world, xEndpoints[i - 1], yEndpoints[i - 1], xEndpoints[i], yEndpoints[i], 0f);
            this.wallBodies.add(wall);
        }
    }

    @Override public List<Body> getBodies() {
        return wallBodies;
    }

    @Override public void draw(Field field, IFieldRenderer renderer) {
        int color = currentColor(DEFAULT_WALL_COLOR);
        // If possible, drawing an arc is faster and looks better compared to drawing the
        // individual line segments.
        if (renderer.canDrawArc()) {
            renderer.drawArc(centerX, centerY, radiusX, radiusY, startAngle, endAngle, color);
        }
        else {
            renderer.drawLinePath(xEndpoints, yEndpoints, color);
        }
    }
}
