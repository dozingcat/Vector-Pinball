package com.dozingcatsoftware.vectorpinball.elements;

import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asFloat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.dozingcatsoftware.vectorpinball.model.Field;
import com.dozingcatsoftware.vectorpinball.model.IFieldRenderer;

/**
 * FieldElement subclass which represents a series of wall segments. The segments are defined in
 * the "positions" parameter as a list of [x,y] values, for example:
 * {
 * 		"class": "WallPathElement",
 * 		"positions": [[5,5], [5,10], [8,10], [5, 15]]
 * }
 */
public class WallPathElement extends FieldElement {

    public static final String POSITIONS_PROPERTY = "positions";
    public static final String IGNORE_BALL_PROPERTY = "ignoreBall";

    private List<Body> wallBodies = new ArrayList<>();
    private float[] xEndpoints;
    private float[] yEndpoints;

    @Override public void finishCreateElement(
            Map<String, ?> params, FieldElementCollection collection) {
        @SuppressWarnings("unchecked")
        List<List<?>> positions = (List<List<?>>) params.get(POSITIONS_PROPERTY);
        // N positions produce N-1 line segments.
        this.xEndpoints = new float[positions.size()];
        this.yEndpoints = new float[positions.size()];
        for (int i = 0; i < positions.size(); i++) {
            List<?> pos = positions.get(i);
            xEndpoints[i] = asFloat(pos.get(0));
            yEndpoints[i] = asFloat(pos.get(1));
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
        renderer.drawLinePath(this.xEndpoints, this.yEndpoints, color);
    }
}
