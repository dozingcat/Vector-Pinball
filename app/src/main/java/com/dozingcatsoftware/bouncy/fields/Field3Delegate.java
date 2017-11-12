package com.dozingcatsoftware.bouncy.fields;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.badlogic.gdx.physics.box2d.Body;
import com.dozingcatsoftware.bouncy.Ball;
import com.dozingcatsoftware.bouncy.BaseFieldDelegate;
import com.dozingcatsoftware.bouncy.Color;
import com.dozingcatsoftware.bouncy.Field;
import com.dozingcatsoftware.bouncy.elements.BumperElement;
import com.dozingcatsoftware.bouncy.elements.DropTargetGroupElement;
import com.dozingcatsoftware.bouncy.elements.FieldElement;
import com.dozingcatsoftware.bouncy.elements.RolloverGroupElement;
import com.dozingcatsoftware.bouncy.elements.SensorElement;
import com.dozingcatsoftware.bouncy.elements.WallElement;

public class Field3Delegate extends BaseFieldDelegate {

    static Color[] TEMPERATURE_COLORS = {
        // Blue to cyan in steps of 16.
        Color.fromRGB(0, 0, 255),
        Color.fromRGB(0, 16, 255),
        Color.fromRGB(0, 32, 255),
        Color.fromRGB(0, 48, 255),
        Color.fromRGB(0, 64, 255),
        Color.fromRGB(0, 80, 255),
        Color.fromRGB(0, 96, 255),
        Color.fromRGB(0, 112, 255),
        Color.fromRGB(0, 128, 255),
        Color.fromRGB(0, 144, 255),
        Color.fromRGB(0, 160, 255),
        Color.fromRGB(0, 176, 255),
        Color.fromRGB(0, 192, 255),
        Color.fromRGB(0, 208, 255),
        Color.fromRGB(0, 224, 255),
        Color.fromRGB(0, 240, 255),
        // Cyan to green in steps of 32.
        Color.fromRGB(0, 255, 240),
        Color.fromRGB(0, 255, 208),
        Color.fromRGB(0, 255, 176),
        Color.fromRGB(0, 255, 144),
        Color.fromRGB(0, 255, 112),
        Color.fromRGB(0, 255, 80),
        Color.fromRGB(0, 255, 48),
        Color.fromRGB(0, 255, 16),
        // Green to yellow in steps of 32.
        Color.fromRGB(16, 255, 0),
        Color.fromRGB(48, 255, 0),
        Color.fromRGB(80, 255, 0),
        Color.fromRGB(112, 255, 0),
        Color.fromRGB(144, 255, 0),
        Color.fromRGB(176, 255, 0),
        Color.fromRGB(208, 255, 0),
        Color.fromRGB(240, 255, 0),
        // Yellow to red in steps of 12.
        Color.fromRGB(255, 240, 0),
        Color.fromRGB(255, 228, 0),
        Color.fromRGB(255, 216, 0),
        Color.fromRGB(255, 204, 0),
        Color.fromRGB(255, 192, 0),
        Color.fromRGB(255, 180, 0),
        Color.fromRGB(255, 168, 0),
        Color.fromRGB(255, 156, 0),
        Color.fromRGB(255, 144, 0),
        Color.fromRGB(255, 132, 0),
        Color.fromRGB(255, 120, 0),
        Color.fromRGB(255, 108, 0),
        Color.fromRGB(255, 96, 0),
        Color.fromRGB(255, 84, 0),
        Color.fromRGB(255, 72, 0),
        Color.fromRGB(255, 60, 0),
        Color.fromRGB(255, 48, 0),
        Color.fromRGB(255, 36, 0),
        Color.fromRGB(255, 24, 0),
        Color.fromRGB(255, 12, 0),
        Color.fromRGB(255, 0, 0),
    };

    static Color colorForTemperatureRatio(double ratio) {
        int len = TEMPERATURE_COLORS.length;
        if (ratio <= 0) return TEMPERATURE_COLORS[0];
        if (ratio >= 1) return TEMPERATURE_COLORS[len-1];
        return TEMPERATURE_COLORS[(int) Math.round((len-1) * ratio)];
    }

    static enum MultiballStatus {PENDING, ACTIVE, INACTIVE};

    static Random RAND = new Random();

    long baseBumperBonusDurationNanos;
    long bumperBonusDurationNanos;

    boolean bumperBonusActive;
    long bumperBonusNanosElapsed;
    FieldElement[] bumperElements;

    int baseBumperBonusMultiplier;
    int bumperBonusMultiplier;

    int bumperBonusMultiplierIncrement = 1;
    long bumperBonusDurationIncrement = TimeUnit.SECONDS.toNanos(1);

    int upperTargetGroupCompleted = 0;
    int lowerTargetGroupCompleted = 0;
    double bumperEnergy = 0;

    double maxBumperEnergy = 0;
    int maxUpperTargetGroupCompleted = 0;
    int maxLowerTargetGroupCompleted = 0;

    MultiballStatus multiballStatus = MultiballStatus.INACTIVE;
    double[] multiballFlashValues = new double[3];
    double[] multiballFlashIncrements = new double[3];

    void resetState(Field field) {
        // TODO: Read these parameters from variables in field layout.
        maxBumperEnergy = 200;
        maxUpperTargetGroupCompleted = 2;
        maxLowerTargetGroupCompleted = 5;

        baseBumperBonusDurationNanos = TimeUnit.SECONDS.toNanos(15);
        baseBumperBonusMultiplier = 5;
        resetBumperBonuses(field);

        List<FieldElement> bumpers = new ArrayList<FieldElement>();
        for (FieldElement element : field.getFieldElements()) {
            if (element instanceof BumperElement) {
                bumpers.add(element);
            }
        }
        bumperElements = bumpers.toArray(new FieldElement[0]);
    }

    void resetBumperBonuses(Field field) {
        bumperBonusDurationNanos = baseBumperBonusDurationNanos;
        bumperBonusMultiplier = baseBumperBonusMultiplier;
        bumperEnergy = 0;
        upperTargetGroupCompleted = 0;
        lowerTargetGroupCompleted = 0;

        field.getFieldElementById("UpperTargetIndicator").setNewColor(null);
        field.getFieldElementById("LowerTargetIndicator").setNewColor(null);
        field.getFieldElementById("BumperIndicator").setNewColor(null);
        setMultiballRolloverActive(field, false);
    }

    private void restoreLeftBallSaver(Field field) {
        ((WallElement)field.getFieldElementById("BallSaver-left")).setRetracted(false);
    }

    private void restoreRightBallSaver(Field field) {
        ((WallElement)field.getFieldElementById("BallSaver-right")).setRetracted(false);
    }

    @Override
    public void allRolloversInGroupActivated(Field field, RolloverGroupElement rolloverGroup) {
        String id = rolloverGroup.getElementId();
        if ("LeftRampRollover".equals(id) || "RightRampRollover".equals(id)) {
            startBumperBonus();
            if (multiballStatus==MultiballStatus.INACTIVE && isMultiballRolloverActive(field)) {
                startMultiball(field);
            }
            // Double score if already in multiball.
            if (multiballStatus==MultiballStatus.ACTIVE) {
                field.addScore(rolloverGroup.getScore());
            }
        }
        else {
            // Rollover groups increment field multiplier when all rollovers are activated.
            rolloverGroup.setAllRolloversActivated(false);
            field.getGameState().incrementScoreMultiplier();
            field.showGameMessage(((int)field.getGameState().getScoreMultiplier()) + "x Multiplier", 1500);
        }
    }

    @Override
    public void allDropTargetsInGroupHit(Field field, DropTargetGroupElement targetGroup) {
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
        else if ("LowerMultiballTargets".equals(id)) {
            // Increase bumper bonus duration.
            if (lowerTargetGroupCompleted < maxLowerTargetGroupCompleted) {
                bumperBonusDurationNanos += bumperBonusDurationIncrement;
                ++lowerTargetGroupCompleted;
                double ratio = ((double) lowerTargetGroupCompleted) / maxLowerTargetGroupCompleted;
                field.getFieldElementById("LowerTargetIndicator").setNewColor(colorForTemperatureRatio(ratio));
                checkForEnableMultiball(field);
            }
        }
        else if ("UpperMultiballTargets".equals(id)) {
            // Increase bumper bonus multiplier.
            if (upperTargetGroupCompleted < maxUpperTargetGroupCompleted) {
                bumperBonusMultiplier += bumperBonusMultiplierIncrement;
                ++upperTargetGroupCompleted;
                double ratio = ((double)upperTargetGroupCompleted) / maxUpperTargetGroupCompleted;
                field.getFieldElementById("UpperTargetIndicator").setNewColor(colorForTemperatureRatio(ratio));
                checkForEnableMultiball(field);
            }
        }
    }

    @Override
    public void tick(Field field, long nanos) {
        if (bumperBonusActive) {
            bumperBonusNanosElapsed += nanos;
            if (bumperBonusNanosElapsed >= bumperBonusDurationNanos) {
                endBumperBonus();
            }
            else {
                double fractionElapsed = ((double) bumperBonusNanosElapsed) / bumperBonusDurationNanos;
                Color color = colorForTemperatureRatio(1 - fractionElapsed);
                for (FieldElement bumper : bumperElements) {
                    bumper.setNewColor(color);
                }
            }
        }
        if (multiballStatus == MultiballStatus.ACTIVE) {
            if (field.getBalls().size() <= 1) {
                // Multiball ended; reset counters.
                resetBumperBonuses(field);
                multiballStatus = MultiballStatus.INACTIVE;
            }
            else {
                tickMultiballFlashers(field, nanos);
            }
        }
    }

    void startBumperBonus() {
        bumperBonusActive = true;
        bumperBonusNanosElapsed = 0;
    }

    void endBumperBonus() {
        bumperBonusActive = false;
        for (FieldElement bumper : bumperElements) {
            bumper.setNewColor(null);
        }
    }

    @Override public void processCollision(Field field, FieldElement element, Body hitBody, Ball ball) {
        // Add bumper bonus if active.
        if (element instanceof BumperElement) {
            double extraEnergy = 0;
            if (bumperBonusActive) {
                double fractionRemaining = 1 - (((double) bumperBonusNanosElapsed) / bumperBonusDurationNanos);
                extraEnergy = fractionRemaining * bumperBonusMultiplier;
                // Round score to nearest multiple of 10.
                double bonusScore = element.getScore() * extraEnergy;
                field.addScore(10 * (Math.round(bonusScore / 10)));
            }
            bumperEnergy = Math.min(bumperEnergy + 1 + extraEnergy, maxBumperEnergy);
            double ratio = (bumperEnergy) / maxBumperEnergy;
            field.getFieldElementById("BumperIndicator").setNewColor(colorForTemperatureRatio(ratio));
            checkForEnableMultiball(field);
        }
    }

    // Support for enabling launch barrier after ball passes by it and hits sensor,
    // and disabling for new ball or new game.
    void setLaunchBarrierEnabled(Field field, boolean enabled) {
        WallElement barrier = (WallElement)field.getFieldElementById("LaunchBarrier");
        barrier.setRetracted(!enabled);
    }

    @Override
    public void ballInSensorRange(Field field, SensorElement sensor, Ball ball) {
        // enable launch barrier
        if ("LaunchBarrierSensor".equals(sensor.getElementId())) {
            setLaunchBarrierEnabled(field, true);
        }
        else if ("LaunchBarrierRetract".equals(sensor.getElementId())) {
            setLaunchBarrierEnabled(field, false);
        }
    }

    @Override
    public void gameStarted(Field field) {
        setLaunchBarrierEnabled(field, false);
        resetState(field);
        multiballStatus = MultiballStatus.INACTIVE;
    }

    @Override
    public void ballLost(Field field) {
        setLaunchBarrierEnabled(field, false);
        endBumperBonus();
        // This should normally be handled by tick().
        if (multiballStatus == MultiballStatus.ACTIVE) {
            resetBumperBonuses(field);
            multiballStatus = MultiballStatus.INACTIVE;
        }
    }

    void checkForEnableMultiball(Field field) {
        if (bumperEnergy >= maxBumperEnergy &&
                upperTargetGroupCompleted >= maxUpperTargetGroupCompleted &&
                lowerTargetGroupCompleted >= maxLowerTargetGroupCompleted) {
            setMultiballRolloverActive(field, true);
        }
    }

    boolean isMultiballRolloverActive(Field field) {
        return multiballStatus==MultiballStatus.INACTIVE &&
                ((RolloverGroupElement) field.getFieldElementById("BumperIndicator")).isRolloverActiveAtIndex(0);
    }
    void setMultiballRolloverActive(Field field, boolean ready) {
        ((RolloverGroupElement) field.getFieldElementById("BumperIndicator")).setAllRolloversActivated(ready);
    }

    void startMultiball(final Field field) {
        field.showGameMessage("Multiball!", 2000);
        multiballStatus = MultiballStatus.PENDING;
        restoreLeftBallSaver(field);
        restoreRightBallSaver(field);

        Runnable launchBall = new Runnable() {
            @Override
            public void run() {
                if (field.getBalls().size()<3) field.launchBall();
                if (multiballStatus != MultiballStatus.ACTIVE) {
                    multiballStatus = MultiballStatus.ACTIVE;
                    initializeMultiballFlashers();
                }
            }
        };
        field.scheduleAction(1000, launchBall);
        field.scheduleAction(3500, launchBall);

        // Increase bonuses past normal maximum.
        bumperBonusMultiplier += bumperBonusMultiplierIncrement;
        bumperBonusDurationNanos += bumperBonusDurationIncrement;
    }

    void initializeMultiballFlashers() {
        // Between a 2 and 5 second cycle. (0.2 to 0.5 delta per second).
        for (int i = 0; i < 3; i++) {
            multiballFlashValues[i] = 0;
            multiballFlashIncrements[i] = 0.2 + 0.3*RAND.nextDouble();
        }
    }

    Color colorForMultiballFlasher(int index) {
        return colorForTemperatureRatio(Math.abs(multiballFlashValues[index] - 1));
    }

    void tickMultiballFlashers(Field field, long nanos) {
        double seconds = nanos / 1e9;
        for (int i = 0; i < 3; i++) {
            double nextVal = multiballFlashValues[i] + (seconds * multiballFlashIncrements[i]);
            if (nextVal > 2) nextVal -= 2;
            multiballFlashValues[i] = nextVal;
        }
        field.getFieldElementById("UpperTargetIndicator").setNewColor(colorForMultiballFlasher(0));
        field.getFieldElementById("LowerTargetIndicator").setNewColor(colorForMultiballFlasher(1));
        field.getFieldElementById("BumperIndicator").setNewColor(colorForMultiballFlasher(2));
    }
}
