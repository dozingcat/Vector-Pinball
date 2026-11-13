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
 *
 * Corners can be replaced by curves by adding distances to a position: [x, y, distance] starts
 * the curve `distance` units before the corner and ends it `distance` units after, and
 * [x, y, distanceBefore, distanceAfter] can use different distances on each side. With equal
 * distances the curve is a circular arc. For example, this makes a 90 degree arc of radius 1
 * connecting a vertical and horizontal wall:
 * {
 * 		"class": "WallPathElement",
 * 		"positions": [[5,5], [5,10,1], [8,10]]
 * }
 * See WallPathGeometry for details.
 */
public class WallPathElement extends FieldElement {

    public static final String POSITIONS_PROPERTY = "positions";
    public static final String IGNORE_BALL_PROPERTY = "ignoreBall";
    public static final String RESTITUTION_PROPERTY = "restitution";
    public static final String FRICTION_PROPERTY = "friction";

    private List<Body> wallBodies = new ArrayList<>();
    private float[] xEndpoints;
    private float[] yEndpoints;
    private float restitution;
    private float friction;

    @Override public void finishCreateElement(
            Map<String, ?> params, FieldElementCollection collection) {
        @SuppressWarnings("unchecked")
        List<List<?>> positions = (List<List<?>>) params.get(POSITIONS_PROPERTY);
        // N points produce N-1 line segments. Curves add points between the given positions.
        WallPathGeometry geometry = WallPathGeometry.fromPositions(positions);
        this.xEndpoints = geometry.xPointsAsFloats();
        this.yEndpoints = geometry.yPointsAsFloats();
        this.restitution = asFloat(params.get(RESTITUTION_PROPERTY));
        this.friction = asFloat(params.get(FRICTION_PROPERTY), Box2DFactory.DEFAULT_WALL_FRICTION);
    }

    @Override public void createBodies(World world) {
        if (getBooleanParameterValueForKey(IGNORE_BALL_PROPERTY)) {
            return;
        }
        for (int i = 1; i < xEndpoints.length; i++) {
            Body wall = Box2DFactory.createThinWall(
                    world, xEndpoints[i - 1], yEndpoints[i - 1], xEndpoints[i], yEndpoints[i],
                    restitution, friction);
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
