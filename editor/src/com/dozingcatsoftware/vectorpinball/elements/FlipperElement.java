package com.dozingcatsoftware.vectorpinball.elements;

import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asFloat;
import static com.dozingcatsoftware.vectorpinball.util.MathUtils.toRadiansF;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.model.Field;
import com.dozingcatsoftware.vectorpinball.model.IFieldRenderer;
import com.dozingcatsoftware.vectorpinball.util.MathUtils;

/**
 * FieldElement subclass for a flipper that is controlled by the player. A flipper consists of a
 * Box2D RevoluteJoint where a thin wall rotates around an invisible anchor. Flippers are defined
 * in the layout JSON as follows:
 * {
 *     "class": "FlipperElement",
 *     "position": [5.5, 10], // x,y of fixed end of flipper which it rotates around
 *     "length": 2.5, // length of the flipper. Negative if the flipper rotates around its right
 *     end.
 *     "minangle": -20, // minimum angle from the horizontal. Negative angles are below horizontal.
 *     "maxangle": 20, // maximum angle from the horizontal.
 *     "upspeed": 7, // rate at which the flipper rotates up when activated (in radians/sec?)
 *     "downspeed": 3 // rate at which the flipper rotates down when not activated (in radians/sec?)
 * }
 */
public class FlipperElement extends FieldElement {

    public static final String POSITION_PROPERTY = "position";
    public static final String LENGTH_PROPERTY = "length";
    public static final String MIN_ANGLE_PROPERTY = "minangle";
    public static final String MAX_ANGLE_PROPERTY = "maxangle";
    public static final String UP_SPEED_PROPERTY = "upspeed";
    public static final String DOWN_SPEED_PROPERTY = "downspeed";

    static final int DEFAULT_COLOR = Color.fromRGB(0, 255, 0);

    Body flipperBody;
    List<Body> flipperBodySet;
    public Body anchorBody;
    public RevoluteJoint joint;
    RevoluteJointDef jointDef;

    float flipperLength; // Negative if flipper rotates around its right end.
    float upspeed, downspeed;
    float flipperDownAngle, flipperUpAngle;
    float cx, cy;  // Center of revolution.

    @Override
    public void finishCreateElement(Map<String, ?> params, FieldElementCollection collection) {
        @SuppressWarnings("unchecked")
        List<Object> pos = (List<Object>) params.get(POSITION_PROPERTY);

        this.cx = asFloat(pos.get(0));
        this.cy = asFloat(pos.get(1));
        this.flipperLength = asFloat(params.get(LENGTH_PROPERTY));
        this.upspeed = asFloat(params.get(UP_SPEED_PROPERTY));
        this.downspeed = asFloat(params.get(DOWN_SPEED_PROPERTY));

        // If the flipper rotates around its right end, flipperLength will be negative.
        // User-provided angles are reversed for negative lengths, so an angle of 0 will
        // extend the flipper to the left.
        float minAngle = toRadiansF(asFloat(params.get(MIN_ANGLE_PROPERTY)));
        float maxAngle = toRadiansF(asFloat(params.get(MAX_ANGLE_PROPERTY)));
        this.flipperDownAngle = isReversed() ? -minAngle : minAngle;
        this.flipperUpAngle = isReversed() ? -maxAngle : maxAngle;
    }

    @Override public void createBodies(World world) {
        this.anchorBody = Box2DFactory.createCircle(world, this.cx, this.cy, 0.05f, true);
        // The flipper needs to be slightly extended past anchorBody to rotate correctly.
        // If reversed, flipperLength is negative so the "segment" goes from the length to +0.05.
        float ext = isReversed() ? +0.05f : -0.05f;
        float startX = (float) (this.cx + ext * Math.cos(flipperDownAngle));
        float endX = (float) (this.cx + this.flipperLength * Math.cos(flipperDownAngle));
        float startY = (float) (this.cy + ext * Math.sin(flipperDownAngle));
        float endY = (float) (this.cy + this.flipperLength * Math.sin(flipperDownAngle));
        float midX = (startX + endX) / 2;
        float midY = (startY + endY) / 2;
        float flipperBodyLength = Math.abs(this.flipperLength) + Math.abs(ext);
        // Width larger than 0.12 slows rotation?
        this.flipperBody = Box2DFactory.createWall(
                world,
                midX - flipperBodyLength / 2,
                midY - 0.12f,
                midX + flipperBodyLength / 2,
                midY + 0.12f,
                flipperDownAngle);
        flipperBody.setType(BodyDef.BodyType.DynamicBody);
        flipperBody.setBullet(true);
        flipperBody.getFixtureList().get(0).setDensity(5.0f);

        jointDef = new RevoluteJointDef();
        jointDef.initialize(anchorBody, flipperBody, new Vector2(this.cx, this.cy));
        jointDef.enableLimit = true;
        jointDef.enableMotor = true;
        // The starting angle is 0 from the joint's perspective, so if the flipper rotates
        // counterclockwise (a "left" flipper), then 0 is the minimum joint angle, and if it rotates
        // clockwise then 0 is the maximum joint angle (and the minimum is negative).
        if (isReversed()) {
            jointDef.lowerAngle = flipperUpAngle - flipperDownAngle;
            jointDef.upperAngle = 0;
        }
        else {
            jointDef.lowerAngle = 0;
            jointDef.upperAngle = flipperUpAngle - flipperDownAngle;
        }
        jointDef.maxMotorTorque = 1000f;

        this.joint = (RevoluteJoint) world.createJoint(jointDef);

        flipperBodySet = Collections.singletonList(flipperBody);
        this.setEffectiveMotorSpeed(-this.downspeed); // Force flipper to bottom when field is first created.
    }

    /** Returns true if the flipper rotates around its right end. */
    boolean isReversed() {
        return (flipperLength < 0);
    }

    /** Returns true if the flipper should be activated by a left flipper button. */
    public boolean isLeftFlipper() {
        return !isReversed();
    }

    /** Returns true if the flipper should be activated by a right flipper button. */
    public boolean isRightFlipper() {
        return isReversed();
    }

    /**
     * Returns the motor speed of the Box2D joint, normalized to be positive when the flipper is
     * moving up.
     */
    float getEffectiveMotorSpeed() {
        float speed = joint.getMotorSpeed();
        return (isReversed()) ? -speed : speed;
    }

    /** Sets the motor speed of the Box2D joint. Positive values move the flipper up. */
    void setEffectiveMotorSpeed(float speed) {
        if (isReversed()) speed = -speed;
        joint.setMotorSpeed(speed);
    }

    @Override public List<Body> getBodies() {
        return flipperBodySet;
    }

    @Override public boolean shouldCallTick() {
        return true;
    }

    @Override public void tick(Field field, long nanos) {
        super.tick(field, nanos);

        // If angle is at maximum, reduce speed so that the ball won't fly off when it hits.
        if (getEffectiveMotorSpeed() > 0.5f) {
            float topAngle = (isReversed()) ? jointDef.lowerAngle : jointDef.upperAngle;
            if (Math.abs(topAngle - joint.getJointAngle()) < 0.05) {
                setEffectiveMotorSpeed(0.5f);
            }
        }
    }
    
    public boolean isFlipperEngaged() {
        return getEffectiveMotorSpeed() > 0;
    }

    public void setFlipperEngaged(boolean active) {
        // Only adjust speed if state is changing, so we don't accelerate flipper that's been
        // slowed down in tick()
        if (active != this.isFlipperEngaged()) {
            float speed = (active) ? upspeed : -downspeed;
            setEffectiveMotorSpeed(speed);
        }
    }

    @Override public void draw(Field field, IFieldRenderer renderer) {
        // Draw single line segment from anchor point.
        Vector2 position = anchorBody.getPosition();
        // Angle can briefly get out of range, always draw between min and max.
        // Also compensate for a 0 joint angle actually being this.lowerAngle.
        float jointAngle = MathUtils.clamp(
                joint.getJointAngle(), jointDef.lowerAngle, jointDef.upperAngle);
        float actualAngle = flipperDownAngle + jointAngle;
        float x1 = position.x;
        float y1 = position.y;
        float x2 = position.x + flipperLength * (float) Math.cos(actualAngle);
        float y2 = position.y + flipperLength * (float) Math.sin(actualAngle);

        renderer.drawLine(x1, y1, x2, y2, currentColor(DEFAULT_COLOR));
    }
}
