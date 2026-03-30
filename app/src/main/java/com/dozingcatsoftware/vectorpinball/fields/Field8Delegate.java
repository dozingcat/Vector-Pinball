package com.dozingcatsoftware.vectorpinball.fields;

import com.badlogic.gdx.physics.box2d.Body;
import com.dozingcatsoftware.vectorpinball.elements.FieldElement;
import com.dozingcatsoftware.vectorpinball.elements.FlipperElement;
import com.dozingcatsoftware.vectorpinball.elements.SpinnerElement;
import com.dozingcatsoftware.vectorpinball.model.Ball;
import com.dozingcatsoftware.vectorpinball.model.BaseFieldDelegate;
import com.dozingcatsoftware.vectorpinball.model.Field;
import com.dozingcatsoftware.vectorpinball.elements.RolloverGroupElement;
import com.dozingcatsoftware.vectorpinball.elements.WallElement;
import com.dozingcatsoftware.vectorpinball.elements.SensorElement;
import com.dozingcatsoftware.vectorpinball.elements.DropTargetGroupElement;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicBoolean;

public class Field8Delegate extends BaseFieldDelegate {

    // Given a number, returns a string containing that number with comma separation
    String insertCommas(long number) {
        DecimalFormat df = new DecimalFormat("#,###");
        return df.format(number);
    }

    // Activate on right bank hit
    // Sets bank and bonus inserts
    void handleRightBankTargets() {
        for (int i = 0; i < 7; i++) {
            if(!rightBankTargets.getBodies().get(i).isActive()) {
                rightBankInserts.setRolloverActiveAtIndex(6 - i, false);
                bonusInserts.setRolloverActiveAtIndex(6 - i, true);
            }
        }
    }

    // Increases bonus multiplier up to 5
    void increaseMultiplier() {
        if (bonusMultiplier != 5) {
            bonusMultiplier += 1;
        }
    }

    // Counts balls collected on current ball based off of lit bonus inserts
    int countBallsCollected() {
        int numActive = 0;
        int n = bonusInserts.numberOfRollovers();
        for (int i = 0; i < n; i++) {
            if (bonusInserts.isRolloverActiveAtIndex(i)) {
                numActive += 1;
            }
        }
        return numActive;
    }

    // Collects the entire rack on skill shot
    void collectRack(Field field) {
        bonusInserts.setAllRolloversActivated(true);
        rightBankInserts.setAllRolloversActivated(false);

        int targetsDown = 0;
        for (int i = 0; i < 7; i++) {
            if (rightBankTargets.getBodies().get(i).isActive()) {
                targetsDown += 1;
                rightBankTargets.getBodies().get(i).setActive(false);
            }
        }
        field.addScore((long) 3000 * targetsDown);
        field.addScore(eightBallValue);
        skillShotActive = false;
        bonusCollectLit = true;
        rightBankCompleted = true;
        bonusCollectSaucer.setVisible(true);
        racksCompleted += 1;
    }

    void rightBankLightShow(Field field) {
        int timer = 500;
        for (int i = 0; i < 7; i++) {
            final int index = i;
            field.scheduleAction(timer + (100 * i), () -> rightBankInserts.setRolloverActiveAtIndex(index, true));
        }
        field.scheduleAction(1200, () -> rightBankTargets.makeAllTargetsVisible());
    }

    void initFieldElements(Field field) {
        launchGate = field.getFieldElementById("LaunchGate");
        rightGate = field.getFieldElementById("RightGate");
        leftGate = field.getFieldElementById("LeftGate");

        laneRollovers = field.getFieldElementById("LaneRollovers");
        bonusCollectSaucer = field.getFieldElementById("BonusCollectSaucer");
        leftOutlane = field.getFieldElementById("LeftOutlane");
        rightOutlane = field.getFieldElementById("RightOutlane");

        rightBankInserts = field.getFieldElementById("RightBankInserts");
        eightBallInsert = field.getFieldElementById("EightBallInsert");
        bonusInserts = field.getFieldElementById("BonusInserts");
        bonusMultiplierInserts = field.getFieldElementById("BonusMultiplierInserts");
        leftOrbitValueInserts = field.getFieldElementById("LeftOrbitValueInserts");
        eightBallValueInserts = field.getFieldElementById("EightBallValueInserts");

        rightBankTargets = field.getFieldElementById("RightBankTargets");
        eightBallTarget = field.getFieldElementById("EightBallTarget");
        inlineTargets = field.getFieldElementById("InlineTargets");

        leftOrbitSpinner = field.getFieldElementById("LeftOrbitSpinner");

        leftFlipper = field.getFieldElementById("LeftFlipper");
        rightFlipper = field.getFieldElementById("RightFlipper");
    }

    void resetGameVariables(boolean firstBall) {
        if (firstBall) {
            racksCompleted = 0;
        }
        ballsCollected = 0;
        bonusMultiplier = 1;
        eightBallValue = 20000;

        rightBankCompleted = false;
        bonusCollectLit = false;
        skillShotActive = false;
    }

    void resetElements(Field field, boolean firstBall) {
        if (rightBankCompleted || firstBall) {
            bonusInserts.setAllRolloversActivated(false);
            rightBankCompleted = false;

            rightBankLightShow(field);
        }

        laneRollovers.setAllRolloversActivated(false);
        bonusCollectSaucer.setAllRolloversActivated(false);
        bonusCollectSaucer.setVisible(false);
        leftOutlane.setVisible(false);
        rightOutlane.setVisible(false);

        eightBallInsert.setAllRolloversActivated(false);
        bonusMultiplierInserts.setAllRolloversActivated(false);
        leftOrbitValueInserts.setAllRolloversActivated(false);
        leftOrbitValueInserts.activateFirstUnactivatedRollover();
        eightBallValueInserts.setAllRolloversActivated(false);
        eightBallValueInserts.activateFirstUnactivatedRollover();

        eightBallTarget.makeAllTargetsVisible();
        inlineTargets.makeAllTargetsVisible();

        leftOrbitSpinner.setScore(500);
    }

    // Elements
    WallElement launchGate;
    WallElement rightGate;
    WallElement leftGate;

    RolloverGroupElement laneRollovers;
    RolloverGroupElement bonusCollectSaucer;
    RolloverGroupElement leftOutlane;
    RolloverGroupElement rightOutlane;

    RolloverGroupElement rightBankInserts;
    RolloverGroupElement eightBallInsert;
    RolloverGroupElement bonusInserts;
    RolloverGroupElement bonusMultiplierInserts;
    RolloverGroupElement leftOrbitValueInserts;
    RolloverGroupElement eightBallValueInserts;

    DropTargetGroupElement rightBankTargets;
    DropTargetGroupElement eightBallTarget;
    DropTargetGroupElement inlineTargets;

    SpinnerElement leftOrbitSpinner;

    FlipperElement leftFlipper;
    FlipperElement rightFlipper;

    // Variables
    int ballsCollected;
    int racksCompleted;
    int bonusMultiplier;
    int eightBallValue;

    boolean rightBankCompleted;
    boolean bonusCollectLit;
    boolean skillShotActive;

    @Override public void gameStarted(Field field) {
        initFieldElements(field);
        resetElements(field, true);
        resetGameVariables(true);
    }

    @Override public boolean isFieldActive(Field field) {
        return true;
    }

    @Override public void ballLost(Field field) {
        ballsCollected = countBallsCollected();
        int bonusTotal = ((racksCompleted * 56000) + (ballsCollected * 7000)) * bonusMultiplier;
        field.addScore(bonusTotal);
        field.showGameMessage("Bonus: " + insertCommas(bonusTotal), 3000);

        resetElements(field, false);
        resetGameVariables(false);
    }

    @Override public void processCollision(
            Field field, FieldElement element, Body hitBody, Ball ball
    ) {
        String id = element.getElementId();

        // Process right drop
        if (id.equals("RightBankTargets")) {
            handleRightBankTargets();
        }

        // Process multiplier targets
        else if (id.equals("InlineTargets")) {
            increaseMultiplier();
            bonusMultiplierInserts.activateFirstUnactivatedRollover();
        }
    }

    @Override public void allRolloversInGroupActivated(
            Field field, RolloverGroupElement rolloverGroup, Ball ball
    ) {
        String id = rolloverGroup.getElementId();

        switch (id) {
            case "LaneRollovers":
                field.showGameMessage("Lanes Completed", 3000);
                laneRollovers.setAllRolloversActivated(false);

                if (leftOrbitSpinner.getScore() < 1500) {
                    leftOrbitSpinner.setScore(leftOrbitSpinner.getScore() + 500);
                    leftOrbitValueInserts.activateFirstUnactivatedRollover();

                    eightBallValue += 20000;
                    eightBallValueInserts.activateFirstUnactivatedRollover();
                }
                break;
            case "BonusCollectSaucer":
                bonusCollectLit = false;

                ballsCollected = countBallsCollected();
                int bonusTotal = ((racksCompleted * 56000) + (ballsCollected * 7000)) * bonusMultiplier;
                field.addScore(bonusTotal);
                field.showGameMessage("Bonus Collected: " + insertCommas(bonusTotal), 3000);
                break;
            case "LeftOutlane":
            case "RightOutlane":
                field.showGameMessage("Scratch!", 3000);
                break;
        }
    }

    @Override public void spinnerActivated(Field field, SpinnerElement spinner, Ball ball) {
        if ("LeftOrbitSpinner".equals(spinner.getElementId())) {
            leftGate.setRetracted(true);
            field.scheduleAction(2000, () -> leftGate.setRetracted(false));
        }
    }

    @Override public void allDropTargetsInGroupHit(
            Field field, DropTargetGroupElement targetGroup, Ball ball
    ) {
        String id = targetGroup.getElementId();

        if (id.equals("EightBallTarget")) {
            if(rightBankCompleted) {
                collectRack(field);
                field.showGameMessage("Bonus Collect Lit", 3000);

            } else if (skillShotActive) {
                collectRack(field);
                field.showGameMessage("Breakshot!", 3000);
            }

            else {
                eightBallTarget.makeAllTargetsVisible();
                field.addScoreWithAnimation(eightBallValue, ball.getPosition());
                field.showGameMessage("Corner Pocket: " + insertCommas(eightBallValue), 3000);
            }
        }

        else if (id.equals("RightBankTargets") && !rightBankCompleted) {
            rightBankCompleted = true;
            eightBallInsert.setRolloverActiveAtIndex(0, true);
            field.showGameMessage("Shoot the Eight Ball!", 3000);
        }
    }

    @Override public void ballInSensorRange(final Field field, SensorElement sensor, Ball ball) {
        String id = sensor.getElementId();

        switch (id) {
            case "LaunchGateRetract":
                launchGate.setRetracted(true);
                field.scheduleAction(1000, () -> launchGate.setRetracted(false));

                if (leftFlipper.isFlipperEngaged()) {
                    skillShotActive = true;
                    leftGate.setRetracted(true);
                    field.scheduleAction(2000, () -> leftGate.setRetracted(false));
                }
                break;
            case "RightGateRetract":
                rightGate.setRetracted(true);
                field.scheduleAction(1000, () -> rightGate.setRetracted(false));
                break;
            case "EndSkillShot":
                skillShotActive = false;
                break;
        }
    }
}
