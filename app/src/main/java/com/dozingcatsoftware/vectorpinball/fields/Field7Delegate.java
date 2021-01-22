package com.dozingcatsoftware.vectorpinball.fields;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.dozingcatsoftware.vectorpinball.model.Ball;
import com.dozingcatsoftware.vectorpinball.model.BaseFieldDelegate;
import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.model.Field;
import com.dozingcatsoftware.vectorpinball.model.Shape;
import com.dozingcatsoftware.vectorpinball.elements.DropTargetGroupElement;
import com.dozingcatsoftware.vectorpinball.elements.FieldElement;
import com.dozingcatsoftware.vectorpinball.elements.RolloverGroupElement;
import com.dozingcatsoftware.vectorpinball.elements.SensorElement;
import com.dozingcatsoftware.vectorpinball.elements.WallElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static com.dozingcatsoftware.vectorpinball.fields.Stars.Constellation;
import static com.dozingcatsoftware.vectorpinball.fields.Stars.StarCatalog;

public class Field7Delegate extends BaseFieldDelegate {

    static final double TAU = 2 * Math.PI;
    static Random RAND = new Random();

    static final List<Constellation> CONSTELLATIONS = Stars.CONSTELLATIONS;
    static final StarCatalog CATALOG = Stars.CATALOG;

    static long billions(int n) {
        return 1_000_000_000L * n;
    }

    static double interp(double start, double end, double fraction) {
        return start + fraction * (end - start);
    }

    enum StarMode {
        WANDERING,
        CONSTELLATION,
    }

    static class ProjectionTarget {
        double rightAscension;
        double declination;
        double angularRadius;
    }

    static class Star2DProjection {
        int size = 0;
        // This is a bit wasteful since only a fraction of the stars will be in the projection for
        // each frame, but it shouldn't be too bad.
        int capacity = CATALOG.size();
        double[] x = new double[capacity];
        double[] y = new double[capacity];
        double[] magnitude = new double[capacity];
        int[] indices = new int[capacity];
        int[] starIndexToProjIndex = new int[capacity];

        int size() {
            return size;
        }

        void clear() {
            size = 0;
            Arrays.fill(this.starIndexToProjIndex, -1);
        }

        void add(double xx, double yy, double mag, int index) {
            this.x[size] = xx;
            this.y[size] = yy;
            this.magnitude[size] = mag;
            this.indices[size] = index;
            this.starIndexToProjIndex[index] = size;
            this.size += 1;
        }
    }

    static class StarState {
        final static double CONSTELLATION_RADIUS_MULTIPLIER = 1.2;

        Set<Integer> activatedStars = new HashSet<>();
        List<Constellation> lockedConstellations = new ArrayList<>();
        Constellation currentConstellation = null;
        ProjectionTarget currentTarget = new ProjectionTarget();
        Star2DProjection projection = new Star2DProjection();

        StarMode mode = StarMode.WANDERING;
        long wanderNanos = 0;
        long wanderPeriodNanos = billions(40);

        ProjectionTarget animateFromTarget = new ProjectionTarget();
        ProjectionTarget animateToTarget = new ProjectionTarget();
        long animationDurationNanos = billions(10);
        long animationElapsedNanos = -1;

        private void updateWanderingProjection(long nanos) {
            wanderNanos = (wanderNanos + nanos) % wanderPeriodNanos;
            // Going from tau to 0 makes the animation go left to right, which looks better.
            currentTarget.rightAscension = TAU * (1 - (1.0 * wanderNanos / wanderPeriodNanos));
            currentTarget.declination = 0;
            currentTarget.angularRadius = 0.4;
        }

        private void updateAnimationProjection(long nanos) {
            animationElapsedNanos += nanos;
            ProjectionTarget src = this.animateFromTarget;
            ProjectionTarget dst = this.animateToTarget;
            double frac = Math.min(1.0, 1.0 * animationElapsedNanos / animationDurationNanos);
            currentTarget.rightAscension = interp(src.rightAscension, dst.rightAscension, frac);
            currentTarget.declination = interp(src.declination, dst.declination, frac);
            currentTarget.angularRadius = interp(src.angularRadius, dst.angularRadius, frac);
            if (animationElapsedNanos >= animationDurationNanos) {
                animationElapsedNanos = -1;
            }
        }

        private void updateConstellationProjection() {
            Constellation dst = this.currentConstellation;
            currentTarget.rightAscension = dst.centerRaRadians;
            currentTarget.declination = dst.centerDecRadians;
            currentTarget.angularRadius = CONSTELLATION_RADIUS_MULTIPLIER * dst.angularRadius;
        }

        void tick(long nanos) {
            if (animationElapsedNanos >= 0) {
                updateAnimationProjection(nanos);
            }
            else if (mode == StarMode.WANDERING) {
                updateWanderingProjection(nanos);
            }
            else if (currentConstellation != null) {
                updateConstellationProjection();
            }
            projectVisibleStars(CATALOG, this.currentTarget, this.projection);
        }

        boolean allStarsInConstellationActive(Constellation c) {
            return this.activatedStars.containsAll(c.starIndices);
        }

        boolean allStarsInCurrentConstellationActive() {
            return currentConstellation != null &&
                    allStarsInConstellationActive(currentConstellation);
        }

        void enterWanderingMode() {
            if (this.mode != StarMode.WANDERING) {
                this.animateToStartPosition();
                this.mode = StarMode.WANDERING;
                this.wanderNanos = 0;
            }
        }

        void resetAndWander() {
            this.activatedStars.clear();
            this.lockedConstellations.clear();
            this.enterWanderingMode();
        }

        void lockCurrentConstellation() {
            this.lockedConstellations.add(this.currentConstellation);
        }

        boolean allConstellationsLocked() {
            return this.lockedConstellations.containsAll(CONSTELLATIONS);
        }

        void activateStarsInActiveConstellationNearPoint(double x, double y) {
            if (this.currentConstellation != null && (x * x + y * y < 1)) {
                for (int starIndex : this.currentConstellation.starIndices) {
                    int pi = this.projection.starIndexToProjIndex[starIndex];
                    if (pi >= 0) {
                        double px = projection.x[pi] / this.currentTarget.angularRadius;
                        double py = projection.y[pi] / this.currentTarget.angularRadius;
                        double dist2 = (x - px) * (x - px) + (y - py) * (y - py);
                        if (dist2 < 0.1 * 0.1) {
                            this.activatedStars.add(starIndex);
                        }
                    }
                }
            }
        }

        void animateToConstellation(Constellation c) {
            this.currentConstellation = c;
            this.animateFromTarget.rightAscension = this.currentTarget.rightAscension;
            this.animateFromTarget.declination = this.currentTarget.declination;
            this.animateFromTarget.angularRadius = this.currentTarget.angularRadius;
            this.animateToTarget.rightAscension = c.centerRaRadians;
            this.animateToTarget.declination = c.centerDecRadians;
            this.animateToTarget.angularRadius = CONSTELLATION_RADIUS_MULTIPLIER * c.angularRadius;
            // Take shorter rotation path if possible.
            if (this.animateFromTarget.rightAscension > c.centerRaRadians + TAU / 2) {
                this.animateFromTarget.rightAscension -= TAU;
            }
            else if (this.animateFromTarget.rightAscension < c.centerRaRadians - TAU / 2) {
                this.animateFromTarget.rightAscension += TAU;
            }
            this.animationElapsedNanos = 0;
        }

        void animateToStartPosition() {
            this.currentConstellation = null;
            this.animateFromTarget.rightAscension = this.currentTarget.rightAscension;
            this.animateFromTarget.declination = this.currentTarget.declination;
            this.animateFromTarget.angularRadius = this.currentTarget.angularRadius;
            this.animateToTarget.rightAscension = 0;
            this.animateToTarget.declination = 0;
            this.animateToTarget.angularRadius = 0.4;
            this.animationElapsedNanos = 0;
        }

        boolean switchToRandomUnlockedConstellation() {
            List<Constellation> candidates = new ArrayList<>();
            for (Constellation c : CONSTELLATIONS) {
                if (c != currentConstellation && !lockedConstellations.contains(c)) {
                    candidates.add(c);
                }
            }
            if (candidates.isEmpty()) {
                return false;
            }
            Constellation dst = candidates.get(RAND.nextInt(candidates.size()));
            animateToConstellation(dst);
            mode = StarMode.CONSTELLATION;
            return true;
        }

        static void projectVisibleStars(
                StarCatalog catalog, ProjectionTarget target, Star2DProjection projection) {
            projection.clear();
            double rad2 = target.angularRadius * target.angularRadius;
            int catSize = catalog.size();
            double sinRa = Math.sin(target.rightAscension);
            double cosRa = Math.cos(target.rightAscension);
            double sinDec = Math.sin(target.declination);
            double cosDec = Math.cos(target.declination);
            // Rotate each star around the Z axis for right ascension, then the Y axis for
            // declination. The point we're looking at will now be at (1, 0, 0), and when we
            // project to 2D, Y becomes X and Z becomes Y.
            for (int i = 0; i < catSize; i++) {
                double x = catalog.x[i];
                double y = catalog.y[i];
                double z = catalog.z[i];
                // https://en.wikipedia.org/wiki/Rotation_matrix#In_three_dimensions
                // Around Z axis:
                // [cos(theta), -sin(theta), 0]
                // [sin(theta), cos(theta), 0]
                // [0, 0, 1]
                // We can treat this as a 2d rotation in the XY plane; z remains constant.
                double x1 = x * cosRa - y * sinRa;
                double y1 = x * sinRa + y * cosRa;
                double z1 = z;
                // Around Y axis:
                // [cos(theta), 0, sin(theta)]
                // [0, 1, 0]
                // [-sin(theta), 0, cos(theta)]
                double x2 = x1 * cosDec + z1 * sinDec;
                double y2 = y1;
                double z2 = -x1 * sinDec + z1 * cosDec;
                // We started with a unit vector so we could normalize [x2, y2, z2], but it
                // shouldn't be too far off. The star is "visible" if it's close enough to the
                // X axis on the positive side.
                double yzOffsetSq = y2 * y2 + z2 * z2;
                if (x2 > 0 && yzOffsetSq < rad2) {
                    projection.add(y2, z2, catalog.magnitude[i], i);
                }
            }
        }
    }

    StarState starState = new StarState();

    static final int BALL_LOCK_LAYER = 4;
    static final int MINITABLE_LAYER = 1;

    static final long STAR_SCORE = 500;
    static final long JACKPOT_SCORE = 100000;
    static final long BASE_RAMP_SCORE = 5000;
    static final long RAMP_SCORE_INCREMENT = 1000;

    enum MultiballStatus {INACTIVE, STARTING, ACTIVE}
    MultiballStatus multiballStatus;
    int numBallsLocked;

    Vector2 starViewCenter;
    double starViewRadius;
    List<RolloverGroupElement> lockRollovers;
    FieldElement leftLoopGuide;
    FieldElement rightLoopGuide;
    FieldElement lockAndJackpotGuide;
    int loopGuideColor = Color.fromRGB(0, 0xAA, 0x66);
    int lockGuideColor = Color.fromRGB(0xCC, 0xCC, 0);
    int jackpotGuideColor = Color.fromRGB(0xAA, 0, 0);
    long guideTickCounter = 0;
    long guideTickMax = billions(5);

    long rampScore;

    WallElement ballSaverLeft;
    WallElement ballSaverRight;

    @Override public boolean isFieldActive(Field field) {
        return true;
    }

    @Override public void gameStarted(Field field) {
        starState = new StarState();
        multiballStatus = MultiballStatus.INACTIVE;
        numBallsLocked = 0;
        rampScore = BASE_RAMP_SCORE;
    }

    @Override public void tick(Field field, long nanos) {
        if (starViewCenter == null) {
            initFieldElements(field);
        }
        starState.tick(nanos);
        updateActivatedStars(field);
        updateBallLockRollovers(field);
        updateGuides(field, nanos);
        field.setShapes(shapesFromProjection());
        if (multiballStatus == MultiballStatus.ACTIVE && field.getBalls().size() <= 1) {
            multiballStatus = MultiballStatus.INACTIVE;
            starState.resetAndWander();
        }
    }

    @Override public void allRolloversInGroupActivated(
            Field field, RolloverGroupElement rolloverGroup, Ball ball) {
        String id = rolloverGroup.getElementId();
        if ("FlipperRollovers".equals(id) || "TopRollovers".equals(id)) {
            // rollover groups increment field multiplier when all rollovers are activated, also reset to inactive
            rolloverGroup.setAllRolloversActivated(false);
            field.incrementAndDisplayScoreMultiplier(1500);
        }
        else if (lockRollovers.contains(rolloverGroup)) {
            this.numBallsLocked++;
            if (this.numBallsLocked == lockRollovers.size()) {
                startMultiball(field);
            }
            else {
                field.removeBallWithoutBallLoss(ball);
                String msg = field.resolveString("ball_locked_message", this.numBallsLocked);
                field.showGameMessage(msg, 3000);
            }
            starState.lockCurrentConstellation();
            starState.enterWanderingMode();
        }
    }

    @Override public void allDropTargetsInGroupHit(
            Field field, DropTargetGroupElement targetGroup, Ball ball) {
        String id = targetGroup.getElementId();
        if ("DropTargetLeftSave".equals(id)) {
            ballSaverLeft.setRetracted(false);
            field.showGameMessage(field.resolveString("left_save_enabled_message"), 1500);
        }
        else if ("DropTargetRightSave".equals(id)) {
            ballSaverRight.setRetracted(false);
            field.showGameMessage(field.resolveString("right_save_enabled_message"), 1500);
        }
        else if ("MiniFieldTopTargets".equals(id) || "MiniFieldLeftTargets".equals(id)) {
            rampScore += RAMP_SCORE_INCREMENT;
            field.showGameMessage(field.resolveString("ramp_bonus_increased_message"), 1500);
        }
    }

    @Override public void ballInSensorRange(final Field field, SensorElement sensor, Ball ball) {
        String id = sensor.getElementId();
        String prevId = ball.getMostRecentSensorId();
        // Enable launch barrier.
        if ("LaunchBarrierSensor".equals(id)) {
            setLaunchBarrierEnabled(field, true);
        }
        else if ("LaunchBarrierRetract".equals(id)) {
            setLaunchBarrierEnabled(field, false);
        }
        else if ("LeftFlipperDropSensor".equals(id) || "RightFlipperDropSensor".equals(id)) {
            ball.getBody().setLinearVelocity(0, 0);
        }
        else if ("MiniTableOrBallLockSensor".equals(id)) {
            int toLayer = MINITABLE_LAYER;
            if (this.multiballStatus == MultiballStatus.INACTIVE &&
                    starState.allStarsInCurrentConstellationActive()) {
                toLayer = BALL_LOCK_LAYER;
            }
            ball.moveToLayer(toLayer);
        }
        else if ("LeftLoopDetector".equals(id) && !id.equals(prevId)) {
            handleLoop(field);
        }
        else if ("RightLoopDetector".equals(id) && !id.equals(prevId)) {
            handleLoop(field);
        }
        else if ("MiniFieldDetector".equals(id)) {
            if (starState.allConstellationsLocked()) {
                doJackpot(field);
            }
        }
        else if (("InnerOrbitLeftTrigger".equals(id) && "InnerOrbitRightTrigger".equals(prevId)) ||
                ("InnerOrbitRightTrigger".equals(id) && "InnerOrbitLeftTrigger".equals(prevId))) {
            // Looped around the center bumpers. Should this do anything else?
            field.addScore(rampScore);
        }
    }

    void initFieldElements(Field field) {
        RolloverGroupElement boundary = field.getFieldElementById("StarViewBoundary");
        starViewCenter = boundary.getRolloverCenterAtIndex(0);
        starViewRadius = boundary.getRolloverRadiusAtIndex(0);
        lockRollovers = Arrays.asList(
                field.getFieldElementById("BallLockRollover1"),
                field.getFieldElementById("BallLockRollover2"),
                field.getFieldElementById("BallLockRollover3")
        );
        leftLoopGuide = field.getFieldElementById("LeftLoopGuide");
        rightLoopGuide = field.getFieldElementById("RightLoopGuide");
        lockAndJackpotGuide = field.getFieldElementById("LockAndJackpotGuide");
        ballSaverLeft = field.getFieldElementById("BallSaver-left");
        ballSaverRight = field.getFieldElementById("BallSaver-right");
    }

    void handleLoop(Field field) {
        field.addScore(rampScore);
        if (!starState.allStarsInCurrentConstellationActive()) {
            if (starState.switchToRandomUnlockedConstellation()) {
                field.showGameMessage(starState.currentConstellation.name, 3000);
            }
        }
    }

    private void setLaunchBarrierEnabled(Field field, boolean enabled) {
        WallElement barrier = field.getFieldElementById("LaunchBarrier");
        barrier.setRetracted(!enabled);
    }

    private void updateActivatedStars(Field field) {
        List<Ball> balls = field.getBalls();
        int numPrevStars = starState.activatedStars.size();
        for (int i = 0; i < balls.size(); i++) {
            Ball ball = balls.get(i);
            if (ball.getLayer() != 0) {
                continue;
            }
            double bx = (ball.getPosition().x - starViewCenter.x) / starViewRadius;
            double by = (ball.getPosition().y - starViewCenter.y) / starViewRadius;
            starState.activateStarsInActiveConstellationNearPoint(bx, by);
        }
        int numNewStars = starState.activatedStars.size() - numPrevStars;
        if (numNewStars > 0) {
            field.addScore(numNewStars * STAR_SCORE);
            if (starState.allStarsInCurrentConstellationActive()) {
                if (multiballStatus == MultiballStatus.INACTIVE) {
                    String key = numBallsLocked == 2 ?
                            "multiball_ready_message" : "ball_lock_ready_message";
                    field.showGameMessage(field.resolveString(key), 2000);
                }
                else {
                    starState.lockCurrentConstellation();
                    String msg = starState.allConstellationsLocked() ?
                            field.resolveString("shoot_ramp_jackpot_message") :
                            field.resolveString("constellation_complete_message",
                                    starState.currentConstellation.name);
                    field.showGameMessage(msg, 2000);
                    starState.enterWanderingMode();
                }
            }
        }
    }

    private void updateBallLockRollovers(Field field) {
        for (int i = 0; i < this.lockRollovers.size(); i++) {
            boolean activated = (this.numBallsLocked > i);
            boolean enabled =
                    (this.multiballStatus == MultiballStatus.INACTIVE && this.numBallsLocked == i);
            RolloverGroupElement rollover = this.lockRollovers.get(i);
            rollover.setAllRolloversActivated(activated);
            rollover.setIgnoreBall(!enabled);
        }
    }

    private int guideColorAlpha() {
        double angle = TAU * guideTickCounter / guideTickMax;
        double frac = (1 + Math.sin(angle)) / 2;
        return (int) (75 + 180 * frac);
    }

    private void updateGuides(Field field, long nanos) {
        guideTickCounter = (guideTickCounter + nanos) % guideTickMax;
        int loopAlpha = 0;
        int lockAlpha = 0;
        int lockBaseColor = lockGuideColor;
        if (field.getGameState().isGameInProgress()) {
            if (multiballStatus == MultiballStatus.ACTIVE &&
                    starState.allConstellationsLocked()) {
                lockBaseColor = jackpotGuideColor;
                lockAlpha = guideColorAlpha();
            }
            else if (multiballStatus == MultiballStatus.INACTIVE &&
                    starState.allStarsInCurrentConstellationActive()) {
                lockAlpha = guideColorAlpha();
            }
            else if (starState.mode == StarMode.WANDERING) {
                loopAlpha = guideColorAlpha();
            }
        }
        int loopColor = Color.withAlpha(loopGuideColor, loopAlpha);
        leftLoopGuide.setNewColor(loopColor);
        rightLoopGuide.setNewColor(loopColor);
        int lockColor = Color.withAlpha(lockBaseColor, lockAlpha);
        lockAndJackpotGuide.setNewColor(lockColor);
    }

    private void launchBallForMulitball(Field field, Ball existingBall) {
        Ball ball = existingBall;
        if (ball == null) {
            Vector2 center =
                    this.lockRollovers.get(this.numBallsLocked - 1).getRolloverCenterAtIndex(0);
            ball = field.createBall(center.x, center.y);
        }
        ball.moveToLayer(BALL_LOCK_LAYER);
        ball.getBody().setLinearVelocity(0, -(5.0f + RAND.nextFloat()));
        field.playBallLaunchSound();
        numBallsLocked--;
    }

    private void startMultiball(final Field field) {
        final Ball ball = field.getBalls().get(0);
        final Body bb = ball.getBody();
        // Position directly over the final lock rollover and cancel gravity.
        final RolloverGroupElement lastLock = this.lockRollovers.get(this.lockRollovers.size() - 1);
        Vector2 center = lastLock.getRolloverCenterAtIndex(0);
        bb.setTransform(center.x, center.y, bb.getAngle());
        bb.setLinearVelocity(0, 0);
        bb.setAngularVelocity(0);
        final float origGravity = bb.getGravityScale();
        bb.setGravityScale(0);

        field.showGameMessage(field.resolveString("multiball_started_message"), 3000);
        this.multiballStatus = MultiballStatus.STARTING;
        ballSaverLeft.setRetracted(false);
        ballSaverRight.setRetracted(false);

        // Release the current ball, then create additional balls over the corresponding rollovers.
        field.scheduleAction(1000, () -> {
            bb.setGravityScale(origGravity);
            launchBallForMulitball(field, ball);
        });
        field.scheduleAction(3500, () -> launchBallForMulitball(field, null));
        field.scheduleAction(6000, () -> {
            launchBallForMulitball(field, null);
            multiballStatus = MultiballStatus.ACTIVE;
        });
    }

    void doJackpot(Field field) {
        field.showGameMessage(field.resolveString("jackpot_received_message"), 3000);
        field.addScore(JACKPOT_SCORE);
        starState.resetAndWander();
    }

    static int ACTIVE_STAR_ACTIVE_CONSTELLATION_COLOR = Color.fromRGB(240, 240, 0);
    static int INACTIVE_STAR_ACTIVE_CONSTELLATION_COLOR = Color.fromRGB(0, 240, 0);
    static int ACTIVE_STAR_INACTIVE_CONSTELLATION_COLOR = Color.fromRGB(192, 192, 0);
    static int INACTIVE_STAR_INACTIVE_CONSTELLATION_COLOR = Color.fromRGB(240, 240, 240);
    static int CONSTELLATION_LINE_COLOR = Color.fromRGBA(240, 240, 240, 192);

    int starColorForIndex(int starIndex) {
        boolean isActive = starState.activatedStars.contains(starIndex);
        boolean isInActiveConstellation = starState.currentConstellation != null &&
                starState.currentConstellation.starIndices.contains(starIndex);
        if (isInActiveConstellation) {
            return isActive ?
                    ACTIVE_STAR_ACTIVE_CONSTELLATION_COLOR :
                    INACTIVE_STAR_ACTIVE_CONSTELLATION_COLOR;
        }
        else {
            return isActive ?
                    ACTIVE_STAR_INACTIVE_CONSTELLATION_COLOR :
                    INACTIVE_STAR_INACTIVE_CONSTELLATION_COLOR;
        }
    }

    List<Shape> shapesFromProjection() {
        Star2DProjection proj = starState.projection;
        double centerX = this.starViewCenter.x;
        double centerY = this.starViewCenter.y;
        double distScale = this.starViewRadius / starState.currentTarget.angularRadius;
        double baseRadius = this.starViewRadius * 0.015;
        List<Shape> shapes = new ArrayList<>();
        for (int i = 0; i < proj.size(); i++) {
            double cx = centerX + proj.x[i] * distScale;
            double cy = centerY + proj.y[i] * distScale;
            double mag = proj.magnitude[i];
            int alpha = (mag <= 0) ? 255 : Math.max(0, (int) (255 - 30 * mag));
            int baseColor = starColorForIndex(proj.indices[i]);
            int color = Color.withAlpha(baseColor, alpha);
            double rmul = (mag <= 0) ? 1.5 : (mag >= 4) ? 0.75 : 1.0;
            shapes.add(Shape.Circle.create(
                    cx, cy, rmul * baseRadius, Shape.FillType.SOLID, 0, color, null));
        }
        // Draw brighter stars (with lower magnitudes) last.
        Collections.reverse(shapes);
        // Lines for activated stars in constellations.
        for (Constellation c : CONSTELLATIONS) {
            for (int starIndex : c.starIndices) {
                if (starState.activatedStars.contains((starIndex))) {
                    Set<Integer> endpoints = c.segmentsByIndex.get(starIndex);
                    if (endpoints != null) {
                        for (int endIndex : endpoints) {
                            if (starState.activatedStars.contains(endIndex)) {
                                int pi1 = proj.starIndexToProjIndex[starIndex];
                                int pi2 = proj.starIndexToProjIndex[endIndex];
                                if (pi1 < 0 || pi2 < 0) {
                                    continue;
                                }
                                double x1 = centerX + proj.x[pi1] * distScale;
                                double y1 = centerY + proj.y[pi1] * distScale;
                                double x2 = centerX + proj.x[pi2] * distScale;
                                double y2 = centerY + proj.y[pi2] * distScale;
                                shapes.add(Shape.Line.create(
                                        x1, y1, x2, y2, 0, CONSTELLATION_LINE_COLOR, null));
                            }
                        }
                    }
                }
            }
        }
        return shapes;
    }
}
