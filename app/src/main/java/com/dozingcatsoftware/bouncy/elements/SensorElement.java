package com.dozingcatsoftware.bouncy.elements;

import static com.dozingcatsoftware.bouncy.util.MathUtils.asFloat;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.dozingcatsoftware.bouncy.Ball;
import com.dozingcatsoftware.bouncy.Field;
import com.dozingcatsoftware.bouncy.IFieldRenderer;

/**
 * This FieldElement subclass is used to identify areas on the table that should cause custom
 * behavior when the ball enters. A SensorElement has no bodies and don't draw anything. The area
 * it monitors is a rectangle defined by the "rect" parameter as a [xmin,ymin,xmax,ymax] list.
 * During every tick() invocation, a sensor determines if any of the field's balls are within its
 * area, and if so calls the field delegate's ballInSensorRange method.
 */

public class SensorElement extends FieldElement {

    public static final String RECT_PROPERTY = "rect";

    float xmin, ymin, xmax, ymax;

    @Override public void finishCreateElement(Map<String, ?> params, FieldElementCollection collection) {
        List<?> rectPos = (List<?>)params.get(RECT_PROPERTY);
        this.xmin = Math.min(asFloat(rectPos.get(0)), asFloat(rectPos.get(2)));
        this.ymin = Math.min(asFloat(rectPos.get(1)), asFloat(rectPos.get(3)));
        this.xmax = Math.max(asFloat(rectPos.get(0)), asFloat(rectPos.get(2)));
        this.ymax = Math.max(asFloat(rectPos.get(1)), asFloat(rectPos.get(3)));
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
        if (bpos.x<xmin || bpos.x>xmax || bpos.y<ymin || bpos.y>ymax) {
            return false;
        }
        return true;
    }

    @Override public void tick(Field field) {
        List<Ball> balls = field.getBalls();
        for(int i=0; i<balls.size(); i++) {
            Ball ball = balls.get(i);
            if (ballInRange(ball)) {
                field.getDelegate().ballInSensorRange(field, this, ball);
                return;
            }
        }
    }

    @Override public List<Body> getBodies() {
        return Collections.emptyList();
    }

    @Override public void draw(IFieldRenderer renderer) {
        // No UI.
    }
}
