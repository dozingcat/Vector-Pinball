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
 * Spinner that is displayed by flashing a circle on and off with gradually reducing frequency.
 * The spinner is activated when a ball passes over it with a minimum speed. The spinner activates
 * with a frequency based on the ball's speed, and scores points every time it cycles.
 */
public class SpinnerElement extends FieldElement {

    enum Status {INACTIVE, ACTIVE_VISIBLE, ACTIVE_INVISIBLE}

    public static final String CENTER_PROPERTY = "position";
    public static final String RADIUS_PROPERTY = "radius";
    public static final String MIN_ACTIVATION_SPEED_PROPERTY = "minActivationSpeed";
    public static final String BASE_CYCLES_PER_SECOND_PROPERTY = "baseCyclesPerSecond";
    public static final String MIN_CYCLES_PER_SECOND_PROPERTY = "minCyclesPerSecond";
    public static final String SPEED_DECAY_RATE_PROPERTY = "speedDecayRate";

    private static final long MIN_NANOS_BETWEEN_SCORE_ANIMATIONS = 250_000_000;
    private static final int DEFAULT_COLOR = Color.fromRGB(224, 224, 224);

    float cx, cy;
    float radius;
    float minActivationSpeed;
    float baseCyclesPerSecond;
    float minCyclesPerSecond;
    float currentCyclesPerSecond;
    float speedDecayRate;
    private Status status = Status.INACTIVE;
    private long nanosToNextCycle;
    private long nanosSinceLastScoreAnimation;

    @Override
    public void finishCreateElement(Map<String, ?> params, FieldElementCollection collection) {
        List<?> pos = (List<?>) params.get(CENTER_PROPERTY);
        this.radius = asFloat(params.get(RADIUS_PROPERTY));
        this.cx = asFloat(pos.get(0));
        this.cy = asFloat(pos.get(1));
        this.minActivationSpeed = asFloat(params.get(MIN_ACTIVATION_SPEED_PROPERTY), 4f);
        this.baseCyclesPerSecond = asFloat(params.get(BASE_CYCLES_PER_SECOND_PROPERTY), 8f);
        this.minCyclesPerSecond = asFloat(params.get(MIN_CYCLES_PER_SECOND_PROPERTY), 1.5f);
        this.speedDecayRate = asFloat(params.get(SPEED_DECAY_RATE_PROPERTY), 0.9f);
    }

    @Override
    public void createBodies(World world) {
        // No physical components
    }

    @Override
    public List<Body> getBodies() {
        return Collections.emptyList();
    }

    @Override
    public void draw(Field field, IFieldRenderer renderer) {
        if (status == Status.ACTIVE_VISIBLE) {
            renderer.fillCircle(cx, cy, radius, currentColor(DEFAULT_COLOR));
        }
    }

    private Ball ballInActivationRange(Field field) {
        List<Ball> balls = field.getBalls();
        for (int i = 0; i < balls.size(); i++) {
            Ball b = balls.get(i);
            Vector2 bpos = b.getPosition();
            float dist2 = (bpos.x - cx) * (bpos.x - cx) + (bpos.y - cy) * (bpos.y - cy);
            if (dist2 <= radius * radius) {
                float ballSpeed = b.getLinearVelocity().len();
                if (ballSpeed >= minActivationSpeed) {
                    return b;
                }
            }
        }
        return null;
    }

    private void startSpinnerForBall(Ball b) {
        assert status == Status.INACTIVE;
        float ballSpeed = b.getLinearVelocity().len();
        currentCyclesPerSecond = (ballSpeed / minActivationSpeed) * baseCyclesPerSecond;
        nanosToNextCycle = (long) (1_000_000_000 / currentCyclesPerSecond);
        nanosSinceLastScoreAnimation = MIN_NANOS_BETWEEN_SCORE_ANIMATIONS;
        status = Status.ACTIVE_INVISIBLE;
    }

    @Override public boolean shouldCallTick() {
        return true;
    }

    @Override
    public void tick(Field field, long nanos) {
        if (status == Status.INACTIVE) {
            // Start the spinner if a ball is moving across it with sufficient speed.
            // Note that an already active spinner won't have its speed increased if a faster ball
            // crosses it; might want to add that in the future.
            Ball b = ballInActivationRange(field);
            if (b != null) {
                startSpinnerForBall(b);
                field.getDelegate().spinnerActivated(field, this, b);
            }
        }
        else {
            nanosToNextCycle -= nanos;
            nanosSinceLastScoreAnimation += nanos;
            if (nanosToNextCycle <= 0) {
                // Add score and reduce spinner speed, stopping if it's below minCyclesPerSecond.
                status = (status == Status.ACTIVE_VISIBLE) ? Status.ACTIVE_INVISIBLE : Status.ACTIVE_VISIBLE;
                if (nanosSinceLastScoreAnimation >= MIN_NANOS_BETWEEN_SCORE_ANIMATIONS) {
                    field.addScoreWithAnimation(score, cx, cy);
                    nanosSinceLastScoreAnimation = 0;
                }
                else {
                    field.addScore(score);
                }
                currentCyclesPerSecond *= speedDecayRate;
                nanosToNextCycle = (long) (1_000_000_000 / currentCyclesPerSecond);
                if (status == Status.ACTIVE_INVISIBLE && currentCyclesPerSecond < minCyclesPerSecond) {
                    status = Status.INACTIVE;
                }
            }
        }
    }
}
