package com.dozingcatsoftware.vectorpinball.fields;

import com.badlogic.gdx.math.Vector2;
import com.dozingcatsoftware.vectorpinball.model.Ball;
import com.dozingcatsoftware.vectorpinball.model.BaseFieldDelegate;
import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.model.Field;
import com.dozingcatsoftware.vectorpinball.elements.DropTargetGroupElement;
import com.dozingcatsoftware.vectorpinball.elements.RolloverGroupElement;
import com.dozingcatsoftware.vectorpinball.elements.SensorElement;
import com.dozingcatsoftware.vectorpinball.elements.WallElement;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Field6Delegate extends BaseFieldDelegate {
    private static final double TAU = 2 * Math.PI;

    private enum MultiballStatus {INACTIVE, STARTING, ACTIVE}
    private enum PlanetStatus {OFF, IN_PROGRESS, ON}

    private final double sunGravityForce = 8.0;
    private final double planetGravityForce = 15.0; // multiplied by radius^3 for each planet.
    // The sun and planets are this far "below" the ball.
    private final double gravityDepthSquared = 2.0 * 2.0;
    // Distance from sun beyond which gravity is not applied.
    private final double gravityRangeSquared = 8.0 * 8.0;

    private final long rampBonusDurationNanos = 12_000_000_000L;
    private long rampBonusNanosRemaining = 0;
    private int rampBonusMultiplier = 1;

    private final long rampScore = 2500;
    private final long planet1TargetsScore = 5000;
    private final long planet2RolloversScore = 5000;
    private final long planetActivatedScore = 5000;
    private final long multiballJackpotScore = 100000;

    private MultiballStatus multiballStatus = MultiballStatus.INACTIVE;
    private int multiballJackpotMultiplier = 1;

    private static final int BLACK = Color.fromRGB(0, 0, 0);
    private final List<Integer> planetColors = Arrays.asList(
            Color.fromRGB(0xFF, 0x99, 0x00),
            Color.fromRGB(0x00, 0x99, 0xFF),
            Color.fromRGB(0xAA, 0x00, 0x00),
            Color.fromRGB(0x00, 0xAA, 0x66),
            Color.fromRGB(0xAA, 0x22, 0xCC));
    private final List<Integer> ballColors = Arrays.asList(
            Color.fromRGB(0xFF, 0xBB, 0x44),
            Color.fromRGB(0x66, 0xBB, 0xFF),
            Color.fromRGB(0xDD, 0x44, 0x44),
            Color.fromRGB(0x77, 0xDD, 0xAA),
            Color.fromRGB(0xCC, 0x88, 0xEE));
    private final List<Integer> ballSecondaryColors = Arrays.asList(
            Color.fromRGB(0xDD, 0x99, 0x22),
            Color.fromRGB(0x44, 0x99, 0xDD),
            Color.fromRGB(0xBB, 0x22, 0x22),
            Color.fromRGB(0x55, 0xBB, 0x88),
            Color.fromRGB(0xAA, 0x66, 0xCC));

    private static final class Planet {
        RolloverGroupElement element;
        int color;
        double radius;
        double angle;
        double angularVelocity;
        PlanetStatus status;
    }

    private Planet[] planets;
    private double inProgressPlanetPhase = 0;

    private WallElement launchBarrier;
    private RolloverGroupElement orbits;
    private RolloverGroupElement sun;

    private void startMultiball(final Field field) {
        ((WallElement) field.getFieldElementById("BallSaver-left")).setRetracted(false);
        ((WallElement) field.getFieldElementById("BallSaver-right")).setRetracted(false);
        for (Planet p : planets) {
            p.status = PlanetStatus.OFF;
        }
        // "Starting" state until the last ball is launched so we don't exit multiball until then.
        multiballStatus = MultiballStatus.STARTING;
        multiballJackpotMultiplier = 1;
        field.showGameMessage(field.resolveString("multiball_started_message"), 4000);
        field.scheduleAction(1000, field::launchBall);
        field.scheduleAction(4000, () -> {
            field.launchBall();
            multiballStatus = MultiballStatus.ACTIVE;
        });
    }

    private void endMultiball(Field field) {
        multiballStatus = MultiballStatus.INACTIVE;
        for (Planet p : planets) {
            p.status = PlanetStatus.OFF;
        }
    }

    private boolean allPlanetsOn() {
        for (Planet p : planets) {
            if (p.status != PlanetStatus.ON) {
                return false;
            }
        }
        return true;
    }

    private void activatePlanetIfMatch(Field field, Ball ball, int planetIndex) {
        if (planets[planetIndex].status == PlanetStatus.ON) {
            return;
        }
        int ballColorIndex = ballColors.indexOf(ball.getPrimaryColor());
        if (ballColorIndex == planetIndex) {
            planets[planetIndex].status = PlanetStatus.ON;
            field.addScore(planetActivatedScore);
            if (allPlanetsOn()) {
                if (multiballStatus == MultiballStatus.INACTIVE) {
                    startMultiball(field);
                }
                else {
                    String msg = multiballJackpotMultiplier > 1 ?
                            field.resolveString(
                                    "jackpot_received_with_multiplier_message",
                                    multiballJackpotMultiplier) :
                            field.resolveString("jackpot_received_message");
                    field.showGameMessage(msg, 2000);
                    field.addScore(multiballJackpotScore * multiballJackpotMultiplier);
                    multiballJackpotMultiplier += 1;
                    for (Planet p : planets) {
                        p.status = PlanetStatus.OFF;
                    }
                }
            }
            else {
                String msg = field.resolveString("planet_activated_message", planetIndex + 1);
                field.showGameMessage(msg, 1500);
            }
        }
    }

    private void checkRamp(
            Field field, Ball ball, String prevSensorId, long points, Integer planetIndex) {
        if (prevSensorId.equals(ball.getMostRecentSensorId())) {
            if (rampBonusMultiplier > 1) {
                String msg = field.resolveString("ramp_bonus_message", rampBonusMultiplier);
                field.showGameMessage(msg, 1000);
            }
            rampBonusNanosRemaining = rampBonusDurationNanos;
            field.addScore(points * rampBonusMultiplier);
            rampBonusMultiplier += 1;
            if (planetIndex != null) {
                activatePlanetIfMatch(field, ball, planetIndex);
            }
        }
    }

    private boolean anyBallHasColorForPlanetIndex(Field field, int planetIndex) {
        int color = ballColors.get(planetIndex);
        List<Ball> balls = field.getBalls();
        for (int i = 0; i < balls.size(); i++) {
            if (balls.get(i).getPrimaryColor() == color) {
                return true;
            }
        }
        return false;
    }

    private void updatePlanetStatus(Field field, long nanos) {
        inProgressPlanetPhase += TAU * (nanos / 4e9);
        while (inProgressPlanetPhase > TAU) {
            inProgressPlanetPhase -= TAU;
        }
        for (int i = 0; i < planets.length; i++) {
            Planet p = planets[i];
            if (p.status != PlanetStatus.ON) {
                p.status = anyBallHasColorForPlanetIndex(field, i) ?
                        PlanetStatus.IN_PROGRESS : PlanetStatus.OFF;
            }
            p.element.setAllRolloversActivated(p.status != PlanetStatus.OFF);
            // In-progress planets cycle between 30% and 100% of their full color.
            double phase = (p.status == PlanetStatus.IN_PROGRESS) ?
                    (1 + Math.sin(inProgressPlanetPhase)) * 0.35 : 0;
            p.element.setRolloverColorAtIndex(0, Color.blend(p.color, BLACK, phase));
        }
    }

    void initializePlanets(Field field) {
        sun = field.getFieldElementById("Sun");
        sun.setAllRolloversActivated(true);
        orbits = field.getFieldElementById("Orbits");

        int numPlanets = orbits.numberOfRollovers();
        planets = new Planet[numPlanets];
        Random rand = new Random();
        for (int i = 0; i < numPlanets; i++) {
            Planet p = new Planet();
            planets[i] = p;
            p.element = field.getFieldElementById("Planet" + (i + 1));
            p.radius = p.element.getRolloverRadiusAtIndex(0);
            p.color = planetColors.get(i);
            p.angle = rand.nextDouble() * TAU;
            // Planets closer to the sun have larger angular velocities.
            p.angularVelocity = (0.9 + 0.2 * rand.nextDouble()) / (i + 1);
            p.status = PlanetStatus.OFF;
        }
    }

    @Override public void gameStarted(Field field) {
        launchBarrier = field.getFieldElementById("LaunchBarrier");
        launchBarrier.setRetracted(true);
        initializePlanets(field);
    }

    @Override public boolean isFieldActive(Field field) {
        // Planets should orbit even when there is no active ball.
        return true;
    }

    @Override public void ballLost(Field field) {
        launchBarrier.setRetracted(false);
    }

    @Override public void ballInSensorRange(Field field, SensorElement sensor, Ball ball) {
        String sensorId = sensor.getElementId();
        if ("LaunchBarrierSensor".equals(sensorId)) {
            launchBarrier.setRetracted(false);
        }
        else if ("LaunchBarrierRetract".equals(sensorId)) {
            launchBarrier.setRetracted(true);
        }
        else if ("LeftLoopDetector_Trigger".equals(sensorId)) {
            checkRamp(field, ball, "LeftLoopDetector_Enter", rampScore, 3);
        }
        else if ("RightLoopDetector_Trigger".equals(sensorId)) {
            checkRamp(field, ball, "RightLoopDetector_Enter", rampScore, 2);
        }
        else if ("OrbitDetector_Left".equals(sensorId)) {
            checkRamp(field, ball, "OrbitDetector_Right", rampScore, 4);
        }
        else if ("OrbitDetector_Right".equals(sensorId)) {
            checkRamp(field, ball, "OrbitDetector_Left", rampScore, 4);
        }
    }

    @Override public void allDropTargetsInGroupHit(
            Field field, DropTargetGroupElement targetGroup, Ball ball) {
        String id = targetGroup.getElementId();
        if ("DropTargetLeftSave".equals(id)) {
            ((WallElement) field.getFieldElementById("BallSaver-left")).setRetracted(false);
            field.showGameMessage(field.resolveString("left_save_enabled_message"), 1500);
        }
        else if ("DropTargetRightSave".equals(id)) {
            ((WallElement) field.getFieldElementById("BallSaver-right")).setRetracted(false);
            field.showGameMessage(field.resolveString("right_save_enabled_message"), 1500);
        }
        else if ("Planet1Targets".equals(id)) {
            field.addScore(planet1TargetsScore);
            activatePlanetIfMatch(field, ball, 0);
        }
    }

    @Override public void allRolloversInGroupActivated(
            Field field, RolloverGroupElement group, Ball ball) {
        String id = group.getElementId();
        if ("FlipperRollovers".equals(id)) {
            group.setAllRolloversActivated(false);
            field.incrementAndDisplayScoreMultiplier(1500);
        }
        else if ("Planet2Rollovers".equals(id)) {
            group.setAllRolloversActivated(false);
            field.addScore(planet2RolloversScore);
            activatePlanetIfMatch(field, ball, 1);
        }
        else {
            int planetIndex = -1;
            for (int i = 0; i < planets.length; i++) {
                if (group == planets[i].element) {
                    planetIndex = i;
                    break;
                }
            }
            if (planetIndex >= 0) {
                ball.setPrimaryColor(ballColors.get(planetIndex));
                ball.setSecondaryColor(ballSecondaryColors.get(planetIndex));
                // Planet statuses will be updated in tick().
            }
        }
    }

    Vector2 gravityImpulse = new Vector2();

    @Override public void tick(Field field, long nanos) {
        if (planets == null) {
            initializePlanets(field);
        }
        // Check for exiting multiball.
        boolean gameInProgress = field.getGameState().isGameInProgress();
        if (gameInProgress && field.getBalls().size() <= 1 &&
                multiballStatus == MultiballStatus.ACTIVE) {
            endMultiball(field);
        }
        // Sync planet states with active balls.
        updatePlanetStatus(field, nanos);
        // Update ramp multiplier.
        if (rampBonusNanosRemaining > 0) {
            rampBonusNanosRemaining -= nanos;
        }
        if (rampBonusNanosRemaining <= 0) {
            rampBonusMultiplier = 1;
        }
        // Move planets.
        double dt = nanos / 1e9;
        for (int i = 0; i < planets.length; i++) {
            Planet p = planets[i];
            p.angle += dt * p.angularVelocity;
            while (p.angle > TAU) {
                p.angle -= TAU;
            }
            while (p.angle < 0) {
                p.angle += TAU;
            }
            Vector2 orbitCenter = orbits.getRolloverCenterAtIndex(i);
            double orbitRadius = orbits.getRolloverRadiusAtIndex(i);
            double px = orbitCenter.x + orbitRadius * Math.cos(p.angle);
            double py = orbitCenter.y + orbitRadius * Math.sin(p.angle);
            p.element.setRolloverCenterAtIndex(0, px, py);
        }
        // Apply gravity.
        if (gameInProgress) {
            List<Ball> balls = field.getBalls();
            for (int i = 0; i < balls.size(); i++) {
                Ball ball = balls.get(i);
                Vector2 ballPos = ball.getPosition();
                Vector2 sunPos = sun.getRolloverCenterAtIndex(0);
                double sdx = sunPos.x - ballPos.x;
                double sdy = sunPos.y - ballPos.y;
                double sunDistSq = sdx * sdx + sdy * sdy;
                if (sunDistSq <= gravityRangeSquared) {
                    double sunAngle = Math.atan2(sdy, sdx);
                    double sunForce = sunGravityForce / (gravityDepthSquared + sunDistSq);
                    gravityImpulse.x = (float) (dt * sunForce * Math.cos(sunAngle));
                    gravityImpulse.y = (float) (dt * sunForce * Math.sin(sunAngle));
                    for (Planet planet : planets) {
                        Vector2 planetPos = planet.element.getRolloverCenterAtIndex(0);
                        double mass = Math.pow(planet.radius, 3);
                        double pdx = planetPos.x - ballPos.x;
                        double pdy = planetPos.y - ballPos.y;
                        double planetDistSq = pdx * pdx + pdy * pdy;
                        double planetAngle = Math.atan2(pdy, pdx);
                        double planetForce =
                                planetGravityForce * mass / (gravityDepthSquared + planetDistSq);
                        gravityImpulse.x += (float) (dt * planetForce * Math.cos(planetAngle));
                        gravityImpulse.y += (float) (dt * planetForce * Math.sin(planetAngle));
                    }
                    ball.applyLinearImpulse(gravityImpulse);
                }
            }
        }
    }
}
