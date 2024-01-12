package com.dozingcatsoftware.vectorpinball.fields;

import com.badlogic.gdx.physics.box2d.Body;
import com.dozingcatsoftware.vectorpinball.elements.FieldElement;
import com.dozingcatsoftware.vectorpinball.elements.FlipperElement;
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

    // Controls *spinner* behavior
    void startSpinner(Field field, RolloverGroupElement spinner, Ball ball, int score) {
        AtomicBoolean spinnerActive = new AtomicBoolean(true);

        final double DELAY_CONST = 1.0 / 96000;

        float magnitude = ball.getLinearVelocity().len();

        // Only activate if magnitude high enough to trigger spinner
        if (magnitude > 4) {
            // Schedule spinner activations
            int totalTime = 0;  // Tracks scheduling times
            int spinnerDelay = 0;  //

            // Calculate starting spinner speed based on magnitude, higher values spin slower
            int spinValue = (int) (32 + (-0.5 * Math.pow((magnitude - 8), 3)));
            if (spinValue < 1) {
                spinValue = 1;
            }

            while (spinnerDelay < 600) {
                // Calculate next delay cycle time
                spinnerDelay = (int) (DELAY_CONST * 500 * Math.pow(spinValue, 3));
                totalTime += spinnerDelay;

                // Schedule spins
                field.scheduleAction(totalTime, () -> {
                    field.addScore(score);
                    spinnerActive.set(!spinnerActive.get());
                    spinner.setVisible(spinnerActive.get());
                });

                spinValue += 1;
            }

            // Reset spinner at end
            field.scheduleAction(totalTime + 100, () -> {
                spinner.setRolloverActiveAtIndex(0, false);
                spinner.setVisible(false);
            });

        } else {
            spinner.setRolloverActiveAtIndex(0, false);
            spinner.setVisible(false);
        }
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

        leftOrbitRollover = field.getFieldElementById("LeftOrbitRollover");
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

        leftFlipper = field.getFieldElementById("LeftFlipper");
        rightFlipper = field.getFieldElementById("RightFlipper");
    }

    void resetGameVariables(boolean firstBall) {
        if (firstBall) {
            racksCompleted = 0;
        }
        ballsCollected = 0;
        bonusMultiplier = 1;
        leftOrbitValue = 500;
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

        leftOrbitRollover.setAllRolloversActivated(false);
        leftOrbitRollover.setVisible(false);
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
    }

    // Elements
    WallElement launchGate;
    WallElement rightGate;
    WallElement leftGate;

    RolloverGroupElement leftOrbitRollover;
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

    FlipperElement leftFlipper;
    FlipperElement rightFlipper;

    // Variables
    int ballsCollected;
    int racksCompleted;
    int bonusMultiplier;
    int leftOrbitValue;
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

                if (leftOrbitValue != 1500) {
                    leftOrbitValue += 500;
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
            case "LeftOrbitRollover":
                startSpinner(field, rolloverGroup, ball, leftOrbitValue);

                leftGate.setRetracted(true);
                field.scheduleAction(2000, () -> leftGate.setRetracted(false));
                break;
            case "LeftOutlane":
            case "RightOutlane":
                field.showGameMessage("Scratch!", 3000);
                break;
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
                field.addScore(eightBallValue);
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
