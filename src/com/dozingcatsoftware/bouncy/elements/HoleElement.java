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

/** This FieldElement subclass represents a hole that ends the game when a ball hits it. If inactive it functions like a bumper
 * that applies an impulse to a ball when it hits. The impulse magnitude is controlled
 * by the "kick" parameter and if it's active or not by the "active" parameter in the configuration map . 
 */

public class HoleElement extends FieldElement{

  Body holeBody;
	List holeBodySet;
		
	float radius;
	float cx, cy;
	boolean isActive;
	float kick;
	
	public void finishCreate(Map params, World world) {
		List pos = (List)params.get("position");
		this.radius = asFloat(params.get("radius"));
		this.cx = asFloat(pos.get(0));
		this.cy = asFloat(pos.get(1));
		this.isActive = (Boolean.TRUE.equals(params.get("active")));
		this.kick = asFloat(params.get("kick"));
		
		holeBody = Box2DFactory.createCircle(world, cx, cy, radius, true);
		holeBodySet = Collections.singletonList(holeBody);
	}
	
	@Override
	public boolean shouldCallTick() {
		return true;
	}
	
	@Override
	public List<Body> getBodies() {
			return holeBodySet;
	}
	
	@Override
	public void handleCollision(Body ball, Body bodyHit, Field field) {
		if (isActive) { 			//if the element is active the ball falls in and the game ends
			field.removeBall(ball);
		}
		else{						//if the element is inactive it functions like a bumper
			Vector2 impulse = this.impulseForBall(ball);
			if (impulse!=null) {
					ball.applyLinearImpulse(impulse, ball.getWorldCenter());
					flashForFrames(3);
			}
		}
		
	}
	
	//set the element active or inactive
	public void setActivation(boolean active){ 
		isActive = active;
	}
	
	Vector2 impulseForBall(Body ball) {
		if (this.kick <= 0.01f) return null;
		// compute unit vector from center of hole to ball, and scale by kick value to get impulse
		Vector2 ballpos = ball.getWorldCenter();
		Vector2 thisPos = holeBody.getPosition();
		float ix = ballpos.x - thisPos.x;
		float iy = ballpos.y - thisPos.y;
		float mag = (float)Math.sqrt(ix*ix + iy*iy);
		float scale = this.kick / mag;
		return new Vector2(ix*scale, iy*scale);
	}

	@Override
	public void draw(IFieldRenderer renderer) {
		//when element active draws a circle and when inactive draws a filled circle to look like a bumper
		float px = holeBody.getPosition().x;
		float py = holeBody.getPosition().y;
		if(isActive){
			renderer.frameCircle(px, py, radius, redColorComponent(0), greenColorComponent(0), blueColorComponent(255));
		}else{
			renderer.fillCircle(px, py, radius, redColorComponent(0), greenColorComponent(0), blueColorComponent(255));
		}
	}
}
