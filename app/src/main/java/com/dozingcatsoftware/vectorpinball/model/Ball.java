package com.dozingcatsoftware.vectorpinball.model;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.World;
import com.dozingcatsoftware.vectorpinball.elements.Box2DFactory;

/**
 * Represents a ball in play. Not part of the elements package because balls are created and
 * removed at "runtime" rather than being part of the table definition.
 */
public class Ball implements IDrawable {
    static class PreviousPosition {
        long nanos;
        float x;
        float y;
    }

    private WorldLayers worlds;
    private int layer;
    private Body body;
    private int primaryColor;
    private int secondaryColor;
    private String mostRecentSensorId;
    private PreviousPosition[] previousPositions = new PreviousPosition[20];
    private int previousPositionHeadIndex = -1;

    private Ball(
            WorldLayers worlds, int layer, Body body, int primaryColor, int secondaryColor) {
        this.worlds = worlds;
        this.layer = layer;
        this.body = body;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;

        for (int i = 0; i < previousPositions.length; i++) {
            previousPositions[i] = new PreviousPosition();
            previousPositions[i].nanos = -1;
        }
    }

    public static Ball create(
            WorldLayers worlds, int layer, float x, float y, float radius,
            int primaryColor, int secondaryColor) {
        Body body = createBody(worlds.existingWorldForLayer(layer), x, y, radius);
        return new Ball(worlds, layer, body, primaryColor, secondaryColor);
    }

    private static Body createBody(World world, float x, float y, float radius) {
        Body ballBody = Box2DFactory.createCircle(world, x, y, radius, false);
        ballBody.setBullet(true);
        // Default is radius of 0.5, if different we want the mass to be the same (could be
        // configurable if needed), so adjust density proportional to square of the radius.
        ballBody.getFixtureList().get(0).setDensity((0.5f*0.5f) / (radius*radius));
        ballBody.resetMassData();
        return ballBody;
    }

    public void tick(Field field, long nanos) {
        if (previousPositionHeadIndex == -1) {
            previousPositionHeadIndex = 0;
        }
        else {
            previousPositionHeadIndex = (previousPositionHeadIndex + 1) % previousPositions.length;
        }
        PreviousPosition pp = previousPositions[previousPositionHeadIndex];
        Vector2 pos = this.getPosition();
        pp.nanos = field.getGameTimeNanos();
        pp.x = pos.x;
        pp.y = pos.y;
    }

    @Override public void draw(Field field, IFieldRenderer renderer) {
        if (field.ballTrailsEnabled()) {
            drawTrails(field, renderer);
        }
        Vector2 center = this.getPosition();
        float radius = this.getRadius();
        renderer.fillCircle(center.x, center.y, radius, primaryColor);

        // Draw a smaller circle to show the ball's rotation.
        float angle = body.getAngle();
        float smallCenterX = center.x + (radius / 2) * MathUtils.cos(angle);
        float smallCenterY = center.y + (radius / 2) * MathUtils.sin(angle);
        renderer.fillCircle(smallCenterX, smallCenterY, radius / 4, secondaryColor);
    }

    private void drawTrails(Field field, IFieldRenderer renderer) {
        final int maxTrailImages = 25;
        final long nanosPerTrailImage = 12_000_000L;

        Vector2 center = this.getPosition();
        int numTrailsDrawn = 0;
        float prevX = center.x;
        float prevY = center.y;
        long now = field.getGameTimeNanos();
        long prevDiffNanos = 0;
        long nextTrailBoundary = nanosPerTrailImage;

        for (int i = 0; i < previousPositions.length && numTrailsDrawn < maxTrailImages; i++) {
            int pi = previousPositionHeadIndex - i;
            if (pi < 0) {
                pi += previousPositions.length;
            }
            PreviousPosition pp = previousPositions[pi];
            if (pp.nanos <= 0) {
                break;
            }

            long diffNanos = now - pp.nanos;
            while (diffNanos > nextTrailBoundary && numTrailsDrawn < maxTrailImages) {
                // Interpolate position.
                float positionFraction = ((float)(nextTrailBoundary - prevDiffNanos)) / (diffNanos - prevDiffNanos);
                float x = prevX + positionFraction * (pp.x - prevX);
                float y = prevY + positionFraction * (pp.y - prevY);
                // Trail circles get gradually smaller and more transparent.
                float sizeFraction = 1.0f - (numTrailsDrawn / (float) maxTrailImages);
                float radius = this.getRadius() * sizeFraction;
                int color = Color.blend(Color.withAlpha(primaryColor, 0), primaryColor, sizeFraction * 0.3f);
                renderer.fillCircle(x, y, radius, color);
                numTrailsDrawn += 1;
                nextTrailBoundary += nanosPerTrailImage;
            }
            prevDiffNanos = diffNanos;
            prevX = pp.x;
            prevY = pp.y;
        }
    }

    @Override public int getLayer() {
        return this.layer;
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

    public float getRadius() {
        CircleShape shape = (CircleShape)body.getFixtureList().get(0).getShape();
        return shape.getRadius();
    }

    public int getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(int primaryColor) {
        this.primaryColor = primaryColor;
    }

    public int getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(int secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public String getMostRecentSensorId() {
        return this.mostRecentSensorId;
    }

    public void setMostRecentSensorId(String sensorId) {
        this.mostRecentSensorId = sensorId;
    }

    public void moveToLayer(int newLayer) {
        if (layer == newLayer) {
            return;
        }
        Body oldBody = this.body;
        this.body = copyBodyToWorld(worlds.existingOrNewWorldForLayer(newLayer));
        this.layer = newLayer;
        oldBody.getWorld().destroyBody(oldBody);
    }

    private Body copyBodyToWorld(World world) {
        Vector2 position = this.body.getPosition();
        Body newBody = createBody(world, position.x, position.y, this.getRadius());
        newBody.setTransform(position.x, position.y, this.body.getAngle());
        newBody.setLinearVelocity(this.body.getLinearVelocity());
        newBody.setAngularVelocity(this.body.getAngularVelocity());
        return newBody;
    }

    void destroySelf() {
        this.getBody().getWorld().destroyBody(this.getBody());
    }
}
