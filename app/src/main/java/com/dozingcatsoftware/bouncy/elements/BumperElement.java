package com.dozingcatsoftware.bouncy.elements;

import static com.dozingcatsoftware.bouncy.util.MathUtils.asFloat;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.dozingcatsoftware.bouncy.Ball;
import com.dozingcatsoftware.bouncy.Color;
import com.dozingcatsoftware.bouncy.Field;
import com.dozingcatsoftware.bouncy.IFieldRenderer;

/**
 * This FieldElement subclass represents a bumper that applies an impulse to a ball when it hits.
 * The impulse magnitude is controlled by the "kick" parameter in the configuration map.
 */

public class BumperElement extends FieldElement {

    public static final String POSITION_PROPERTY = "position";
    public static final String RADIUS_PROPERTY = "radius";
    public static final String KICK_PROPERTY = "kick";

    static final Color DEFAULT_COLOR = Color.fromRGB(0, 0, 255);

    Body bumperBody;
    List<Body> bumperBodySet;

    float radius;
    float cx, cy;
    float kick;

    @Override public void finishCreateElement(Map<String, ?> params, FieldElementCollection collection) {
        List<?> pos = (List<?>)params.get(POSITION_PROPERTY);
        this.radius = asFloat(params.get(RADIUS_PROPERTY));
        this.cx = asFloat(pos.get(0));
        this.cy = asFloat(pos.get(1));
        this.kick = asFloat(params.get(KICK_PROPERTY));
    }

    @Override public void createBodies(World world) {
        bumperBody = Box2DFactory.createCircle(world, cx, cy, radius, true);
        bumperBodySet = Collections.singletonList(bumperBody);
    }

    @Override public List<Body> getBodies() {
        return bumperBodySet;
    }

    @Override public boolean shouldCallTick() {
        // Needs to call tick to decrement flash counter, but can use superclass tick() implementation.
        return true;
    }


    Vector2 impulseForBall(Ball ball) {
        if (this.kick <= 0.01f) return null;
        // Compute unit vector from center of bumper to ball, and scale by kick value to get impulse.
        Vector2 ballpos = ball.getPosition();
        Vector2 thisPos = bumperBody.getPosition();
        float ix = ballpos.x - thisPos.x;
        float iy = ballpos.y - thisPos.y;
        float mag = (float)Math.hypot(ix, iy);
        float scale = this.kick / mag;
        return new Vector2(ix*scale, iy*scale);
    }

    @Override public void handleCollision(Ball ball, Body bodyHit, Field field) {
        Vector2 impulse = this.impulseForBall(ball);
        if (impulse!=null) {
            ball.applyLinearImpulse(impulse);
            flashForFrames(3);
        }
    }

    @Override public void draw(IFieldRenderer renderer) {
        float px = bumperBody.getPosition().x;
        float py = bumperBody.getPosition().y;
        renderer.fillCircle(px, py, radius, currentColor(DEFAULT_COLOR));
    }
}
