package com.dozingcatsoftware.bouncy.fields;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.physics.box2d.Body;
import com.dozingcatsoftware.bouncy.Ball;
import com.dozingcatsoftware.bouncy.BaseFieldDelegate;
import com.dozingcatsoftware.bouncy.Color;
import com.dozingcatsoftware.bouncy.Field;
import com.dozingcatsoftware.bouncy.elements.DropTargetGroupElement;
import com.dozingcatsoftware.bouncy.elements.FieldElement;
import com.dozingcatsoftware.bouncy.elements.RolloverGroupElement;
import com.dozingcatsoftware.bouncy.elements.SensorElement;
import com.dozingcatsoftware.bouncy.elements.WallElement;

public class Field5Delegate extends BaseFieldDelegate {

    private enum BallColor {BLUE, RED, YELLOW, GREEN};
    private enum MultiballStatus {NOT_READY, READY, STARTING, ACTIVE};

    private static final BallColor[] BALL_COLOR_VALUES = BallColor.values();

    private static final double TAU = 2*Math.PI;
    private static final boolean IGNORE_BALL_COLOR = false;

    private static final long JACKPOT_BASE_SCORE = 50000;
    private static final long RAMP_BASE_SCORE = 5000;

    private static <T> Map<BallColor, T> buildBallColorMap(
            T blueVal, T redVal, T yellowVal, T greenVal) {
        Map<BallColor, T> map = new HashMap<BallColor, T>();
        map.put(BallColor.BLUE, blueVal);
        map.put(BallColor.RED, redVal);
        map.put(BallColor.YELLOW, yellowVal);
        map.put(BallColor.GREEN, greenVal);
        return map;
    }

    private static Map<BallColor, Color> BALL_PRIMARY_COLORS = buildBallColorMap(
            Color.fromRGB(0x66, 0x88, 0xEE),
            Color.fromRGB(0xEE, 0x88, 0x88),
            Color.fromRGB(0xCC, 0xCC, 0x77),
            Color.fromRGB(0x77, 0xDD, 0x77));

    private static Map<BallColor, Color> BALL_SECONDARY_COLORS = buildBallColorMap(
            Color.fromRGB(0x44, 0x66, 0xCC),
            Color.fromRGB(0xCC, 0x66, 0x66),
            Color.fromRGB(0xAA, 0xAA, 0x55),
            Color.fromRGB(0x55, 0xBB, 0x55));

    private static Map<BallColor, Color> WALL_COLORS = buildBallColorMap(
            Color.fromRGB(0x00, 0x66, 0xFF),
            Color.fromRGB(0xCC, 0x00, 0x00),
            Color.fromRGB(0xCC, 0xCC, 0x00),
            Color.fromRGB(0x00, 0xCC, 0x00));

    private static Map<BallColor, Color> DISABLED_ROLLOVER_COLORS = buildBallColorMap(
            Color.fromRGB(0x00, 0x33, 0x66),
            Color.fromRGB(0x66, 0x00, 0x00),
            Color.fromRGB(0x66, 0x66, 0x00),
            Color.fromRGB(0x00, 0x66, 0x00));

    private Map<BallColor, List<RolloverGroupElement>> centerRolloversByColor;
    private Map<BallColor, List<WallElement>> centerLinesByColor;

    private WallElement launchBarrier;

    private Map<BallColor, Integer> rampBonuses;

    private List<WallElement> extraBallBarriers;
    private RolloverGroupElement extraBallRollover;

    // Rotating triangle.
    private double triangleRotationSpeedMultiplier = 1;
    private double triangleRotationBaseSpeed = 1.0; // radians/sec.
    private double triangleRotationAngle = 0;
    private double triangleCenterX = 10.0f;
    private double triangleCenterY = 18.86f;
    // Equilateral triangle with sides of length 2. Base is sqrt(3),
    // center is 2/3 from vertex to base.
    private double triangleRadius = Math.sqrt(3.0) * 2/3;
    private List<WallElement> triangleWalls;
    private RolloverGroupElement triangleCenterRollover;

    // For each ball, store the most recent sensor (id) that it's hit.
    // This lets us detect when a ball has fully gone over a ramp.
    Map<Ball, String> previousSensorIds = null;

    private MultiballStatus multiballStatus;
    private int multiballJackpotCount;

    private static void setBallColor(Ball ball, BallColor color) {
        ball.setPrimaryColor(BALL_PRIMARY_COLORS.get(color));
        ball.setSecondaryColor(BALL_SECONDARY_COLORS.get(color));
    }

    private BallColor getBallColor(Ball ball) {
        Color primaryColor = ball.getPrimaryColor();
        for (BallColor ballColor : BALL_COLOR_VALUES) {
            if (primaryColor.equals(BALL_PRIMARY_COLORS.get(ballColor))) {
                return ballColor;
            }
        }
        return null;
    }

    private boolean hasBallWithColor(Field field, BallColor ballColor) {
        List<Ball> balls = field.getBalls();
        for (int i=0; i<balls.size(); i++) {
            if (ballColor == getBallColor(balls.get(i))) {
                return true;
            }
        }
        return false;
    }

    private BallColor unusedBallColor(Field field) {
        for (BallColor ballColor : BALL_COLOR_VALUES) {
            if (!hasBallWithColor(field, ballColor)) {
                return ballColor;
            }
        }
        return null;
    }

    private void updateCenterRollovers(Field field) {
        for (BallColor ballColor : BALL_COLOR_VALUES) {
            boolean hasColor = hasBallWithColor(field, ballColor) || IGNORE_BALL_COLOR;
            List<RolloverGroupElement> rollovers = centerRolloversByColor.get(ballColor);
            for (int i=0; i<rollovers.size(); i++) {
                RolloverGroupElement ro = rollovers.get(i);
                ro.setIgnoreBall(!hasColor);
                if (hasColor) {
                    ro.setVisible(true);
                    ro.setNewColor(WALL_COLORS.get(ballColor));
                }
                else {
                    boolean active = ro.allRolloversActive();
                    ro.setVisible(active);
                    if (active) {
                        ro.setNewColor(DISABLED_ROLLOVER_COLORS.get(ballColor));
                    }
                }
            }
        }
    }

    private void updateCenterLines(Field field) {
        boolean allLinesVisible = true;
        for (BallColor ballColor : BALL_COLOR_VALUES) {
            List<RolloverGroupElement> rollovers = centerRolloversByColor.get(ballColor);
            List<WallElement> lines = centerLinesByColor.get(ballColor);
            for (int i=0; i<rollovers.size(); i++) {
                RolloverGroupElement r1 = rollovers.get(i);
                RolloverGroupElement r2 = rollovers.get(i==rollovers.size()-1 ? 0 : i+1);
                boolean lineVisible = r1.allRolloversActive() && r2.allRolloversActive();
                lines.get(i).setVisible(lineVisible);
                allLinesVisible = allLinesVisible && lineVisible;
            }
        }
        if (allLinesVisible) {
            if (multiballStatus == MultiballStatus.ACTIVE) {
                multiballJackpotCount += 1;
                String prefix = (multiballJackpotCount > 1) ? multiballJackpotCount+"x " : "";
                field.showGameMessage(prefix + "Jackpot!", 3000);
                field.addScore(JACKPOT_BASE_SCORE * multiballJackpotCount);
                // This will make a recursive call to updateCenterLines,
                // but only one because all the lines will be hidden
                resetCenter(field);
            }
            else if (multiballStatus == MultiballStatus.NOT_READY) {
                field.showGameMessage("Shoot the pyramid!", 3000);
                multiballStatus = MultiballStatus.READY;
            }
        }
    }

    private void checkForRamp(Field field, Ball ball, String sensorId, BallColor ballColor) {
        if (sensorId.equals(previousSensorIds.get(ball))) {
            if (!hasBallWithColor(field, ballColor)) {
                setBallColor(ball, ballColor);
                updateCenterRollovers(field);
            }
            int percentMultiplier = 100 + rampBonuses.get(ballColor);
            if (field.getBalls().size() > 1) {
                percentMultiplier *= 2;
            }
            long score = (RAMP_BASE_SCORE / 100) * percentMultiplier;
            field.addScore(score);
            field.getAudioPlayer().playRollover();
        }
    }

    private void resetCenter(Field field) {
        for (BallColor ballColor : BALL_COLOR_VALUES) {
            List<RolloverGroupElement> rollovers = centerRolloversByColor.get(ballColor);
            for (int i=0; i<rollovers.size(); i++) {
                rollovers.get(i).setRolloverActiveAtIndex(0, false);
            }
        }
        updateCenterRollovers(field);
        updateCenterLines(field);
    }

    private void incrementRampBonus(Field field, BallColor ballColor, String name) {
        rampBonuses.put(ballColor, rampBonuses.get(ballColor) + 10);
        field.showGameMessage(name + " ramp +" + rampBonuses.get(ballColor) + "%", 1500);
    }

    private void restoreLeftBallSaver(Field field) {
        ((WallElement)field.getFieldElementById("BallSaver-left")).setRetracted(false);
    }

    private void restoreRightBallSaver(Field field) {
        ((WallElement)field.getFieldElementById("BallSaver-right")).setRetracted(false);
    }

    private void startMultiball(final Field field) {
        for (int i=0; i<triangleWalls.size(); i++) {
            triangleWalls.get(i).setRetracted(false);
        }
        triangleRotationSpeedMultiplier = -2.0;
        resetCenter(field);
        field.removeBallWithoutBallLoss(field.getBalls().get(0));
        restoreLeftBallSaver(field);
        restoreRightBallSaver(field);

        final Runnable doLaunch = new Runnable() {
            @Override public void run() {
                Ball ball = field.launchBall();
                setBallColor(ball, unusedBallColor(field));
                updateCenterRollovers(field);
            }
        };

        // "Starting" state until the last ball is launched so we don't exit multiball until then.
        multiballStatus = MultiballStatus.STARTING;
        multiballJackpotCount = 0;
        field.scheduleAction(1000, doLaunch);
        field.scheduleAction(4000, doLaunch);
        field.scheduleAction(7000, doLaunch);
        field.scheduleAction(10000, new Runnable() {
            @Override public void run() {
                doLaunch.run();
                multiballStatus = MultiballStatus.ACTIVE;
            }
        });
    }

    private void endMultiball(Field field) {
        triangleRotationSpeedMultiplier = 1.0;
        triangleCenterRollover.setRolloverActiveAtIndex(0, false);
        resetCenter(field);
        multiballStatus = MultiballStatus.NOT_READY;
    }

    private void doExtraBall(Field field) {
        field.addExtraBall();
        field.showGameMessage("Extra Ball!", 3000);
    }

    private void resetExtraBallIfNeeded() {
        if (extraBallRollover.allRolloversActive()) {
            extraBallRollover.setRolloverActiveAtIndex(0, false);
            for (int i=0; i<extraBallBarriers.size(); i++) {
                extraBallBarriers.get(i).setRetracted(false);
            }
        }
    }

    @Override public void gameStarted(Field field) {
        launchBarrier = (WallElement)field.getFieldElementById("LaunchBarrier");

        triangleRotationAngle = TAU/4;
        triangleWalls = Arrays.asList(
                (WallElement)field.getFieldElementById("TriangleWall1"),
                (WallElement)field.getFieldElementById("TriangleWall2"),
                (WallElement)field.getFieldElementById("TriangleWall3"));
        triangleCenterRollover = (RolloverGroupElement)field.getFieldElementById("TriangleCenter");

        previousSensorIds = new HashMap<Ball, String>();

        centerRolloversByColor = buildBallColorMap(
                Arrays.asList(
                        (RolloverGroupElement)field.getFieldElementById("CenterRollover_Blue_1"),
                        (RolloverGroupElement)field.getFieldElementById("CenterRollover_Blue_2"),
                        (RolloverGroupElement)field.getFieldElementById("CenterRollover_Blue_3")),
                Arrays.asList(
                        (RolloverGroupElement)field.getFieldElementById("CenterRollover_Red_1"),
                        (RolloverGroupElement)field.getFieldElementById("CenterRollover_Red_2"),
                        (RolloverGroupElement)field.getFieldElementById("CenterRollover_Red_3")),
                Arrays.asList(
                        (RolloverGroupElement)field.getFieldElementById("CenterRollover_Yellow_1"),
                        (RolloverGroupElement)field.getFieldElementById("CenterRollover_Yellow_2"),
                        (RolloverGroupElement)field.getFieldElementById("CenterRollover_Yellow_3")),
                Arrays.asList(
                        (RolloverGroupElement)field.getFieldElementById("CenterRollover_Green_1"),
                        (RolloverGroupElement)field.getFieldElementById("CenterRollover_Green_2"),
                        (RolloverGroupElement)field.getFieldElementById("CenterRollover_Green_3")));

        centerLinesByColor = buildBallColorMap(
                Arrays.asList(
                        (WallElement)field.getFieldElementById("CenterLine_Blue_1_2"),
                        (WallElement)field.getFieldElementById("CenterLine_Blue_2_3"),
                        (WallElement)field.getFieldElementById("CenterLine_Blue_3_1")),
                Arrays.asList(
                        (WallElement)field.getFieldElementById("CenterLine_Red_1_2"),
                        (WallElement)field.getFieldElementById("CenterLine_Red_2_3"),
                        (WallElement)field.getFieldElementById("CenterLine_Red_3_1")),
                Arrays.asList(
                        (WallElement)field.getFieldElementById("CenterLine_Yellow_1_2"),
                        (WallElement)field.getFieldElementById("CenterLine_Yellow_2_3"),
                        (WallElement)field.getFieldElementById("CenterLine_Yellow_3_1")),
                Arrays.asList(
                        (WallElement)field.getFieldElementById("CenterLine_Green_1_2"),
                        (WallElement)field.getFieldElementById("CenterLine_Green_2_3"),
                        (WallElement)field.getFieldElementById("CenterLine_Green_3_1")));

        rampBonuses = buildBallColorMap(0, 0, 0, 0);

        extraBallRollover = (RolloverGroupElement)field.getFieldElementById("ExtraBallRollover");
        extraBallBarriers = Arrays.asList(
                (WallElement)field.getFieldElementById("ExtraBallBarrier_Blue"),
                (WallElement)field.getFieldElementById("ExtraBallBarrier_Red"),
                (WallElement)field.getFieldElementById("ExtraBallBarrier_Yellow"),
                (WallElement)field.getFieldElementById("ExtraBallBarrier_Green"));

        updateCenterRollovers(field);
        updateCenterLines(field);
        multiballStatus = MultiballStatus.NOT_READY;
    }

    @Override public void ballLost(Field field) {
        updateCenterRollovers(field);
        previousSensorIds.clear();
    }

    @Override public void ballInSensorRange(Field field, SensorElement sensor, Ball ball) {
        String sensorId = sensor.getElementId();
        if ("LaunchBarrierSensor".equals(sensorId)) {
            launchBarrier.setRetracted(false);
        }
        else if ("LaunchBarrierRetract".equals(sensorId)) {
            launchBarrier.setRetracted(true);
        }
        else if ("RampSensor_OuterLeftTop".equals(sensorId)) {
            checkForRamp(field, ball, "RampSensor_OuterRightTop", BallColor.GREEN);
        }
        else if ("RampSensor_OuterRightTop".equals(sensorId)) {
            checkForRamp(field, ball, "RampSensor_OuterLeftTop", BallColor.BLUE);
        }
        else if ("RampSensor_LeftTop".equals(sensorId)) {
            checkForRamp(field, ball, "RampSensor_LeftMiddle", BallColor.RED);
        }
        else if ("RampSensor_RightTop".equals(sensorId)) {
            checkForRamp(field, ball, "RampSensor_RightMiddle", BallColor.YELLOW);
        }
        else if ("Sensor_ExtraBallExit".equals(sensorId)) {
            resetExtraBallIfNeeded();
        }
        previousSensorIds.put(ball, sensorId);
    }

    @Override public void allDropTargetsInGroupHit(Field field, DropTargetGroupElement targetGroup) {
        String id = targetGroup.getElementId();
        if ("DropTargetLeftSave".equals(id)) {
            restoreLeftBallSaver(field);
            field.showGameMessage("Left Save Enabled", 1500);
        }
        else if ("DropTargetRightSave".equals(id)) {
            restoreRightBallSaver(field);
            field.showGameMessage("Right Save Enabled", 1500);
        }
        else if ("DropTargets_BlueRamp".equals(id)) {
            incrementRampBonus(field, BallColor.BLUE, "Blue");
        }
        else if ("DropTargets_RedRamp".equals(id)) {
            incrementRampBonus(field, BallColor.RED, "Red");
        }
        else if ("DropTargets_YellowRamp".equals(id)) {
            incrementRampBonus(field, BallColor.YELLOW, "Yellow");
        }
        else if ("DropTargets_GreenRamp".equals(id)) {
            incrementRampBonus(field, BallColor.GREEN, "Green");
        }
    }

    @Override public void allRolloversInGroupActivated(Field field, RolloverGroupElement rollovers) {
        String id = rollovers.getElementId();
        if ("FlipperRollovers".equals(id)) {
            field.incrementScoreMultiplier();
            field.showGameMessage(((int)field.getScoreMultiplier()) + "x Multiplier", 1500);
            rollovers.setAllRolloversActivated(false);
        }
        else if (rollovers == triangleCenterRollover) {
            // Should always be ready, but just a sanity check.
            if (multiballStatus == MultiballStatus.READY) {
                startMultiball(field);
            }
        }
        else if (rollovers == extraBallRollover) {
            doExtraBall(field);
        }
        updateCenterLines(field);
    }

    @Override public void processCollision(Field field, FieldElement element, Body bodyHit, Ball ball) {
        if (multiballStatus == MultiballStatus.READY) {
            if (triangleWalls.contains(element)) {
                ((WallElement)element).setRetracted(true);
            }
        }
    }

    @Override public boolean isFieldActive(Field field) {
        return true;
    }

    @Override public void tick(Field field, long nanos) {
        if (triangleWalls!=null && triangleRotationSpeedMultiplier!=0) {
            triangleRotationAngle +=
                (triangleRotationSpeedMultiplier * triangleRotationBaseSpeed) * nanos/1e9;
            if (triangleRotationAngle < 0) triangleRotationAngle += TAU;
            if (triangleRotationAngle >= TAU) triangleRotationAngle -= TAU;
            double a0 = triangleRotationAngle;
            double a1 = triangleRotationAngle + TAU/3;
            double a2 = triangleRotationAngle - TAU/3;
            float x0 = (float)(triangleCenterX + triangleRadius*Math.cos(a0));
            float y0 = (float)(triangleCenterY + triangleRadius*Math.sin(a0));
            float x1 = (float)(triangleCenterX + triangleRadius*Math.cos(a1));
            float y1 = (float)(triangleCenterY + triangleRadius*Math.sin(a1));
            float x2 = (float)(triangleCenterX + triangleRadius*Math.cos(a2));
            float y2 = (float)(triangleCenterY + triangleRadius*Math.sin(a2));
            // change wall position?
            triangleWalls.get(0).setStartAndDirection(x0, y0, x1, y1);
            triangleWalls.get(1).setStartAndDirection(x1, y1, x2, y2);
            triangleWalls.get(2).setStartAndDirection(x2, y2, x0, y0);
          }

          // Check for exiting multiball.
          if (field.getBalls().size()<=1 && multiballStatus == MultiballStatus.ACTIVE) {
            endMultiball(field);
          }
    }
}
