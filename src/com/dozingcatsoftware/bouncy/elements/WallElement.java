package com.dozingcatsoftware.bouncy.elements;

import static com.dozingcatsoftware.bouncy.util.MathUtils.asFloat;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.dozingcatsoftware.bouncy.Field;
import com.dozingcatsoftware.bouncy.IFieldRenderer;

/**
 * FieldElement subclass that represents a straight wall. Its position is specified by the
 * "position" parameter with 4 values, which are [start x, start y, end x, end y]. There are
 * several optional parameters to customize the wall's behavior:
 * "kick": impulse to apply when a ball hits the wall, used for kickers and ball savers.
 * "kill": if true, the ball is lost when it hits the wall. Tables normally have an invisible wall
 *         below the flippers with this property set.
 * "retractWhenHit": if true, the wall is removed when hit by a ball.
 * "disabled": if true, the wall starts out retracted, and will only be shown when
 *             setRetracted(field, true) is called.
 * "ignoreBall": if true, the wall will be drawn but will not interact with the ball.
 *
 * A wall can be removed from the field by calling setRetracted(field, true). A retracted wall will
 * not be drawn and will not interact with the ball. setRetracted(field, true) will restore it.
 */

public class WallElement extends FieldElement {

    public static final String POSITION_PROPERTY = "position";
    public static final String RESTITUTION_PROPERTY = "restitution";
    public static final String KICK_PROPERTY = "kick";
    public static final String KILL_PROPERTY = "kill";
    public static final String RETRACT_WHEN_HIT_PROPERTY = "retractWhenHit";
    public static final String IGNORE_BALL_PROPERTY = "ignoreBall";
    public static final String DISABLED_PROPERTY = "disabled";

    Body wallBody;
    List<Body> bodySet;
    float x1, y1, x2, y2;
    float kick;

    boolean killBall;
    boolean retractWhenHit;
    float restitution;
    boolean disabled;
    boolean ignoreBall;

    @Override public void finishCreateElement(Map<String, ?> params, FieldElementCollection collection) {
        List<?> pos = (List<?>)params.get(POSITION_PROPERTY);
        this.x1 = asFloat(pos.get(0));
        this.y1 = asFloat(pos.get(1));
        this.x2 = asFloat(pos.get(2));
        this.y2 = asFloat(pos.get(3));
        this.restitution = asFloat(params.get(RESTITUTION_PROPERTY));

        this.kick = asFloat(params.get(KICK_PROPERTY));
        this.killBall = (Boolean.TRUE.equals(params.get(KILL_PROPERTY)));
        this.retractWhenHit = (Boolean.TRUE.equals(params.get(RETRACT_WHEN_HIT_PROPERTY)));
        this.disabled = Boolean.TRUE.equals(params.get(DISABLED_PROPERTY));
        this.ignoreBall = Boolean.TRUE.equals(params.get(IGNORE_BALL_PROPERTY));
    }

    @Override public void createBodies(World world) {
        if (ignoreBall) {
            bodySet = Collections.emptyList();
            return;
        }

        wallBody = Box2DFactory.createThinWall(world, x1, y1, x2, y2, restitution);
        bodySet = Collections.singletonList(wallBody);
        if (disabled) {
            setRetracted(true);
        }
    }

    public boolean isRetracted() {
        return wallBody!=null && !wallBody.isActive();
    }

    public void setRetracted(boolean retracted) {
        if (retracted!=this.isRetracted()) {
            wallBody.setActive(!retracted);
        }
    }

    @Override public List<Body> getBodies() {
        return bodySet;
    }

    @Override public boolean shouldCallTick() {
        // tick() only needs to be called if this wall provides a kick which makes it flash.
        return (this.kick > 0.01f);
    }

    Vector2 impulseForBall(Body ball) {
        if (this.kick <= 0.01f) return null;
        // Rotate wall direction 90 degrees for normal, choose direction toward ball.
        float ix = this.y2 - this.y1;
        float iy = this.x1 - this.x2;
        float mag = (float)Math.sqrt(ix*ix + iy*iy);
        float scale = this.kick / mag;
        ix *= scale;
        iy *= scale;

        // Dot product of (ball center - wall center) and impulse direction should be positive,
        // if not flip impulse.
        Vector2 balldiff = ball.getWorldCenter().cpy().sub(this.x1, this.y1);
        float dotprod = balldiff.x*ix + balldiff.y*iy;
        if (dotprod < 0) {
            ix = -ix;
            iy = -iy;
        }
        return new Vector2(ix, iy);
    }


    @Override public void handleCollision(Body ball, Body bodyHit, Field field) {
        if (retractWhenHit) {
            this.setRetracted(true);
        }

        if (killBall) {
            field.removeBall(ball);
        }
        else {
            Vector2 impulse = this.impulseForBall(ball);
            if (impulse!=null) {
                ball.applyLinearImpulse(impulse, ball.getWorldCenter(), true);
                flashForFrames(3);
            }
        }
    }

    @Override public void draw(IFieldRenderer renderer) {
        if (isRetracted()) return;
        renderer.drawLine(x1, y1, x2, y2, currentColor(DEFAULT_WALL_COLOR));
    }
}
