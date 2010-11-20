package com.dozingcatsoftware.bouncy.fields;

import com.dozingcatsoftware.bouncy.BaseFieldDelegate;
import com.dozingcatsoftware.bouncy.Field;
import com.dozingcatsoftware.bouncy.elements.DropTargetGroupElement;
import com.dozingcatsoftware.bouncy.elements.RolloverGroupElement;
import com.dozingcatsoftware.bouncy.elements.WallElement;

public class Field1Delegate extends BaseFieldDelegate {
		
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
				field.showGameMessage("Extra Ball!", 2000);
				field.getGameState().addExtraBall();
				extraBallRollovers.setAllRolloversActivated(false);
			}
		}
		
	}
	
	@Override
	public void allDropTargetsInGroupHit(Field field, DropTargetGroupElement targetGroup) {
		// activate ball saver for left and right groups
		String id = targetGroup.getElementID();
		if ("DropTargetLeftSave".equals(id)) {
			((WallElement)field.getFieldElementByID("BallSaver-left")).setRetracted(field, false);
			field.showGameMessage("Left Save Enabled", 1500);
		}
		else if ("DropTargetRightSave".equals(id)) {
			((WallElement)field.getFieldElementByID("BallSaver-right")).setRetracted(field, false);
			field.showGameMessage("Right Save Enabled", 1500);
		}
		// for all groups, increment extra ball rollover
		RolloverGroupElement extraBallRollovers = (RolloverGroupElement)field.getFieldElementByID("ExtraBallRollovers");
		if (!extraBallRollovers.allRolloversActive()) {
			extraBallRollovers.activateFirstUnactivatedRollover();
			if (extraBallRollovers.allRolloversActive()) {
				field.showGameMessage("Shoot Ramp for Extra Ball", 1500);
			}
		}
	}
	
}
