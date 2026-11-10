package com.dozingcatsoftware.vectorpinball.elements;

import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asFloat;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.dozingcatsoftware.vectorpinball.model.Ball;
import com.dozingcatsoftware.vectorpinball.model.Field;
import com.dozingcatsoftware.vectorpinball.model.IFieldRenderer;

/**
 * This FieldElement subclass is used to identify areas on the table that should cause custom
 * behavior when the ball enters. A SensorElement has no bodies and doesn't draw anything. The area
 * it monitors is a rectangle defined by the "rect" parameter as a [xmin,ymin,xmax,ymax] list.
 * During every tick() invocation, a sensor determines if any of the field's balls are within its
 * area, and if so calls the field delegate's ballInSensorRange method.
 */
public class SensorElement extends FieldElement {

    public static final String RECT_PROPERTY = "rect";
    public static final String BALL_LAYER_TO_PROPERTY = "ballLayer";
    public static final String BALL_LAYER_FROM_PROPERTY = "ballLayerFrom";
    public static final String RECORD_BALL_TIMES_PROPERTY = "recordBallTimes";

    float xmin, ymin, xmax, ymax;
    private Number layerTo;
    private Number layerFrom;
    boolean recordBallTimes;
    WeakHashMap<Ball, Long> ballEntryTimestamps;

    @Override public void finishCreateElement(
            Map<String, ?> params, FieldElementCollection collection) {
        List<?> rectPos = (List<?>) params.get(RECT_PROPERTY);
        this.xmin = Math.min(asFloat(rectPos.get(0)), asFloat(rectPos.get(2)));
        this.ymin = Math.min(asFloat(rectPos.get(1)), asFloat(rectPos.get(3)));
        this.xmax = Math.max(asFloat(rectPos.get(0)), asFloat(rectPos.get(2)));
        this.ymax = Math.max(asFloat(rectPos.get(1)), asFloat(rectPos.get(3)));
        this.layerFrom = (Number)params.get(BALL_LAYER_FROM_PROPERTY);
        this.layerTo = (Number)params.get(BALL_LAYER_TO_PROPERTY);
        this.recordBallTimes = Boolean.TRUE.equals(params.get(RECORD_BALL_TIMES_PROPERTY));
        if (this.recordBallTimes) {
            this.ballEntryTimestamps = new WeakHashMap<>();
        }
    }

    @Override public void createBodies(World world) {
        // Not needed.
    }

    @Override public boolean shouldCallTick() {
        return true;
    }

    boolean ballInRange(Ball ball) {
        Vector2 bpos = ball.getPosition();
        // Test against rect.
        if (bpos.x < xmin || bpos.x > xmax || bpos.y < ymin || bpos.y > ymax) {
            return false;
        }
        // Only trigger the sensor if the "from" layer is empty or it matches the ball.
        if (this.layerFrom != null && this.layerFrom.intValue() != ball.getLayer()) {
            return false;
        }
        return true;
    }

    @Override public void tick(Field field, long nanos) {
        List<Ball> balls = field.getBalls();
        for (int i = 0; i < balls.size(); i++) {
            Ball ball = balls.get(i);
            if (ballInRange(ball)) {
                if (this.layerTo != null) {
                    ball.moveToLayer(this.layerTo.intValue());
                }
                if (this.recordBallTimes && !this.ballEntryTimestamps.containsKey(ball)) {
                    this.ballEntryTimestamps.put(ball, field.getGameTimeNanos());
                }
                field.getDelegate().ballInSensorRange(field, this, ball);
                ball.setMostRecentSensorId(this.getElementId());
            }
            else {
                if (this.recordBallTimes) {
                    this.ballEntryTimestamps.remove(ball);
                }
            }
        }
    }

    @Override public List<Body> getBodies() {
        return Collections.emptyList();
    }

    @Override public void draw(Field field, IFieldRenderer renderer) {
        // No UI.
    }

    /**
     * If `ball` is in this sensor's range (as of the most recent call of the tick method),
     * returns the game timestamp in nanoseconds that the ball first came in range,
     * otherwise returns null.
     */
    public Long ballEntryTimeNanos(Ball ball) {
        return this.ballEntryTimestamps != null ? this.ballEntryTimestamps.get(ball) : null;
    }
}
