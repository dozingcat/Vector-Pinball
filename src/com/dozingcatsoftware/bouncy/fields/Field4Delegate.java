package com.dozingcatsoftware.bouncy.fields;

import com.badlogic.gdx.physics.box2d.Body;
import com.dozingcatsoftware.bouncy.BaseFieldDelegate;
import com.dozingcatsoftware.bouncy.Field;
import com.dozingcatsoftware.bouncy.elements.DropTargetGroupElement;
import com.dozingcatsoftware.bouncy.elements.FieldElement;
import com.dozingcatsoftware.bouncy.elements.SensorElement;
import com.dozingcatsoftware.bouncy.elements.WallElement;

public class Field4Delegate extends BaseFieldDelegate {

    WallElement launchBarrier;
    WallElement goldTopBlocker;
    WallElement goldRightKicker;

    void startMultiball(final Field field) {
        field.showGameMessage("Multiball!", 2000);
        Runnable launchBall = new Runnable() {
            @Override
            public void run() {
                if (field.getBalls().size()<3) field.launchBall();
            }
        };
        field.scheduleAction(1000, launchBall);
        field.scheduleAction(3500, launchBall);
    }

    @Override public void gameStarted(Field field) {
        launchBarrier = (WallElement) field.getFieldElementById("LaunchBarrier");
        goldTopBlocker = (WallElement) field.getFieldElementById("TopRampGoldBlocker");
        goldRightKicker = (WallElement) field.getFieldElementById("RightRampGoldKicker");

        launchBarrier.setRetracted(true);
    }

    @Override public void ballLost(Field field) {
        launchBarrier.setRetracted(true);
    }


    @Override public void ballInSensorRange(Field field, SensorElement sensor, Body ball) {
        String id = sensor.getElementId();
        // Enable launch barrier.
        if ("LaunchBarrierRetract".equals(id)) {
            launchBarrier.setRetracted(true);
        }
        else if ("TopRampSensor".equals(id)) {
            launchBarrier.setRetracted(false);
            goldTopBlocker.setRetracted(true);
            goldRightKicker.setRetracted(false);
        }
        else if ("RightRampSensor".equals(id)) {
            goldTopBlocker.setRetracted(false);
            goldRightKicker.setRetracted(true);
        }
        else if ("BehindGoldSensor".equals(id)) {
            goldTopBlocker.setRetracted(false);
            goldRightKicker.setRetracted(false);
        }
    }

    @Override public void allDropTargetsInGroupHit(Field field, DropTargetGroupElement targetGroup) {
        // Activate ball saver for left and right groups.
        String id = targetGroup.getElementId();
        if ("DropTargetLeftSave".equals(id)) {
            ((WallElement)field.getFieldElementById("BallSaver-left")).setRetracted(false);
            field.showGameMessage("Left Save Enabled", 1500);
        }
        else if ("DropTargetRightSave".equals(id)) {
            ((WallElement)field.getFieldElementById("BallSaver-right")).setRetracted(false);
            field.showGameMessage("Right Save Enabled", 1500);
        }
    }

    @Override public void processCollision(Field field, FieldElement element, Body hitBody, Body ball) {
        // when center red bumper is hit, start multiball if all center rollovers are lit, otherwise retract left barrier
        String elementID = element.getElementId();
        if ("MultiballKicker".equals(elementID)) {
            startMultiball(field);
        }
    }

}
