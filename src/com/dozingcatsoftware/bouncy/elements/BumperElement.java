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

/** This FieldElement subclass represents a bumper that applies an impulse to a ball when it hits. The impulse magnitude is controlled
 * by the "kick" parameter in the configuration map. 
 */

public class BumperElement extends FieldElement {

	Body pegBody;
	List pegBodySet;
	
	float radius;
	float cx, cy;
	float kick;
	
	public void finishCreate(Map params, World world) {
		List pos = (List)params.get("position");
		this.radius = asFloat(params.get("radius"));
		this.cx = asFloat(pos.get(0));
		this.cy = asFloat(pos.get(1));
		this.kick = asFloat(params.get("kick"));
		
		pegBody = Box2DFactory.createCircle(world, cx, cy, radius, true);
		pegBodySet = Collections.singletonList(pegBody);
	}
	
	@Override
	public List<Body> getBodies() {
		return pegBodySet;
	}
	
	@Override
	public boolean shouldCallTick() {
		// needs to call tick to decrement flash counter (but can use superclass tick() implementation)
		return true;
	}

		
	Vector2 impulseForBall(Body ball) {
		if (this.kick <= 0.01f) return null;
		// compute unit vector from center of peg to ball, and scale by kick value to get impulse
		Vector2 ballpos = ball.getWorldCenter();
		Vector2 thisPos = pegBody.getPosition();
		float ix = ballpos.x - thisPos.x;
		float iy = ballpos.y - thisPos.y;
		float mag = (float)Math.sqrt(ix*ix + iy*iy);
		float scale = this.kick / mag;
		return new Vector2(ix*scale, iy*scale);
	}

	@Override
	public void handleCollision(Body ball, Body bodyHit, Field field) {
		Vector2 impulse = this.impulseForBall(ball);
		if (impulse!=null) {
			ball.applyLinearImpulse(impulse, ball.getWorldCenter());
			flashForFrames(3);
		}
	}

	@Override
	public void draw(IFieldRenderer renderer) {
		float px = pegBody.getPosition().x;
		float py = pegBody.getPosition().y;
		renderer.fillCircle(px, py, radius, redColorComponent(0), greenColorComponent(0), blueColorComponent(255));
	}
}
