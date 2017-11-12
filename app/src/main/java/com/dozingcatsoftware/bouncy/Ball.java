package com.dozingcatsoftware.bouncy;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.World;
import com.dozingcatsoftware.bouncy.elements.Box2DFactory;

/**
 * Represents a ball in play. Not part of the elements package because balls are created and
 * removed at "runtime" rather than being part of the table definition.
 */
public class Ball {
    private final Body body;
    private Color primaryColor;
    private Color secondaryColor;

    private Ball(Body body, Color primaryColor, Color secondaryColor) {
        this.body = body;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
    }

    public static Ball create(World world, float x, float y, float radius,
            Color primaryColor, Color secondaryColor) {
        Body ballBody = Box2DFactory.createCircle(world, x, y, radius, false);
        ballBody.setBullet(true);
        // Default is radius of 0.5, if different we want the mass to be the same (could be
        // configurable if needed), so adjust density proportional to square of the radius.
        if (radius != 0.5f) {
            ballBody.getFixtureList().get(0).setDensity((0.5f*0.5f) / (radius*radius));
            ballBody.resetMassData();
        }
        return new Ball(ballBody, primaryColor, secondaryColor);
    }

    public void draw(IFieldRenderer renderer) {
        CircleShape shape = (CircleShape)body.getFixtureList().get(0).getShape();
        Vector2 center = body.getPosition();
        float radius = shape.getRadius();
        renderer.fillCircle(center.x, center.y, radius, primaryColor);

        // Draw a smaller circle to show the ball's rotation.
        float angle = body.getAngle();
        float smallCenterX = center.x + (radius / 2) * MathUtils.cos(angle);
        float smallCenterY = center.y + (radius / 2) * MathUtils.sin(angle);
        renderer.fillCircle(smallCenterX, smallCenterY, radius / 4, secondaryColor);
    }

    public Vector2 getPosition() {
        return body.getPosition();
    }

    public Vector2 getLinearVelocity() {
        return body.getLinearVelocity();
    }

    public void applyLinearImpulse(Vector2 impulse) {
        body.applyLinearImpulse(impulse, body.getWorldCenter(), true);
    }

    public Body getBody() {
        return body;
    }

    public Color getPrimaryColor() {
        return primaryColor;
    }
    public void setPrimaryColor(Color primaryColor) {
        this.primaryColor = primaryColor;
    }

    public Color getSecondaryColor() {
        return secondaryColor;
    }
    public void setSecondaryColor(Color secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

}
