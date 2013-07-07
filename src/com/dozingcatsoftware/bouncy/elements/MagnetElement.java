package com.dozingcatsoftware.bouncy.elements;

import static com.dozingcatsoftware.bouncy.util.MathUtils.asFloat;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.dozingcatsoftware.bouncy.Field;
import com.dozingcatsoftware.bouncy.IFieldRenderer;

public class MagnetElement extends FieldElement {
  Body magnetBody;
	List magnetBodySet;
	
	float radius;
	float cx, cy;
		
	public void finishCreate(Map params, World world) {
		List pos = (List)params.get("position");
		this.radius = asFloat(params.get("radius"));
		this.cx = asFloat(pos.get(0));
		this.cy = asFloat(pos.get(1));
				
		magnetBody = Box2DFactory.createCircle(world, cx, cy, radius, true);
		magnetBodySet = Collections.singletonList(magnetBody);
	} 
	
	@Override 
	public List<Body> getBodies() {
		return magnetBodySet;
	}
	
	public boolean shouldCallTick() {
		// needs to call tick to decrement flash counter (but can use superclass tick() implementation)
		return true;
	}
	
	@Override
	public void handleCollision(Body ball, Body bodyHit, Field field) {
		flashForFrames(3);
		try {
			field.wait(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		field.addScore(200);	
	}

	
	@Override
	public void draw(IFieldRenderer renderer) {
		float px = magnetBody.getPosition().x;
		float py = magnetBody.getPosition().y;
		renderer.fillCircle(px, py, radius, redColorComponent(0), greenColorComponent(0), blueColorComponent(255));
	}
}

