package com.dozingcatsoftware.bouncy.fields;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.dozingcatsoftware.bouncy.Ball;
import com.dozingcatsoftware.bouncy.BaseFieldDelegate;
import com.dozingcatsoftware.bouncy.Field;
import com.dozingcatsoftware.bouncy.elements.DropTargetGroupElement;
import com.dozingcatsoftware.bouncy.elements.FieldElement;
import com.dozingcatsoftware.bouncy.elements.RolloverGroupElement;
import com.dozingcatsoftware.bouncy.elements.SensorElement;
import com.dozingcatsoftware.bouncy.elements.WallElement;

public class Field2Delegate extends BaseFieldDelegate {

    static final double TAU = 2*Math.PI; // pi is wrong.

    static class RotatingGroup {
        String[] elementIDs;
        double centerX, centerY;
        double radius;
        double rotationSpeed;
        double startAngle;
        double currentAngle;
        double angleIncrement;

        public RotatingGroup(String[] ids, double cx, double cy, double radius, double startAngle, double speed) {
            this.elementIDs = ids;
            this.centerX = cx;
            this.centerY = cy;
            this.radius = radius;
            this.rotationSpeed = speed;
            this.startAngle = this.currentAngle = startAngle;
            this.angleIncrement = TAU / ids.length;
        }

        /**
         * Creates a RotatingGroup by computing the distance and angle to center from the first
         * element ID in the ids array.
         */
        public static RotatingGroup create(Field field, String[] ids, double cx, double cy, double speed) {
            FieldElement element = field.getFieldElementById(ids[0]);
            Body body = element.getBodies().get(0);
            Vector2 position = body.getPosition();
            double radius = Math.hypot(position.x - cx, position.y - cy);
            double angle = Math.atan2(position.y - cy, position.x - cx);
            return new RotatingGroup(ids, cx, cy, radius, angle, speed);
        }

        public void applyRotation(Field field, double dt) {
            currentAngle += dt*rotationSpeed;
            if (currentAngle>TAU) currentAngle -= TAU;
            if (currentAngle<0) currentAngle += TAU;
            for(int i=0; i<elementIDs.length; i++) {
                double angle = currentAngle + angleIncrement*i;

                FieldElement element = field.getFieldElementById(elementIDs[i]);
                Body body = element.getBodies().get(0);
                double x = centerX + radius*Math.cos(angle);
                double y = centerY + radius*Math.sin(angle);
                body.setTransform((float)x, (float)y, body.getAngle());
            }
        }


    }

    RotatingGroup[] rotatingGroups;

    RotatingGroup createRotatingGroup(Field field, String centerID, String[] ids, double speed) {
        FieldElement centerElement = field.getFieldElementById(centerID);
        Vector2 centerPosition = centerElement.getBodies().get(0).getPosition();
        return RotatingGroup.create(field, ids, centerPosition.x, centerPosition.y, speed);
    }

    void setupRotatingGroups(Field field) {
        // Read rotation params from variables defined in the field.
        float b1Speed = ((Number)field.getValueWithKey("RotatingBumper1Speed")).floatValue();
        float b2Speed = ((Number)field.getValueWithKey("RotatingBumper2Speed")).floatValue();
        float b2cx = ((Number)field.getValueWithKey("RotatingBumper2CenterX")).floatValue();
        float b2cy = ((Number)field.getValueWithKey("RotatingBumper2CenterY")).floatValue();
        String[] group1Ids = {
                "RotatingBumper1A", "RotatingBumper1B", "RotatingBumper1C", "RotatingBumper1D"
        };
        rotatingGroups = new RotatingGroup[] {
                createRotatingGroup(field, "CenterBumper1", group1Ids, b1Speed),
                RotatingGroup.create(field, new String[] {"RotatingBumper2A", "RotatingBumper2B"},
                        b2cx, b2cy, b2Speed)
        };
    }

    @Override
    public void tick(Field field, long nanos) {
        if (rotatingGroups==null) {
            setupRotatingGroups(field);
        }

        double seconds = nanos/1e9;
        for(int i=0; i<rotatingGroups.length; i++) {
            rotatingGroups[i].applyRotation(field, seconds);
        }
    }

    private void restoreLeftBallSaver(Field field) {
        ((WallElement)field.getFieldElementById("BallSaver-left")).setRetracted(false);
    }

    private void restoreRightBallSaver(Field field) {
        ((WallElement)field.getFieldElementById("BallSaver-right")).setRetracted(false);
    }

    void startMultiball(final Field field) {
        field.showGameMessage("Multiball!", 2000);
        restoreLeftBallSaver(field);
        restoreRightBallSaver(field);

        Runnable launchBall = new Runnable() {
            @Override
            public void run() {
                if (field.getBalls().size()<3) field.launchBall();
            }
        };
        field.scheduleAction(1000, launchBall);
        field.scheduleAction(3500, launchBall);
    }

    /** Always return true so the rotating bumpers animate smoothly */
    @Override public boolean isFieldActive(Field field) {
        return true;
    }

    @Override public void allRolloversInGroupActivated(Field field, RolloverGroupElement rolloverGroup) {
        // Rollover groups increment field multiplier when all rollovers are activated.
        rolloverGroup.setAllRolloversActivated(false);
        field.getGameState().incrementScoreMultiplier();
        field.showGameMessage(((int)field.getGameState().getScoreMultiplier()) + "x Multiplier", 1500);
    }

    @Override public void processCollision(Field field, FieldElement element, Body hitBody, Ball ball) {
        // When center red bumper is hit, start multiball if all center rollovers are lit,
        // otherwise retract left barrier.
        String elementID = element.getElementId();
        if ("CenterBumper1".equals(elementID)) {
            WallElement barrier = (WallElement)field.getFieldElementById("LeftTubeBarrier");
            RolloverGroupElement multiballRollovers =
                    (RolloverGroupElement)field.getFieldElementById("ExtraBallRollovers");

            if (multiballRollovers.allRolloversActive()) {
                barrier.setRetracted(false);
                startMultiball(field);
                multiballRollovers.setAllRolloversActivated(false);
            }
            else {
                // don't retract during multiball
                if (field.getBalls().size()==1) {
                    barrier.setRetracted(true);
                }
            }
        }
    }

    @Override
    public void allDropTargetsInGroupHit(Field field, DropTargetGroupElement targetGroup) {
        // activate ball saver for left and right groups, "increment" multiball rollover for left/right/center column
        int startRolloverIndex = -1;
        String id = targetGroup.getElementId();
        if ("DropTargetLeft".equals(id)) {
            restoreLeftBallSaver(field);
            field.showGameMessage("Left Save Enabled", 1500);
            startRolloverIndex = 0;
        }
        else if ("DropTargetRight".equals(id)) {
            restoreRightBallSaver(field);
            field.showGameMessage("Right Save Enabled", 1500);
            startRolloverIndex = 2;
        }
        else if ("DropTargetTopLeft".equals(id)) {
            startRolloverIndex = 1;
        }

        // activate next rollover for appropriate column if possible
        if (startRolloverIndex>=0) {
            RolloverGroupElement multiballRollovers = (RolloverGroupElement)field.getFieldElementById("ExtraBallRollovers");
            int numRollovers = multiballRollovers.numberOfRollovers();
            while (startRolloverIndex < numRollovers) {
                if (!multiballRollovers.isRolloverActiveAtIndex(startRolloverIndex)) {
                    multiballRollovers.setRolloverActiveAtIndex(startRolloverIndex, true);

                    if (multiballRollovers.allRolloversActive()) {
                        field.showGameMessage("Shoot Red Bumper", 1500);
                    }
                    break;
                }
                else {
                    startRolloverIndex += 3;
                }
            }
        }

    }

    // support for enabling launch barrier after ball passes by it and hits sensor, and disabling for new ball or new game
    void setLaunchBarrierEnabled(Field field, boolean enabled) {
        WallElement barrier = (WallElement)field.getFieldElementById("LaunchBarrier");
        barrier.setRetracted(!enabled);
    }

    @Override
    public void ballInSensorRange(final Field field, SensorElement sensor, Ball ball) {
        String sensorID = sensor.getElementId();
        // enable launch barrier
        if ("LaunchBarrierSensor".equals(sensorID)) {
            setLaunchBarrierEnabled(field, true);
        }
        else if ("LaunchBarrierRetract".equals(sensorID)) {
            setLaunchBarrierEnabled(field, false);
        }
        else if ("LeftTubeSensor".equals(sensorID)) {
            if (ball.getLinearVelocity().y > 0) {
                // ball going up, retract barrier after delay
                field.scheduleAction(1000, new Runnable() {
                    @Override
                    public void run() {
                        WallElement barrier = (WallElement)field.getFieldElementById("LeftTubeBarrier");
                        barrier.setRetracted(false);
                    }
                });
            }
        }
    }

    @Override
    public void gameStarted(Field field) {
        setLaunchBarrierEnabled(field, false);
    }

    @Override
    public void ballLost(Field field) {
        setLaunchBarrierEnabled(field, false);
    }
}
