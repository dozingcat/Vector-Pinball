package com.dozingcatsoftware.vectorpinball.elements;

import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asFloat;

import java.util.ArrayList;
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
 * This class represents a collection of rollover elements. They are activated (and optionally
 * deactivated) when a ball passes over them. Individual rollovers in the group are represented by
 * instances of the Rollover nested class, which specify center, radius, and color. Parameters at
 * the collection level control whether the rollovers should cycle when flippers are activated,
 * and whether rollovers can toggle from on to off.
 */
public class RolloverGroupElement extends FieldElement {

    public static final String TOGGLE_OFF_PROPERTY = "toggleOff";
    public static final String CYCLE_ON_FLIPPER_PROPERTY = "cycleOnFlipper";
    public static final String IGNORE_BALL_PROPERTY = "ignoreBall";
    public static final String RADIUS_PROPERTY = "radius";
    public static final String RESET_DELAY_PROPERTY = "reset";
    public static final String ROLLOVERS_PROPERTY = "rollovers";
    // For individual rollovers.
    public static final String POSITION_PROPERTY = "position";
    public static final String COLOR_PROPERTY = "color";
    public static final String SCORE_PROPERTY = "score";

    static class Rollover {
        Vector2 position;
        float radius;
        float radiusSquared; // Optimization when computing whether ball is in range.
        Integer color;
        long score;
        float resetDelay;
    }

    static final int DEFAULT_COLOR = Color.fromRGB(0, 255, 0);

    boolean cycleOnFlipper;
    boolean canToggleOff;
    boolean ignoreBall;
    float defaultRadius;
    float defaultResetDelay;
    List<Rollover> rollovers = new ArrayList<>();
    List<Rollover> activeRollovers = new ArrayList<>();
    List<Rollover> rolloversHitOnPreviousTick = new ArrayList<>();
    boolean isVisible = true;

    @SuppressWarnings("unchecked")
    @Override public void finishCreateElement(
            Map<String, ?> params, FieldElementCollection collection) {
        this.canToggleOff = Boolean.TRUE.equals(params.get(TOGGLE_OFF_PROPERTY));
        this.cycleOnFlipper = Boolean.TRUE.equals(params.get(CYCLE_ON_FLIPPER_PROPERTY));
        this.ignoreBall = Boolean.TRUE.equals(params.get(IGNORE_BALL_PROPERTY));
        this.defaultRadius = asFloat(params.get(RADIUS_PROPERTY));
        this.defaultResetDelay = asFloat(params.get(RESET_DELAY_PROPERTY));

        List<Map<String, ?>> rolloverMaps = (List<Map<String, ?>>) params.get(ROLLOVERS_PROPERTY);
        for (Map<String, ?> rmap : rolloverMaps) {
            Rollover rollover = new Rollover();
            rollovers.add(rollover);

            List<?> pos = (List<?>) rmap.get(POSITION_PROPERTY);
            rollover.position = new Vector2(asFloat(pos.get(0)), asFloat(pos.get(1)));
            // radius, color, score, and reset delay can be specified for each rollover.
            // If not present use default from group.
            rollover.radius = (rmap.containsKey(RADIUS_PROPERTY)) ?
                    asFloat(rmap.get(RADIUS_PROPERTY)) : this.defaultRadius;
            rollover.color = (rmap.containsKey(COLOR_PROPERTY))
                    ? Color.fromList((List<Number>) rmap.get(COLOR_PROPERTY)) : null;
            rollover.score = (rmap.containsKey(SCORE_PROPERTY)) ?
                    ((Number) rmap.get(SCORE_PROPERTY)).longValue() : this.score;
            rollover.resetDelay = (rmap.containsKey(RESET_DELAY_PROPERTY)) ?
                    asFloat(rmap.get(RESET_DELAY_PROPERTY)) : this.defaultResetDelay;

            rollover.radiusSquared = rollover.radius * rollover.radius;
        }
    }

    @Override public void createBodies(World world) {
        // Not needed.
    }

    @Override public List<Body> getBodies() {
        return Collections.emptyList();
    }

    /** Sets `hitRollovers` to the rollovers which have `ball` within their specified radius. */
    private void getRolloversHitByBall(Ball ball, List<Rollover> hitRollovers) {
        hitRollovers.clear();
        if (ball.getLayer() != this.getLayer()) {
            return;
        }
        int rsize = this.rollovers.size();
        for(int i = 0; i < rsize; i++) {
            Rollover rollover = this.rollovers.get(i);
            Vector2 position = ball.getPosition();
            float xdiff = position.x - rollover.position.x;
            float ydiff = position.y - rollover.position.y;
            float distanceSquared = xdiff * xdiff + ydiff * ydiff;
            if (distanceSquared <= rollover.radiusSquared) {
                hitRollovers.add(rollover);
            }
        }
    }

    /** Returns true if all rollovers in the group are active. */
    public boolean allRolloversActive() {
        return activeRollovers.size() == rollovers.size();
    }

    /** Activates the first unactivated rollover in the group. Has no effect if all are active.
     */
    public void activateFirstUnactivatedRollover() {
        int rsize = this.rollovers.size();
        for (int i = 0; i < rsize; i++) {
            Rollover rollover = this.rollovers.get(i);
            if (!activeRollovers.contains(rollover)) {
                activeRollovers.add(rollover);
                break;
            }
        }
    }

    public int numberOfRollovers() {
        return rollovers.size();
    }

    public boolean isRolloverActiveAtIndex(int index) {
        return activeRollovers.contains(rollovers.get(index));
    }

    public void setRolloverActiveAtIndex(int index, boolean active) {
        Rollover r = rollovers.get(index);
        if (active) {
            if (!activeRollovers.contains(r)) activeRollovers.add(r);
        }
        else {
            activeRollovers.remove(r);
        }
    }

    public Vector2 getRolloverCenterAtIndex(int index) {
        return rollovers.get(index).position;
    }

    public void setRolloverCenterAtIndex(int index, double x, double y) {
        Rollover r = rollovers.get(index);
        r.position.x = (float) x;
        r.position.y = (float) y;
    }

    public float getRolloverRadiusAtIndex(int index) {
        return rollovers.get(index).radius;
    }

    public void setRolloverRadiusAtIndex(int index, float radius) {
        Rollover r = rollovers.get(index);
        r.radius = radius;
        r.radiusSquared = radius * radius;
    }

    public void setRolloverColorAtIndex(int index, Integer color) {
        rollovers.get(index).color = color;
    }

    @Override public boolean shouldCallTick() {
        return true;
    }

    // Reuse these to avoid allocating memory in tick().
    List<Rollover> rolloversHitByBall = new ArrayList<>();
    List<Rollover> allHitRollovers = new ArrayList<>();

    @Override public void tick(Field field, long nanos) {
        super.tick(field, nanos);
        if (this.ignoreBall) return;

        boolean allActivePrevious = this.allRolloversActive();
        allHitRollovers.clear();
        List<Ball> balls = field.getBalls();
        // With multiple balls this may not behave as expected, for example if two balls
        // simultaneously activate the two remaining inactive rollovers, the ball object passed
        // to `allRolloversInGroupActivated` will arbitrarily be one of them.
        for (int i = 0; i < balls.size(); i++) {
            final Ball ball = balls.get(i);
            getRolloversHitByBall(ball, rolloversHitByBall);
            for (int j = 0; j < rolloversHitByBall.size(); j++) {
                final Rollover r = rolloversHitByBall.get(j);
                if (allHitRollovers.contains(r)) {
                    continue;
                }
                allHitRollovers.add(r);
                if (rolloversHitOnPreviousTick.contains(r)) {
                    continue;
                }
                // Inactive rollover becomes active, active rollover becomes inactive if toggleOff
                // setting is true. Add score whenever the state changes.
                if (!activeRollovers.contains(r)) {
                    activeRollovers.add(r);
                    field.addScore(r.score);
                    field.getAudioPlayer().playRollover();
                    // Set timer to clear rollover if reset parameter is present and >0.
                    if (r.resetDelay > 0) {
                        field.scheduleAction((long)(r.resetDelay*1000), () -> activeRollovers.remove(r));
                    }
                    // Notify delegate if all rollovers are now active and they weren't previously.
                    if (!allActivePrevious && allRolloversActive()) {
                        field.getDelegate().allRolloversInGroupActivated(field, this, ball);
                    }
                }
                else if (this.canToggleOff) {
                    activeRollovers.remove(r);
                    field.addScore(r.score);
                    field.getAudioPlayer().playRollover();
                }
            }
        }

        rolloversHitOnPreviousTick.clear();
        rolloversHitOnPreviousTick.addAll(allHitRollovers);
    }

    @Override public void flippersActivated(Field field, List<FlipperElement> flippers) {
        if (this.cycleOnFlipper) {
            // Cycle to right if any right flipper is activated.
            boolean hasRightFlipper = false;
            for (int i = 0; !hasRightFlipper && i < flippers.size(); i++) {
                hasRightFlipper = flippers.get(i).isRightFlipper();
            }
            this.cycleRollovers(hasRightFlipper);
        }
    }

    List<Rollover> newActiveRollovers = new ArrayList<>();

    /**
     * Cycles the states of all rollover elements by "rotating" left or right. For example, if this
     * group has three rollovers whose states are (on, on, off), after calling this method with
     * toRight=true the states will be (off, on, on). The state of the last rollover wraps around
     * to the first, so (off, off, on) -> (on, off, off).
     */
    public void cycleRollovers(boolean toRight) {
        newActiveRollovers.clear();
        for (int i = 0; i < this.rollovers.size(); i++) {
            int prevIndex = (toRight) ? ((i == 0) ? this.rollovers.size() - 1 : i - 1) :
                    ((i == this.rollovers.size() - 1) ? 0 : i + 1);
            if (this.activeRollovers.contains(this.rollovers.get(prevIndex))) {
                newActiveRollovers.add(this.rollovers.get(i));
            }
        }

        this.activeRollovers.clear();
        this.activeRollovers.addAll(newActiveRollovers);
    }

    /** Sets all rollovers to be active or inactive according to the boolean argument. */
    public void setAllRolloversActivated(boolean active) {
        activeRollovers.clear();
        if (active) {
            activeRollovers.addAll(rollovers);
        }
    }

    public boolean getIgnoreBall() {
        return this.ignoreBall;
    }

    public void setIgnoreBall(boolean ignore) {
        this.ignoreBall = ignore;
    }

    public boolean getVisible() {
        return this.isVisible;
    }

    public void setVisible(boolean visible) {
        this.isVisible = visible;
    }

    @Override public void draw(Field field, IFieldRenderer renderer) {
        if (!this.isVisible) return;

        // default color defined at the group level
        int groupColor = currentColor(DEFAULT_COLOR);

        // for each rollover, draw outlined circle for inactive or filled circle for active
        int rsize = this.rollovers.size();
        for (int i = 0; i < rsize; i++) {
            Rollover r = this.rollovers.get(i);
            // use custom rollover color if available
            int color = (r.color != null) ? r.color : groupColor;

            if (activeRollovers.contains(r)) {
                renderer.fillCircle(r.position.x, r.position.y, r.radius, color);
            }
            else {
                renderer.frameCircle(r.position.x, r.position.y, r.radius, color);
            }
        }
    }
}
