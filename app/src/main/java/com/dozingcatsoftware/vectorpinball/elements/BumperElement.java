package com.dozingcatsoftware.vectorpinball.elements;

import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asFloat;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.dozingcatsoftware.vectorpinball.model.Ball;
import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.model.Field;
import com.dozingcatsoftware.vectorpinball.model.IFieldRenderer;

/**
 * This FieldElement subclass represents a bumper that applies an impulse to a ball when it hits.
 * The impulse magnitude is controlled by the "kick" parameter in the configuration map.
 */
public class BumperElement extends FieldElement {

    public static final String POSITION_PROPERTY = "position";
    public static final String RADIUS_PROPERTY = "radius";
    public static final String KICK_PROPERTY = "kick";
    public static final String OUTER_RADIUS_PROPERTY = "outerRadius";
    public static final String OUTER_COLOR_PROPERTY = "outerColor";
    public static final String INACTIVE_LAYER_OUTER_COLOR_PROPERTY = "inactiveLayerOuterColor";

    static final int DEFAULT_COLOR = Color.fromRGB(0, 0, 255);
    static final int DEFAULT_OUTER_COLOR = Color.fromRGBA(0, 0, 255, 128);

    Body bumperBody;
    List<Body> bumperBodySet;

    float radius, outerRadius;
    float cx, cy;
    float kick;
    int outerColor;
    Integer newOuterColor;
    Integer inactiveLayerOuterColor;

    @Override
    public void finishCreateElement(Map<String, ?> params, FieldElementCollection collection) {
        List<?> pos = (List<?>) params.get(POSITION_PROPERTY);
        this.radius = asFloat(params.get(RADIUS_PROPERTY));
        this.outerRadius = asFloat(params.get(OUTER_RADIUS_PROPERTY));
        this.cx = asFloat(pos.get(0));
        this.cy = asFloat(pos.get(1));
        this.kick = asFloat(params.get(KICK_PROPERTY));
        this.outerColor = params.containsKey(OUTER_COLOR_PROPERTY) ?
                Color.fromList((List<Number>) params.get(OUTER_COLOR_PROPERTY)) :
                DEFAULT_OUTER_COLOR;
        if (params.containsKey(INACTIVE_LAYER_OUTER_COLOR_PROPERTY)) {
            this.inactiveLayerOuterColor =
                    Color.fromList((List<Number>) params.get(INACTIVE_LAYER_OUTER_COLOR_PROPERTY));
        }
    }

    @Override public void createBodies(World world) {
        if (radius > 0) {
            bumperBody = Box2DFactory.createCircle(world, cx, cy, radius, true);
            bumperBodySet = Collections.singletonList(bumperBody);
        } else {
            bumperBodySet = Collections.emptyList();
        }
    }

    @Override public List<Body> getBodies() {
        return bumperBodySet;
    }

    @Override public boolean shouldCallTick() {
        // Needs to call tick() to decrement flash counter, but can use superclass implementation.
        return true;
    }

    Vector2 impulseForBall(Ball ball) {
        if (this.kick <= 0.01f) return null;
        // Compute unit vector from center of bumper to ball, and scale by kick value to get impulse.
        Vector2 ballpos = ball.getPosition();
        Vector2 thisPos = bumperBody.getPosition();
        float ix = ballpos.x - thisPos.x;
        float iy = ballpos.y - thisPos.y;
        float mag = (float) Math.hypot(ix, iy);
        float scale = this.kick / mag;
        return new Vector2(ix * scale, iy * scale);
    }

    @Override public void handleCollision(Ball ball, Body bodyHit, Field field) {
        Vector2 impulse = this.impulseForBall(ball);
        if (impulse != null) {
            ball.applyLinearImpulse(impulse);
            flashForNanos(100_000_000);
        }
    }

    public Vector2 getCenter() {
        return new Vector2(this.cx, this.cy);
    }

    public void setCenter(float cx, float cy) {
        this.cx = cx;
        this.cy = cy;
        if (this.bumperBody != null) {
            this.bumperBody.setTransform(cx, cy, this.bumperBody.getAngle());
        }
    }

    public void setNewOuterColor(Integer color) {
        this.newOuterColor = color;
    }

    @Override public void draw(Field field, IFieldRenderer renderer) {
        if (outerRadius > 0) {
            int baseOuterColor = this.newOuterColor != null ? this.newOuterColor : this.outerColor;
            int currentOuterColor = colorApplyingLayerOrFlash(
                    baseOuterColor, this.inactiveLayerOuterColor);
            renderer.fillCircle(this.cx, this.cy, outerRadius, currentOuterColor);
        }
        if (radius > 0) {
            int currentInnerColor = currentColor(DEFAULT_COLOR);
            renderer.fillCircle(this.cx, this.cy, radius, currentInnerColor);
        }
    }
}
