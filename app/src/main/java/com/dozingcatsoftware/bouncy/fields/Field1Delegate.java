package com.dozingcatsoftware.bouncy.fields;

import com.dozingcatsoftware.bouncy.Ball;
import com.dozingcatsoftware.bouncy.BaseFieldDelegate;
import com.dozingcatsoftware.bouncy.Field;
import com.dozingcatsoftware.bouncy.elements.DropTargetGroupElement;
import com.dozingcatsoftware.bouncy.elements.RolloverGroupElement;
import com.dozingcatsoftware.bouncy.elements.SensorElement;
import com.dozingcatsoftware.bouncy.elements.WallElement;

public class Field1Delegate extends BaseFieldDelegate {

    @Override public void allRolloversInGroupActivated(Field field, RolloverGroupElement rolloverGroup) {
        // Rollover groups increment field multiplier when all rollovers are activated.
        rolloverGroup.setAllRolloversActivated(false);
        field.getGameState().incrementScoreMultiplier();
        field.showGameMessage(((int)field.getGameState().getScoreMultiplier()) + "x Multiplier", 1500);

        // Multiball for ramp shot if extra ball rollovers all lit.
        if ("RampRollovers".equals(rolloverGroup.getElementId())) {
            RolloverGroupElement extraBallRollovers =
                    (RolloverGroupElement)field.getFieldElementById("ExtraBallRollovers");
            if (extraBallRollovers.allRolloversActive()) {
                extraBallRollovers.setAllRolloversActivated(false);
                startMultiball(field);
            }
        }
    }

    private void restoreLeftBallSaver(Field field) {
        ((WallElement)field.getFieldElementById("BallSaver-left")).setRetracted(false);
    }

    private void restoreRightBallSaver(Field field) {
        ((WallElement)field.getFieldElementById("BallSaver-right")).setRetracted(false);
    }

    private void startMultiball(final Field field) {
        field.showGameMessage("Multiball!", 2000);
        restoreLeftBallSaver(field);
        restoreRightBallSaver(field);

        Runnable launchBall = new Runnable() {
            @Override
            public void run() {
                if (field.getBalls().size() <3 ) field.launchBall();
            }
        };
        field.scheduleAction(1000, launchBall);
        field.scheduleAction(3500, launchBall);
    }

    @Override public void allDropTargetsInGroupHit(Field field, DropTargetGroupElement targetGroup) {
        // Activate ball saver for left and right groups.
        String id = targetGroup.getElementId();
        if ("DropTargetLeftSave".equals(id)) {
            restoreLeftBallSaver(field);
            field.showGameMessage("Left Save Enabled", 1500);
        }
        else if ("DropTargetRightSave".equals(id)) {
            restoreRightBallSaver(field);
            field.showGameMessage("Right Save Enabled", 1500);
        }
        // For all groups, increment extra ball rollover.
        RolloverGroupElement extraBallRollovers =
                (RolloverGroupElement)field.getFieldElementById("ExtraBallRollovers");
        if (extraBallRollovers != null && !extraBallRollovers.allRolloversActive()) {
            extraBallRollovers.activateFirstUnactivatedRollover();
            if (extraBallRollovers.allRolloversActive()) {
                field.showGameMessage("Shoot Ramp for Multiball", 1500);
            }
        }
    }

    // Support for enabling launch barrier after ball passes by it and hits sensor,
    // and disabling for new ball or new game.
    private void setLaunchBarrierEnabled(Field field, boolean enabled) {
        WallElement barrier = (WallElement)field.getFieldElementById("LaunchBarrier");
        barrier.setRetracted(!enabled);
    }

    @Override public void ballInSensorRange(Field field, SensorElement sensor, Ball ball) {
        // Enable launch barrier.
        if ("LaunchBarrierSensor".equals(sensor.getElementId())) {
            setLaunchBarrierEnabled(field, true);
        }
        else if ("LaunchBarrierRetract".equals(sensor.getElementId())) {
            setLaunchBarrierEnabled(field, false);
        }
    }

    @Override public void gameStarted(Field field) {
        setLaunchBarrierEnabled(field, false);
    }

    @Override public void ballLost(Field field) {
        setLaunchBarrierEnabled(field, false);
    }

}
