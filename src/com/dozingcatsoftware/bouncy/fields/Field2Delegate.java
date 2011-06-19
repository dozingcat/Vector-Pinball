package com.dozingcatsoftware.bouncy.fields;

import java.util.List;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.dozingcatsoftware.bouncy.BaseFieldDelegate;
import com.dozingcatsoftware.bouncy.Field;
import com.dozingcatsoftware.bouncy.elements.FieldElement;

public class Field2Delegate extends BaseFieldDelegate {
	
	static final double TAU = 2*Math.PI; // pi is wrong

	static class RotatingGroup {
		String[] elementIDs;
		double centerX, centerY;
		double radius;
		double rotationSpeed;
		double startAngle;
		double currentAngle;
		double angleIncrement;
		Vector2 tempVector = new Vector2();
		
		public RotatingGroup(String[] ids, double cx, double cy, double radius, double startAngle, double speed) {
			this.elementIDs = ids;
			this.centerX = cx;
			this.centerY = cy;
			this.radius = radius;
			this.rotationSpeed = speed;
			this.startAngle = this.currentAngle = startAngle;
			this.angleIncrement = TAU / ids.length;
		}
		
		/** Creates a RotatingGroup by computing the distance and angle to center from the first element ID in the ids array.
		 */
		public static RotatingGroup create(Field field, String[] ids, double cx, double cy, double speed) {
			FieldElement element = field.getFieldElementByID(ids[0]);
			Body body = element.getBodies().get(0);
			Vector2 position = body.getPosition();
			double radius = Math.hypot(position.x - cx, position.y - cy);
			double angle = Math.atan2(position.y - cy, position.x - cx);
			return new RotatingGroup(ids, cx, cy, radius, angle, speed);
		}
		
		public void applyRotation(Field field, double dt) {
			currentAngle += dt*rotationSpeed;
			if (currentAngle>TAU) currentAngle -= TAU;
			if (currentAngle<0) currentAngle += TAU;
			for(int i=0; i<elementIDs.length; i++) {
				double angle = currentAngle + angleIncrement*i;
				
				FieldElement element = field.getFieldElementByID(elementIDs[i]);
				Body body = element.getBodies().get(0);
				double x = centerX + radius*Math.cos(angle);
				double y = centerY + radius*Math.sin(angle);
				tempVector.set((float)x, (float)y);
				body.setTransform(tempVector, 0);
			}
		}
		
		
	}

	RotatingGroup[] rotatingGroups;
	
	RotatingGroup createRotatingGroup(Field field, String centerID, String[] ids, double speed) {
		FieldElement centerElement = field.getFieldElementByID(centerID);
		Vector2 centerPosition = centerElement.getBodies().get(0).getPosition();
		return RotatingGroup.create(field, ids, centerPosition.x, centerPosition.y, speed);
	}
	
	void setupRotatingGroups(Field field) {
		rotatingGroups = new RotatingGroup[] {
			createRotatingGroup(field, "CenterBumper1", new String[] {"RotatingBumper1A", "RotatingBumper1B", "RotatingBumper1C"}, 1.0),
		};
	}

	public void tick(Field field, long nanos) {
		if (rotatingGroups==null) {
			setupRotatingGroups(field);
		}
		
		double seconds = nanos/1e9;
		for(int i=0; i<rotatingGroups.length; i++) {
			rotatingGroups[i].applyRotation(field, seconds);
		}
	}

	/** Always return true so the rotating bumpers animate smoothly */
	@Override
	public boolean isFieldActive(Field field) {
		return true;
	}
}
