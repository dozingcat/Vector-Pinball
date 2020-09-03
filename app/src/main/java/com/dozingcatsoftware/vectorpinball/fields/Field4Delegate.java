package com.dozingcatsoftware.vectorpinball.fields;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.physics.box2d.Body;
import com.dozingcatsoftware.vectorpinball.model.Ball;
import com.dozingcatsoftware.vectorpinball.model.BaseFieldDelegate;
import com.dozingcatsoftware.vectorpinball.model.Field;
import com.dozingcatsoftware.vectorpinball.elements.DropTargetGroupElement;
import com.dozingcatsoftware.vectorpinball.elements.FieldElement;
import com.dozingcatsoftware.vectorpinball.elements.RolloverGroupElement;
import com.dozingcatsoftware.vectorpinball.elements.SensorElement;
import com.dozingcatsoftware.vectorpinball.elements.WallElement;

public class Field4Delegate extends BaseFieldDelegate {

    WallElement launchBarrier;
    WallElement topBlocker;
    WallElement rightKicker;
    List<WallElement> multiballKickers;
    List<RolloverGroupElement> lockedBallRollovers;
    List<WallElement> lockedBallKickers;
    boolean inMultiball = false;
    boolean isMultiballStarting = false;
    int bumperMultiplierIncrease = 1;

    static List<String> COLOR_SUFFIXES = Arrays.asList(
            "Blue", "Cyan", "Green", "Yellow", "Red", "Magenta");
    Map<String, RolloverGroupElement> multiballStatusRollovers;
    int ballsLocked = 0;
    // Lighting all rollovers in multiball scores jackpot*1000 points
    // and increases multiplier by (jackpot) percent.
    int jackpot = 0;
    int baseJackpot = 25;
    int jackpotIncrease = 5;

    boolean allStatusRolloversActiveForIndex(int index) {
        for (RolloverGroupElement rollovers : multiballStatusRollovers.values()) {
            if (!rollovers.isRolloverActiveAtIndex(index)) return false;
        }
        return true;
    }

    void setAllMultiballStatusRolloversActive(boolean active) {
        for (RolloverGroupElement rollovers : multiballStatusRollovers.values()) {
            rollovers.setRolloverActiveAtIndex(0, active);
            rollovers.setRolloverActiveAtIndex(1, active);
            rollovers.setRolloverActiveAtIndex(2, active);
        }
    }

    void setIgnoreBallForMultiballStatusRollovers(boolean ignoreBall) {
        for (RolloverGroupElement rollovers : multiballStatusRollovers.values()) {
            rollovers.setIgnoreBall(ignoreBall);
        }
    }

    private void restoreLeftBallSaver(Field field) {
        ((WallElement) field.getFieldElementById("BallSaver-left")).setRetracted(false);
    }

    private void restoreRightBallSaver(Field field) {
        ((WallElement) field.getFieldElementById("BallSaver-right")).setRetracted(false);
    }

    void clearMultiballStatus() {
        setAllMultiballStatusRolloversActive(false);
        setIgnoreBallForMultiballStatusRollovers(true);
        ballsLocked = 0;
        inMultiball = false;
        isMultiballStarting = false;
    }

    void increaseExtraBumperMultiplier(Field field, int percent) {
        bumperMultiplierIncrease += percent;
        String msg = field.resolveString("bumper_multiplier_message", bumperMultiplierIncrease);
        field.showGameMessage(msg, 1500);
    }

    void startMultiball(final Field field) {
        field.showGameMessage(field.resolveString("multiball_started_message"), 3000);
        restoreLeftBallSaver(field);
        restoreRightBallSaver(field);
        lockedBallRollovers.get(2).setIgnoreBall(true);
        lockedBallRollovers.get(2).setVisible(false);
        ballsLocked = 3;
        inMultiball = true;
        isMultiballStarting = true;
        setAllMultiballStatusRolloversActive(false);
        setIgnoreBallForMultiballStatusRollovers(false);
        jackpot = baseJackpot;

        field.scheduleAction(1000, () -> {
            lockedBallKickers.get(1).setRetracted(true);
            lockedBallKickers.get(2).setRetracted(true);
            lockedBallRollovers.get(1).setIgnoreBall(true);
            lockedBallRollovers.get(1).setVisible(false);
            if (field.getBalls().size() < 3) {
                field.launchBall();
            }
        });
        field.scheduleAction(3500, () -> {
            isMultiballStarting = false;
            lockedBallRollovers.get(0).setIgnoreBall(true);
            lockedBallRollovers.get(0).setVisible(false);
            if (field.getBalls().size() < 3) {
                field.launchBall();
            }
        });
    }

    void doJackpot(Field field) {
        field.addScore(jackpot * 1000);
        field.showGameMessage(field.resolveString("jackpot_received_message"), 3000);
        // Increase multiplier by jackpot percentage. This can actually
        // cause an overflow if the player scores a whole lot of jackpots.
        double multiplier = field.getScoreMultiplier();
        long newMultiplierPercent = (long) (multiplier * (100 + jackpot));
        field.setScoreMultiplier(newMultiplierPercent / 100.0);
        jackpot += jackpotIncrease;
        setAllMultiballStatusRolloversActive(false);
    }

    @Override public void gameStarted(Field field) {
        launchBarrier = field.getFieldElementById("LaunchBarrier");
        topBlocker = field.getFieldElementById("TopRampBlocker");
        rightKicker = field.getFieldElementById("RightRampKicker");
        multiballKickers = Arrays.asList(
                field.getFieldElementById("MultiballKicker1"),
                field.getFieldElementById("MultiballKicker2"),
                field.getFieldElementById("MultiballKicker3")
        );

        // Locked ball rollovers start hidden and disabled.
        lockedBallRollovers = Arrays.asList(
                field.getFieldElementById("LockedBallRollover1"),
                field.getFieldElementById("LockedBallRollover2"),
                field.getFieldElementById("LockedBallRollover3")
        );
        for (RolloverGroupElement rollover : lockedBallRollovers) {
            rollover.setRolloverActiveAtIndex(0, false);
            rollover.setVisible(false);
            rollover.setIgnoreBall(true);
        }

        // Kickers in the ball lock zone start disabled except the bottom one.
        lockedBallKickers = Arrays.asList(
                field.getFieldElementById("LockedBallKicker1"),
                field.getFieldElementById("LockedBallKicker2"),
                field.getFieldElementById("LockedBallKicker3")
        );
        lockedBallKickers.get(1).setRetracted(true);
        lockedBallKickers.get(2).setRetracted(true);

        // Get references to multiball status rollovers and initialize them.
        multiballStatusRollovers = new HashMap<>();
        for (String suffix : COLOR_SUFFIXES) {
            multiballStatusRollovers.put(suffix,
                    field.getFieldElementById("Rollovers." + suffix));
        }
        clearMultiballStatus();

        // Remove the launch barrier.
        launchBarrier.setRetracted(true);
        rightKicker.setRetracted(true);
    }

    @Override public void ballLost(Field field) {
        launchBarrier.setRetracted(false);
        bumperMultiplierIncrease = 1;
    }


    @Override public void ballInSensorRange(Field field, SensorElement sensor, Ball ball) {
        String id = sensor.getElementId();
        // Enable launch barrier.
        if ("LaunchBarrierRetract".equals(id)) {
            launchBarrier.setRetracted(true);
        }
        else if ("TopRampSensor".equals(id)) {
            launchBarrier.setRetracted(false);
            topBlocker.setRetracted(true);
            rightKicker.setRetracted(false);
        }
        else if ("RightRampSensor".equals(id)) {
            topBlocker.setRetracted(false);
            rightKicker.setRetracted(true);
        }
        else if ("AfterRampKickerSensor".equals(id)) {
            topBlocker.setRetracted(false);
            rightKicker.setRetracted(false);
        }
    }

    @Override public void allDropTargetsInGroupHit(
            Field field, DropTargetGroupElement targetGroup, Ball ball) {
        // Activate ball saver for left and right groups.
        String id = targetGroup.getElementId();
        if ("DropTargetLeftSave".equals(id)) {
            restoreLeftBallSaver(field);
            field.showGameMessage(field.resolveString("left_save_enabled_message"), 1500);
        }
        else if ("DropTargetRightSave".equals(id)) {
            restoreRightBallSaver(field);
            field.showGameMessage(field.resolveString("right_save_enabled_message"), 1500);
        }
        else if ("DropTargetTop".equals(id)) {
            increaseExtraBumperMultiplier(field, 1);
        }
    }

    @Override
    public void processCollision(Field field, FieldElement element, Body hitBody, Ball ball) {
        String id = element.getElementId();
        if (id != null && id.startsWith("Bumper.")) {
            String suffix = id.substring(7);
            // Increment multiplier.
            long multiplier = Math.round(field.getScoreMultiplier() * 100);
            multiplier += bumperMultiplierIncrease;
            field.setScoreMultiplier(multiplier / 100.0);
            // Unlock next multiball target if all bumpers hit.
            if (ballsLocked < 3) {
                RolloverGroupElement statusRollovers = multiballStatusRollovers.get(suffix);
                statusRollovers.setRolloverActiveAtIndex(ballsLocked, true);
                if (allStatusRolloversActiveForIndex(ballsLocked)) {
                    // If rollover for ball lock isn't already active, activate and show message.
                    RolloverGroupElement lockedBallRollover = lockedBallRollovers.get(ballsLocked);
                    if (lockedBallRollover.getIgnoreBall()) {
                        lockedBallRollover.setIgnoreBall(false);
                        lockedBallRollover.setVisible(true);
                        lockedBallRollover.setRolloverActiveAtIndex(0, false);
                        String msg = (ballsLocked == 2) ?
                                field.resolveString("multiball_ready_message") :
                                field.resolveString("ball_lock_ready_message");
                        field.showGameMessage(msg, 3000);
                    }
                }
            }
        }
    }

    @Override public void allRolloversInGroupActivated(
            Field field, RolloverGroupElement rollovers, Ball ball) {
        if (rollovers == lockedBallRollovers.get(0)) {
            field.removeBallWithoutBallLoss(field.getBalls().get(0));
            lockedBallKickers.get(1).setRetracted(false);
            field.showGameMessage(field.resolveString("ball_locked_message", 1), 3000);
            ballsLocked = 1;
        }
        else if (rollovers == lockedBallRollovers.get(1)) {
            field.removeBallWithoutBallLoss(field.getBalls().get(0));
            lockedBallKickers.get(2).setRetracted(false);
            field.showGameMessage(field.resolveString("ball_locked_message", 2), 3000);
            ballsLocked = 2;
        }
        else if (rollovers == lockedBallRollovers.get(2)) {
            startMultiball(field);
        }
        else if (inMultiball && multiballStatusRollovers.containsValue(rollovers)) {
            boolean isJackpot = true;
            for (RolloverGroupElement statusRollovers : multiballStatusRollovers.values()) {
                if (!statusRollovers.allRolloversActive()) {
                    isJackpot = false;
                    break;
                }
            }
            if (isJackpot) doJackpot(field);
        }
        else if ("FlipperRollovers".equals(rollovers.getElementId())) {
            increaseExtraBumperMultiplier(field, 2);
            rollovers.setAllRolloversActivated(false);
        }
    }

    @Override public void tick(Field field, long nanos) {
        if (inMultiball && !isMultiballStarting && field.getBalls().size() <= 1) {
            clearMultiballStatus();
        }
    }
}
