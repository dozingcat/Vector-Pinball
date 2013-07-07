package com.dozingcatsoftware.bouncy.fields;

import com.badlogic.gdx.physics.box2d.Body;
import com.dozingcatsoftware.bouncy.BaseFieldDelegate;
import com.dozingcatsoftware.bouncy.Field;
import com.dozingcatsoftware.bouncy.elements.DropTargetGroupElement;
import com.dozingcatsoftware.bouncy.elements.RolloverGroupElement;
import com.dozingcatsoftware.bouncy.elements.SensorElement;
import com.dozingcatsoftware.bouncy.elements.WallElement;
import com.dozingcatsoftware.bouncy.elements.HoleElement;


public class Field3Delegate extends BaseFieldDelegate {



  @Override
	public void allRolloversInGroupActivated(Field field, RolloverGroupElement rolloverGroup) {
		// rollover groups increment field multiplier when all rollovers are activated, also reset to inactive
		rolloverGroup.setAllRolloversActivated(false);
		field.getGameState().incrementScoreMultiplier();
		field.showGameMessage(field.getGameState().getScoreMultiplier() + "x Multiplier", 1500);
		
		// extra ball for ramp shot if extra ball rollovers all lit
		if ("RampRollovers".equals(rolloverGroup.getElementID())) {
			RolloverGroupElement extraBallRollovers = (RolloverGroupElement)field.getFieldElementByID("ExtraBallRollovers");
			if (extraBallRollovers.allRolloversActive()) {
				extraBallRollovers.setAllRolloversActivated(false);
				startMultiball(field);
			}
		}
	}
	
	
	void startMultiball(final Field field) {
		field.showGameMessage("Multiball!", 2000);
		Runnable launchBall = new Runnable() {
			public void run() {
				if (field.getBalls().size()<3) field.launchBall();
			}
		};
		field.scheduleAction(1000, launchBall);
		field.scheduleAction(3500, launchBall);
	}
	
	@Override
	public void allDropTargetsInGroupHit(Field field, DropTargetGroupElement targetGroup) {
		// activate ball saver for left and right groups
		String id = targetGroup.getElementID();
		if ("DropTargetLeftSave".equals(id)) {
			((WallElement)field.getFieldElementByID("BallSaver-left")).setRetracted(false);
			field.showGameMessage("Left Save Enabled", 1500);
		}
		else if ("DropTargetRightSave".equals(id)) {
			((WallElement)field.getFieldElementByID("BallSaver-right")).setRetracted(false);
			field.showGameMessage("Right Save Enabled", 1500);
		}
		else if ("DropTargetRightTop".equals(id)) {
			((WallElement)field.getFieldElementByID("BallSaver-right")).setRetracted(false);
			((WallElement)field.getFieldElementByID("BallSaver-left")).setRetracted(false);
			field.showGameMessage("Both Save Enabled", 1500);
			((HoleElement)field.getFieldElementByID("CenterHole")).setActivation(true);
			field.showGameMessage("Center Hole Enabled", 1500);
		}
		// for all groups, increment extra ball rollover
		RolloverGroupElement extraBallRollovers = (RolloverGroupElement)field.getFieldElementByID("ExtraBallRollovers");
		if (!extraBallRollovers.allRolloversActive()) {
			extraBallRollovers.activateFirstUnactivatedRollover();
			if (extraBallRollovers.allRolloversActive()) {
				field.showGameMessage("Shoot Ramp for Multiball", 1500);
			}
		}
	}
	
	void setLaunchBarrierEnabled(Field field, boolean enabled) {
		WallElement barrier = (WallElement)field.getFieldElementByID("LaunchBarrier");
		barrier.setRetracted(!enabled);
	}

	public void ballInSensorRange(Field field, SensorElement sensor, Body ball) {
		// enable launch barrier 
		if ("LaunchBarrierSensor".equals(sensor.getElementID())) {
			setLaunchBarrierEnabled(field, true);
		}
		else if ("LaunchBarrierRetract".equals(sensor.getElementID())) {
			setLaunchBarrierEnabled(field, false);
		}
	}
	
	@Override
	public void gameStarted(Field field) {
		setLaunchBarrierEnabled(field, false);
	}

	@Override
	public void ballLost(Field field) {
		setLaunchBarrierEnabled(field, false);
	}
}

