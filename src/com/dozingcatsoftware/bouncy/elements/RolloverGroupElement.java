package com.dozingcatsoftware.bouncy.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.dozingcatsoftware.bouncy.Field;
import com.dozingcatsoftware.bouncy.IFieldRenderer;
import com.dozingcatsoftware.bouncy.VPSoundpool;

import static com.dozingcatsoftware.bouncy.util.MathUtils.asFloat;

/** This class represents a collection of rollover elements, such as the rollovers in the top lanes. They are activated
 * (and optionally deactivated) when a ball passes over them. Individual rollovers in the group are represented by
 * instances of the Rollover nested class, which specify center, radius, and color. Parameters at the collection level
 * control whether the rollovers should cycle when flippers are activated, and whether rollovers can toggle from on to off.
 * @author brian
 */

public class RolloverGroupElement extends FieldElement {
	
	static class Rollover {
		float cx, cy;
		float radius;
		float radiusSquared; // optimization when computing whether ball is in range
		List<Integer> color;
		long score;
		float resetDelay;
	}
	
	boolean cycleOnFlipper;
	boolean canToggleOff;
	boolean ignoreBall;
	float defaultRadius;
	float defaultResetDelay;
	List<Rollover> rollovers = new ArrayList<Rollover>();
	List<Rollover> activeRollovers = new ArrayList<Rollover>();
    List<Rollover> rolloversHitOnPreviousTick = new ArrayList<Rollover>();
    
	@Override
	public void finishCreate(Map params, World world) {
		this.canToggleOff = Boolean.TRUE.equals(params.get("toggleOff"));
		this.cycleOnFlipper = Boolean.TRUE.equals(params.get("cycleOnFlipper"));
		this.ignoreBall = Boolean.TRUE.equals(params.get("ignoreBall"));
		this.defaultRadius = asFloat(params.get("radius"));
		this.defaultResetDelay = asFloat(params.get("reset"));
		
		List<Map> rolloverMaps = (List<Map>)params.get("rollovers");
		for(Map rmap : rolloverMaps) {
			Rollover rollover = new Rollover();
			rollovers.add(rollover);
			
			List pos = (List)rmap.get("position");
			rollover.cx = asFloat(pos.get(0));
			rollover.cy = asFloat(pos.get(1));
			// radius, color, score, and reset delay can be specified for each rollover, if not present use default from group
			rollover.radius = (rmap.containsKey("radius")) ? asFloat(rmap.get("radius")) : this.defaultRadius;
			rollover.color = (List<Integer>)rmap.get("color");
			rollover.score = (rmap.containsKey("score")) ? ((Number)rmap.get("score")).longValue() : this.score;
			rollover.resetDelay = (rmap.containsKey("reset")) ? asFloat(rmap.get("reset")) : this.defaultResetDelay;

			rollover.radiusSquared = rollover.radius * rollover.radius;
		}
	}

	@Override
	public List<Body> getBodies() {
		return Collections.EMPTY_LIST;
	}
	
	List<Rollover> hitRollovers = new ArrayList<Rollover>(); // avoid object allocation in rolloversHitByBalls
	
	/** Returns a set of all rollovers which have balls within their specified radius. */
	protected List<Rollover> rolloversHitByBalls(List<Body> balls) {
		hitRollovers.clear();
		
		int rsize = this.rollovers.size();
		for(int i=0; i<rsize; i++) {
			Rollover rollover = this.rollovers.get(i);
			boolean hit = false;
			for(int j=0; j<balls.size(); j++) {
				Body ball = balls.get(j);
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

	@Override
	public void tick(Field field) {
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
				VPSoundpool.playRollover();
				// set timer to clear rollover if reset parameter is present and >0
				if (rollover.resetDelay > 0) {
					field.scheduleAction((long)(rollover.resetDelay*1000), new Runnable() {
						public void run() {
							activeRollovers.remove(rollover);
						}
					});
				}
			}
			else if (this.canToggleOff) {
				activeRollovers.remove(rollover);
				field.addScore(rollover.score);
				VPSoundpool.playRollover();
			}
		}
		
		rolloversHitOnPreviousTick.clear();
        for(int i = 0; i < hitRollovers.size(); i++) {
            rolloversHitOnPreviousTick.add(hitRollovers.get(i));
        }
		
		// notify delegate if all rollovers are now active and they weren't previously
		if (!allActivePrevious && allRolloversActive()) {
			field.getDelegate().allRolloversInGroupActivated(field, this);
		}
	}
	
	@Override
	public void flippersActivated(Field field, List<FlipperElement> flippers) {
		if (this.cycleOnFlipper) {
			// cycle to right if any right flipper is activated
			boolean hasRightFlipper = false;
			for(int i=0; !hasRightFlipper && i<flippers.size(); i++) {
				hasRightFlipper = flippers.get(i).isRightFlipper();
			}
			this.cycleRollovers(hasRightFlipper);
		}
	}
	
	List<Rollover> newActiveRollovers = new ArrayList<Rollover>();
	/** Cycles the states of all rollover elements by "rotating" left or right. For example, if this group has three rollovers
	 * whose states are (on, on, off), after calling this method with toRight=true the states will be (off, on, on). 
	 * The state of the last rollover wraps around to the first, so (off, off, on) -> (on, off, off).
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
	
	/** Sets all rollovers to be active or inactive according to the boolean argument.
	 */
	public void setAllRolloversActivated(boolean active) {
		activeRollovers.clear();
		if (active) {
			activeRollovers.addAll(rollovers);
		}
	}

	@Override
	public void draw(IFieldRenderer renderer) {
		// default color defined at the group level
		int defaultRed = this.redColorComponent(0);
		int defaultGreen = this.greenColorComponent(255);
		int defaultBlue = this.blueColorComponent(0);
		
		// for each rollover, draw outlined circle for inactive or filled circle for active
		int rsize = this.rollovers.size();
		for(int i=0; i<rsize; i++) {
			Rollover rollover = this.rollovers.get(i);
			// use custom rollover color if available
			int red = (rollover.color!=null) ? rollover.color.get(0) : defaultRed;
			int green = (rollover.color!=null) ? rollover.color.get(1) : defaultGreen;
			int blue = (rollover.color!=null) ? rollover.color.get(2) : defaultBlue;
			
			if (activeRollovers.contains(rollover)) {
				renderer.fillCircle(rollover.cx, rollover.cy, rollover.radius, red, green, blue);
			}
			else {
				renderer.frameCircle(rollover.cx, rollover.cy, rollover.radius, red, green, blue);
			}
		}
		
	}


}
