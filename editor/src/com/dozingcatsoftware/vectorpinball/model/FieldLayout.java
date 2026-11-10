package com.dozingcatsoftware.vectorpinball.model;

import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asFloat;
import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asFloatList;
import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asInt;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.dozingcatsoftware.vectorpinball.elements.FieldElement;
import com.dozingcatsoftware.vectorpinball.elements.FieldElementCollection;
import com.dozingcatsoftware.vectorpinball.elements.FlipperElement;

public class FieldLayout {

    static final String WIDTH_PROPERTY = "width";
    static final String HEIGHT_PROPERTY = "height";
    static final String DELEGATE_PROPERTY = "delegate";
    static final String TARGET_TIME_RATIO_PROPERTY = "targetTimeRatio";
    static final String GRAVITY_PROPERTY = "gravity";
    static final String NUM_BALLS_PROPERTY = "numballs";
    static final String BALL_RADIUS_PROPERTY = "ballradius";
    static final String BALL_COLOR_PROPERTY = "ballcolor";
    static final String SECONDARY_BALL_COLOR_PROPERTY = "secondaryBallColor";
    static final String LAUNCH_POSITION_PROPERTY = "launchPosition";
    static final String LAUNCH_VELOCITY_PROPERTY = "launchVelocity";
    static final String LAUNCH_RANDOM_VELOCITY_PROPERTY = "launchVelocityRandomDelta";
    static final String LAUNCH_DEAD_ZONE_PROPERTY = "launchDeadZone";
    // If the ball is lost within this duration after launching, it will be saved.
    static final String MERCY_BALL_DURATION_PROPERTY = "mercyBallDuration";
    // For this duration after multiball starts, lost balls will be saved.
    static final String MULTIBALL_SAVER_DURATION_PROPERTY = "multiballSaverDuration";
    // If the ball is lost within this duration after incrementing the multiplier,
    // the increase will be applied to the next ball.
    static final String PRESERVE_MULTIPLIER_INCREASE_DURATION_PROPERTY = "preserveMultiplierIncreaseDuration";
    static final String SCRIPT_PROPERTY = "script";

    static final String VARIABLES_PROPERTY = "variables";
    static final String ELEMENTS_PROPERTY = "elements";

    Random RAND = new Random();

    Map<String, ?> allParameters;
    FieldElementCollection fieldElements;
    float width;
    float height;
    float gravity;
    int numberOfBalls;
    float ballRadius;
    int ballColor;
    int secondaryBallColor;
    float targetTimeRatio;
    long mercyBallDurationMillis;
    long multiballSaverDurationMillis;
    long preserveMultiplierIncreaseDurationMillis;

    List<Float> launchPosition;
    List<Float> launchVelocity;
    List<Float> launchVelocityRandomDelta;
    List<Float> launchDeadZoneRect;

    static final int DEFAULT_BALL_COLOR = Color.fromRGB(255, 0, 0);
    static final int DEFAULT_SECONDARY_BALL_COLOR = Color.fromRGB(176, 176, 176);

    public FieldLayout(Map<String, Object> layoutMap, WorldLayers worlds) {
        this.width = asFloat(layoutMap.get(WIDTH_PROPERTY), 20.0f);
        this.height = asFloat(layoutMap.get(HEIGHT_PROPERTY), 30.0f);
        this.gravity = asFloat(layoutMap.get(GRAVITY_PROPERTY), 4.0f);
        this.targetTimeRatio = asFloat(layoutMap.get(TARGET_TIME_RATIO_PROPERTY));
        this.mercyBallDurationMillis = asInt(layoutMap.get(MERCY_BALL_DURATION_PROPERTY), 20000);
        this.multiballSaverDurationMillis = asInt(layoutMap.get(MULTIBALL_SAVER_DURATION_PROPERTY), 30000);
        this.preserveMultiplierIncreaseDurationMillis = asInt(layoutMap.get(PRESERVE_MULTIPLIER_INCREASE_DURATION_PROPERTY), 7500);
        this.numberOfBalls = asInt(layoutMap.get(NUM_BALLS_PROPERTY), 3);
        this.ballRadius = asFloat(layoutMap.get(BALL_RADIUS_PROPERTY), 0.5f);
        this.ballColor = colorFromMap(layoutMap, BALL_COLOR_PROPERTY, DEFAULT_BALL_COLOR);
        this.secondaryBallColor = colorFromMap(
                layoutMap, SECONDARY_BALL_COLOR_PROPERTY, DEFAULT_SECONDARY_BALL_COLOR);
        this.launchPosition = asFloatList(listForKey(layoutMap, LAUNCH_POSITION_PROPERTY));
        this.launchVelocity = asFloatList(listForKey(layoutMap, LAUNCH_VELOCITY_PROPERTY));
        this.launchVelocityRandomDelta = asFloatList(listForKey(layoutMap, LAUNCH_RANDOM_VELOCITY_PROPERTY));
        this.launchDeadZoneRect = asFloatList(listForKey(layoutMap, LAUNCH_DEAD_ZONE_PROPERTY));

        this.allParameters = layoutMap;
        this.fieldElements = createFieldElements(layoutMap, worlds);
    }

    static List<?> listForKey(Map<?, ?> map, Object key) {
        if (map.containsKey(key)) return (List<?>) map.get(key);
        return Collections.emptyList();
    }

    private FieldElementCollection createFieldElements(
            Map<String, Object> layoutMap, WorldLayers worlds) {
        FieldElementCollection elements = new FieldElementCollection();

        @SuppressWarnings("unchecked")
        Map<String, Object> variables = (Map<String, Object>) layoutMap.get(VARIABLES_PROPERTY);
        if (variables != null) {
            for (String varname : variables.keySet()) {
                elements.setVariable(varname, variables.get(varname));
            }
        }

        for (Object obj : listForKey(layoutMap, ELEMENTS_PROPERTY)) {
            if (!(obj instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) obj;
            elements.addElement(FieldElement.createFromParameters(params, elements, worlds));
        }

        return elements;
    }

    private int colorFromMap(Map<String, ?> map, String key, int defaultColor) {
        @SuppressWarnings("unchecked")
        List<Number> value = (List<Number>) map.get(key);
        return (value != null) ? Color.fromList(value) : defaultColor;
    }

    public List<FieldElement> getFieldElements() {
        return fieldElements.getAllElements();
    }

    public List<FlipperElement> getFlipperElements() {
        return fieldElements.getFlipperElements();
    }

    public List<FlipperElement> getLeftFlipperElements() {
        return fieldElements.getLeftFlipperElements();
    }

    public List<FlipperElement> getRightFlipperElements() {
        return fieldElements.getRightFlipperElements();
    }

    public float getBallRadius() {
        return ballRadius;
    }

    public int getBallColor() {
        return ballColor;
    }

    public int getSecondaryBallColor() {
        return secondaryBallColor;
    }

    public int getNumberOfBalls() {
        return numberOfBalls;
    }

    public long getMercyBallDurationNanos() {
        return mercyBallDurationMillis * 1_000_000L;
    }

    public long getMultiballSaverDurationNanos() {
        return multiballSaverDurationMillis * 1_000_000L;
    }

    public long getPreserveMultiplierIncreaseDurationNanos() {
        return preserveMultiplierIncreaseDurationMillis * 1_000_000L;
    }

    public List<Float> getLaunchPosition() {
        return launchPosition;
    }

    public List<Float> getLaunchDeadZone() {
        return launchDeadZoneRect;
    }

    // Can apply random velocity increment if specified by "launchVelocityRandomDelta" key.
    public List<Float> getLaunchVelocity() {
        float vx = launchVelocity.get(0);
        float vy = launchVelocity.get(1);

        if (launchVelocityRandomDelta.size() >= 2) {
            if (launchVelocityRandomDelta.get(0) > 0) {
                vx += launchVelocityRandomDelta.get(0) * RAND.nextFloat();
            }
            if (launchVelocityRandomDelta.get(1) > 0) {
                vy += launchVelocityRandomDelta.get(1) * RAND.nextFloat();
            }
        }
        return Arrays.asList(vx, vy);
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    /**
     * Returns the desired ratio between real world time and simulation time. The application
     * should adjust the frame rate and/or time interval passed to Field.tick() to keep the
     * ratio as close to this value as possible.
     */
    public float getTargetTimeRatio() {
        return targetTimeRatio;
    }

    /** Returns the magnitude of the gravity vector. */
    public float getGravity() {
        return gravity;
    }

    public String getDelegateClassName() {
        return (String) allParameters.get(DELEGATE_PROPERTY);
    }

    public String getScriptText() {
        return (String) allParameters.get(SCRIPT_PROPERTY);
    }

    public Object getValueWithKey(String key) {
        return fieldElements.getVariable(key);
    }
}
