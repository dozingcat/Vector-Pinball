package com.dozingcatsoftware.bouncy.elements;

import static com.dozingcatsoftware.bouncy.util.MathUtils.asFloat;
import static com.dozingcatsoftware.bouncy.util.MathUtils.toRadians;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.dozingcatsoftware.bouncy.Color;
import com.dozingcatsoftware.bouncy.IFieldRenderer;

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

    public List<Body> wallBodies = new ArrayList<Body>();
    float[][] lineSegments;

    @Override public void finishCreateElement(Map<String, ?> params, FieldElementCollection collection) {
        List<?> centerPos = (List<?>)params.get(CENTER_PROPERTY);
        float cx = asFloat(centerPos.get(0));
        float cy = asFloat(centerPos.get(1));

        // Can specify "radius" for circle, or "xradius" and "yradius" for ellipse.
        float xradius, yradius;
        if (params.containsKey(RADIUS_PROPERTY)) {
            xradius = yradius = asFloat(params.get(RADIUS_PROPERTY));
        }
        else {
            xradius = asFloat(params.get(X_RADIUS_PROPERTY));
            yradius = asFloat(params.get(Y_RADIUS_PROPERTY));
        }

        Number segments = (Number)params.get(NUM_SEGMENTS_PROPERTY);
        int numsegments = (segments!=null) ? segments.intValue() : 5;
        float minangle = toRadians(asFloat(params.get(MIN_ANGLE_PROPERTY)));
        float maxangle = toRadians(asFloat(params.get(MAX_ANGLE_PROPERTY)));
        float diff = maxangle - minangle;
        // Create numsegments line segments to approximate circular arc.
        lineSegments = new float[numsegments][];
        for(int i=0; i<numsegments; i++) {
            float angle1 = minangle + i * diff / numsegments;
            float angle2 = minangle + (i+1) * diff / numsegments;
            float x1 = cx + xradius * (float)Math.cos(angle1);
            float y1 = cy + yradius * (float)Math.sin(angle1);
            float x2 = cx + xradius * (float)Math.cos(angle2);
            float y2 = cy + yradius * (float)Math.sin(angle2);
            lineSegments[i] = (new float[] {x1, y1, x2, y2});
        }
    }

    @Override public void createBodies(World world) {
        if (getBooleanParameterValueForKey(IGNORE_BALL_PROPERTY)) return;

        for (float[] segment : this.lineSegments) {
            Body wall = Box2DFactory.createThinWall(
                    world, segment[0], segment[1], segment[2], segment[3], 0f);
            this.wallBodies.add(wall);
        }
    }

    @Override public List<Body> getBodies() {
        return wallBodies;
    }

    @Override public void draw(IFieldRenderer renderer) {
        Color color = currentColor(DEFAULT_WALL_COLOR);
        for (float[] segment : this.lineSegments) {
            renderer.drawLine(segment[0], segment[1], segment[2], segment[3], color);
        }
    }
}
