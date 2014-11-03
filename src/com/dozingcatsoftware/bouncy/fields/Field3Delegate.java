package com.dozingcatsoftware.bouncy.fields;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.badlogic.gdx.physics.box2d.Body;
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

    long bumperBonusDurationNanos;
    Color[] bumperColors;
    Color[] upperTargetGroupRolloverColors;
    Color[] lowerTargetGroupRolloverColors;

    boolean bumperBonusActive;
    long bumperBonusNanosElapsed;
    FieldElement[] bumperElements;
    int bumperBonusMultiplier = 5;

    int upperTargetGroupCompleted = 0;
    int lowerTargetGroupCompleted = 0;
    double bumperEnergy = 0;

    void buildBumperColors(Field field) {
        // TODO: Read these parameters from variables in field layout.
        bumperBonusDurationNanos = TimeUnit.SECONDS.toNanos(15);

        bumperColors = new Color[256];
        for (int i = 0; i < 256; i++) {
            bumperColors[i] = Color.fromRGB(i, 0, 255-i);
        }
        upperTargetGroupRolloverColors = new Color[] {
            Color.fromRGB(0, 0, 255),
            Color.fromRGB(0, 255, 255),
            Color.fromRGB(255, 255, 0),
            Color.fromRGB(255, 0, 0),
        };
        lowerTargetGroupRolloverColors = new Color[] {
            bumperColors[0],
            bumperColors[42],
            bumperColors[84],
            bumperColors[127],
            bumperColors[169],
            bumperColors[212],
            bumperColors[255],
        };

        List<FieldElement> bumpers = new ArrayList<FieldElement>();
        for (FieldElement element : field.getFieldElements()) {
            if (element instanceof BumperElement) {
                bumpers.add(element);
            }
        }
        bumperElements = bumpers.toArray(new FieldElement[0]);
    }

    @Override
    public void allRolloversInGroupActivated(Field field, RolloverGroupElement rolloverGroup) {
        String id = rolloverGroup.getElementID();
        if ("LeftRampRollover".equals(id) || "RightRampRollover".equals(id)) {
            startBumperBonus();
        }
        else {
            // rollover groups increment field multiplier when all rollovers are activated, also reset to inactive
            rolloverGroup.setAllRolloversActivated(false);
            field.getGameState().incrementScoreMultiplier();
            field.showGameMessage(field.getGameState().getScoreMultiplier() + "x Multiplier", 1500);
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
        else if ("LowerMultiballTargets".equals(id)) {
            if (lowerTargetGroupCompleted < lowerTargetGroupRolloverColors.length-1) {
                ++lowerTargetGroupCompleted;
                field.getFieldElementByID("LowerTargetIndicator")
                        .setNewColor(lowerTargetGroupRolloverColors[lowerTargetGroupCompleted]);
            }
        }
        else if ("UpperMultiballTargets".equals(id)) {
            if (upperTargetGroupCompleted < upperTargetGroupRolloverColors.length-1) {
                ++upperTargetGroupCompleted;
                field.getFieldElementByID("UpperTargetIndicator")
                        .setNewColor(upperTargetGroupRolloverColors[upperTargetGroupCompleted]);
            }
        }
    }

    public void tick(Field field, long nanos) {
        if (bumperBonusActive) {
            bumperBonusNanosElapsed += nanos;
            if (bumperBonusNanosElapsed >= bumperBonusDurationNanos) {
                endBumperBonus();
            }
            else {
                double fractionElapsed = ((double) bumperBonusNanosElapsed) / bumperBonusDurationNanos;
                int colorIndex = (int) Math.floor(bumperColors.length * fractionElapsed);
                Color color = bumperColors[bumperColors.length - 1 - colorIndex];
                for (FieldElement bumper : bumperElements) {
                    bumper.setNewColor(color);
                }
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
            bumper.setNewColor(bumperColors[0]);
        }
    }

    @Override public void processCollision(Field field, FieldElement element, Body hitBody, Body ball) {
        // Add bumper bonus if active.
        if (element instanceof BumperElement) {
            double energy = 1;
            if (bumperBonusActive) {
                double fractionElapsed = ((double) bumperBonusNanosElapsed) / bumperBonusDurationNanos;
                energy += fractionElapsed * bumperBonusMultiplier;
                field.addScore((long) (element.getScore() * (energy - 1)));
            }
            bumperEnergy = Math.min(bumperEnergy+energy, 255);
            int colorIndex = (int) Math.floor(bumperEnergy);
            field.getFieldElementByID("BumperIndicator")
                .setNewColor(bumperColors[colorIndex]);
        }
    }
    
    // support for enabling launch barrier after ball passes by it and hits sensor, and disabling for new ball or new game
    void setLaunchBarrierEnabled(Field field, boolean enabled) {
        WallElement barrier = (WallElement)field.getFieldElementByID("LaunchBarrier");
        barrier.setRetracted(!enabled);
    }

    @Override
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
        buildBumperColors(field);
    }

    @Override
    public void ballLost(Field field) {
        setLaunchBarrierEnabled(field, false);
        endBumperBonus();
    }

}
