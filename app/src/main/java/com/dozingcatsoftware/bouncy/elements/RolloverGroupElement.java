package com.dozingcatsoftware.bouncy.elements;

import static com.dozingcatsoftware.bouncy.util.MathUtils.asFloat;

import java.util.ArrayList;
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
        float cx, cy;
        float radius;
        float radiusSquared; // Optimization when computing whether ball is in range.
        Color color;
        long score;
        float resetDelay;
    }

    static final Color DEFAULT_COLOR = Color.fromRGB(0, 255, 0);

    boolean cycleOnFlipper;
    boolean canToggleOff;
    boolean ignoreBall;
    float defaultRadius;
    float defaultResetDelay;
    List<Rollover> rollovers = new ArrayList<Rollover>();
    List<Rollover> activeRollovers = new ArrayList<Rollover>();
    List<Rollover> rolloversHitOnPreviousTick = new ArrayList<Rollover>();
    boolean isVisible = true;

    @SuppressWarnings("unchecked")
    @Override public void finishCreateElement(Map<String, ?> params, FieldElementCollection collection) {
        this.canToggleOff = Boolean.TRUE.equals(params.get(TOGGLE_OFF_PROPERTY));
        this.cycleOnFlipper = Boolean.TRUE.equals(params.get(CYCLE_ON_FLIPPER_PROPERTY));
        this.ignoreBall = Boolean.TRUE.equals(params.get(IGNORE_BALL_PROPERTY));
        this.defaultRadius = asFloat(params.get(RADIUS_PROPERTY));
        this.defaultResetDelay = asFloat(params.get(RESET_DELAY_PROPERTY));

        List<Map<String, ?>> rolloverMaps = (List<Map<String, ?>>)params.get(ROLLOVERS_PROPERTY);
        for(Map<String, ?> rmap : rolloverMaps) {
            Rollover rollover = new Rollover();
            rollovers.add(rollover);

            List<?> pos = (List<?>)rmap.get(POSITION_PROPERTY);
            rollover.cx = asFloat(pos.get(0));
            rollover.cy = asFloat(pos.get(1));
            // radius, color, score, and reset delay can be specified for each rollover.
            // If not present use default from group.
            rollover.radius = (rmap.containsKey(RADIUS_PROPERTY)) ? asFloat(rmap.get(RADIUS_PROPERTY)) : this.defaultRadius;
            rollover.color = (rmap.containsKey(COLOR_PROPERTY)) ? Color.fromList((List<Number>)rmap.get(COLOR_PROPERTY)) : null;
            rollover.score = (rmap.containsKey(SCORE_PROPERTY)) ? ((Number)rmap.get(SCORE_PROPERTY)).longValue() : this.score;
            rollover.resetDelay = (rmap.containsKey(RESET_DELAY_PROPERTY)) ? asFloat(rmap.get(RESET_DELAY_PROPERTY)) : this.defaultResetDelay;

            rollover.radiusSquared = rollover.radius * rollover.radius;
        }
    }

    @Override public void createBodies(World world) {
        // Not needed.
    }

    @Override public List<Body> getBodies() {
        return Collections.emptyList();
    }

    // Avoid object allocation in rolloversHitByBalls.
    List<Rollover> hitRollovers = new ArrayList<Rollover>();

    /** Returns a set of all rollovers which have balls within their specified radius. */
    protected List<Rollover> rolloversHitByBalls(List<Ball> balls) {
        hitRollovers.clear();

        int rsize = this.rollovers.size();
        for(int i=0; i<rsize; i++) {
            Rollover rollover = this.rollovers.get(i);
            boolean hit = false;
            for(int j=0; j<balls.size(); j++) {
                Ball ball = balls.get(j);
                Vector2 position = ball.getPosition();
                float xdiff = position.x - rollover.cx;
                float ydiff = position.y - rollover.cy;
                float distanceSquared = xdiff*xdiff + ydiff*ydiff;
                if (distanceSquared <= rollover.radiusSquared) {
                    hit = true;
                    break;
                }
            }
            if (hit) {
                hitRollovers.add(rollover);
            }
        }
        return hitRollovers;
    }

    /** Returns true if all rollovers in the group are active. */
    public boolean allRolloversActive() {
        return activeRollovers.size() == rollovers.size();
    }

    /** Activates the first unactivated rollover in the group. Has no effect if all are active.
     */
    public void activateFirstUnactivatedRollover() {
        int rsize = this.rollovers.size();
        for(int i=0; i<rsize; i++) {
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

    @Override
    public boolean shouldCallTick() {
        return true;
    }

    @Override public void tick(Field field) {
        if (this.ignoreBall) return;

        boolean allActivePrevious = this.allRolloversActive();
        List<Rollover> hitRollovers = rolloversHitByBalls(field.getBalls());
        // only update rollovers that are hit on this tick and weren't on the previous tick
        for(final Rollover rollover : hitRollovers) {
            if (rolloversHitOnPreviousTick.contains(rollover)) continue;
            // Inactive rollover becomes active, active rollover becomes inactive if toggleOff setting is true.
            // Add score whenever the state changes.
            if (!activeRollovers.contains(rollover)) {
                activeRollovers.add(rollover);
                field.addScore(rollover.score);
                field.getAudioPlayer().playRollover();
                // Set timer to clear rollover if reset parameter is present and >0.
                if (rollover.resetDelay > 0) {
                    field.scheduleAction((long)(rollover.resetDelay*1000), new Runnable() {
                        @Override
                        public void run() {
                            activeRollovers.remove(rollover);
                        }
                    });
                }
            }
            else if (this.canToggleOff) {
                activeRollovers.remove(rollover);
                field.addScore(rollover.score);
                field.getAudioPlayer().playRollover();
            }
        }

        rolloversHitOnPreviousTick.clear();
        for(int i = 0; i < hitRollovers.size(); i++) {
            rolloversHitOnPreviousTick.add(hitRollovers.get(i));
        }

        // Notify delegate if all rollovers are now active and they weren't previously.
        if (!allActivePrevious && allRolloversActive()) {
            field.getDelegate().allRolloversInGroupActivated(field, this);
        }
    }

    @Override public void flippersActivated(Field field, List<FlipperElement> flippers) {
        if (this.cycleOnFlipper) {
            // Cycle to right if any right flipper is activated.
            boolean hasRightFlipper = false;
            for(int i=0; !hasRightFlipper && i<flippers.size(); i++) {
                hasRightFlipper = flippers.get(i).isRightFlipper();
            }
            this.cycleRollovers(hasRightFlipper);
        }
    }

    List<Rollover> newActiveRollovers = new ArrayList<Rollover>();
    /**
     * Cycles the states of all rollover elements by "rotating" left or right. For example, if this
     * group has three rollovers whose states are (on, on, off), after calling this method with
     * toRight=true the states will be (off, on, on). The state of the last rollover wraps around
     * to the first, so (off, off, on) -> (on, off, off).
     */
    public void cycleRollovers(boolean toRight) {
        newActiveRollovers.clear();
        for(int i=0; i<this.rollovers.size(); i++) {
            int prevIndex = (toRight) ? ((i==0) ? this.rollovers.size()-1 : i-1) :
                ((i==this.rollovers.size()-1) ? 0 : i+1);
            if (this.activeRollovers.contains(this.rollovers.get(prevIndex))) {
                newActiveRollovers.add(this.rollovers.get(i));
            }
        }

        this.activeRollovers.clear();
        for(int i=0; i<newActiveRollovers.size(); i++) {
            this.activeRollovers.add(newActiveRollovers.get(i));
        }
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

    @Override public void draw(IFieldRenderer renderer) {
        if (!this.isVisible) return;

        // default color defined at the group level
        Color groupColor = currentColor(DEFAULT_COLOR);

        // for each rollover, draw outlined circle for inactive or filled circle for active
        int rsize = this.rollovers.size();
        for(int i=0; i<rsize; i++) {
            Rollover rollover = this.rollovers.get(i);
            // use custom rollover color if available
            Color color = (rollover.color != null) ? rollover.color : groupColor;

            if (activeRollovers.contains(rollover)) {
                renderer.fillCircle(rollover.cx, rollover.cy, rollover.radius, color);
            }
            else {
                renderer.frameCircle(rollover.cx, rollover.cy, rollover.radius, color);
            }
        }
    }
}
