package com.dozingcatsoftware.bouncy;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.dozingcatsoftware.vectorpinball.model.Field;
import com.dozingcatsoftware.vectorpinball.model.IFieldRenderer;

import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * This class handles the common functionality for Canvas and OpenGL-based views, including mapping
 * world coordinates to pixels and handling touch events.
 */

public class FieldViewManager {

    IFieldRenderer fieldRenderer;
    Runnable startGameAction;

    public void setFieldRenderer(IFieldRenderer renderer) {
        this.fieldRenderer = renderer;
    }

    Field field;
    boolean independentFlippers;
    float maxZoom = 1.0f;
    int customLineWidth = 0;

    // x/y offsets and scale are cached at the beginning of draw(), to avoid repeated calls as
    // elements are drawn.
    private float cachedXOffset, cachedYOffset, cachedScale, cachedHeight;

    // Delay after losing a ball, before a touch will launch a new ball.
    static final long END_BALL_DELAY_MS = 750;

    public void setField(Field value) {
        field = value;
    }

    public Field getField() {
        return field;
    }

    public void setIndependentFlippers(boolean value) {
        independentFlippers = value;
    }

    // Line width can be specified directly, otherwise it's a fraction of the smaller width or
    // height dimension. A factor of 1/216 works pretty well; on a 1080p display it's 5, which
    // looks decent and performs reasonably; a Nexus 5x can stay close to 60fps with both the
    // canvas and OpenGL renderer.
    public void setCustomLineWidth(int lineWidth) {
        customLineWidth = lineWidth;
    }

    public int getCustomLineWidth() {
        return customLineWidth;
    }

    public int getLineWidth() {
        int cw = customLineWidth;
        int minDim = Math.min(fieldRenderer.getWidth(), fieldRenderer.getHeight());
        // Line width of more than 1/60 of the screen size is too thick.
        int lineWidth = (cw > 0) ? Math.min(cw, minDim / 60) : minDim / 216;
        return Math.max(lineWidth, 1);
    }

    public void setStartGameAction(Runnable action) {
        startGameAction = action;
    }

    float getScale(float zoom) {
        float xs = fieldRenderer.getWidth() / field.getWidth();
        float ys = fieldRenderer.getHeight() / field.getHeight();
        return Math.min(xs, ys) * zoom;
    }

    float getCachedScale() {
        return cachedScale;
    }

    // Sets maxZoom ivar, zoom will still be 1 when game is not in progress.
    public void setZoom(float value) {
        maxZoom = value;
    }

    /**
     * Saves scale and x and y offsets for use by world2pixel methods, avoiding repeated method
     * calls and math operations.
     */
    private void cacheScaleAndOffsets() {
        cachedHeight = fieldRenderer.getHeight();
        float zr = field.zoomRatio();
        // Apply a cubic Bezier function to smoothly accelerate and decelerate.
        float easedRatio = zr * zr * (3.0f - 2.0f * zr);
        // The actual zoom factor ranges from 1 when zoomRatio() is 0 to `maxZoom` when it's 1.
        float zoomFactor = 1 + (maxZoom - 1) * easedRatio;
        cachedScale = getScale(zoomFactor);
        // Center the zoomed view on the ball if available, or the launch position if not.
        Vector2 center = field.zoomCenterPoint();
        // `spanX` and `spanY` are how many world units are visible when zoomed. We don't want
        // the zoomed view to extend to less than 0, or greater than the field size. If the
        // span is greater than the table's width/height, then apply a negative offset so that
        // the table is centered.
        float spanX = fieldRenderer.getWidth() / cachedScale;
        if (spanX >= field.getWidth()) {
            cachedXOffset = -(spanX - field.getWidth()) / 2;
        }
        else {
            float rawXOffset = center.x - spanX / 2;
            float maxXOffset = field.getWidth() - spanX;
            cachedXOffset = MathUtils.clamp(rawXOffset, 0, maxXOffset);
        }

        float spanY = fieldRenderer.getHeight() / cachedScale;
        if (spanY >= field.getHeight()) {
            cachedYOffset = -(spanY - field.getHeight()) / 2;
        }
        else {
            float rawYOffset = center.y - spanY / 2;
            float maxYOffset = field.getHeight() - spanY;
            cachedYOffset = MathUtils.clamp(rawYOffset, 0, maxYOffset);
        }
    }

    // world2pixel methods assume cacheScaleAndOffsets has been called previously.

    /** Converts an x coordinate from world coordinates to the view's pixel coordinates. */
    public float world2pixelX(float x) {
        return (x - cachedXOffset) * cachedScale;
    }

    /**
     * Converts a y coordinate from world coordinates to the view's pixel coordinates.
     * In world coordinates, positive y is up, in pixel coordinates, positive y is down.
     */
    public float world2pixelY(float y) {
        return cachedHeight - ((y - cachedYOffset) * cachedScale);
    }

    // For compatibility with Android 1.6, use reflection for multitouch features.
    boolean hasMultitouch;
    Method getPointerCountMethod;
    Method getXMethod;
    int MOTIONEVENT_ACTION_MASK = 0xffffffff; // Defaults to no-op AND mask.
    int MOTIONEVENT_ACTION_POINTER_UP;
    int MOTIONEVENT_ACTION_POINTER_INDEX_MASK;
    int MOTIONEVENT_ACTION_POINTER_INDEX_SHIFT;

    {
        try {
            getPointerCountMethod = MotionEvent.class.getMethod("getPointerCount");
            getXMethod = MotionEvent.class.getMethod("getX", int.class);
            MOTIONEVENT_ACTION_MASK = MotionEvent.class.getField("ACTION_MASK").getInt(null);
            MOTIONEVENT_ACTION_POINTER_UP =
                    MotionEvent.class.getField("ACTION_POINTER_UP").getInt(null);
            MOTIONEVENT_ACTION_POINTER_INDEX_MASK =
                    MotionEvent.class.getField("ACTION_POINTER_INDEX_MASK").getInt(null);
            MOTIONEVENT_ACTION_POINTER_INDEX_SHIFT =
                    MotionEvent.class.getField("ACTION_POINTER_INDEX_SHIFT").getInt(null);
            hasMultitouch = true;
        }
        catch (Exception ex) {
            hasMultitouch = false;
        }
    }

    void launchBallIfNeeded() {
        // Don't launch a new ball immediately after losing the previous ball, since the user may
        // have been trying to use a flipper.
        if (field.ballLostWithinMillis(END_BALL_DELAY_MS)) {
            return;
        }
        // Remove "dead" balls and launch if none already in play.
        field.removeDeadBalls();
        if (field.getBalls().size() == 0) field.launchBall();
    }

    /**
     * Called when the view is touched. Activates flippers, starts a new game if one is not in
     * progress, and launches a ball if one is not in play.
     */
    public boolean handleTouchEvent(MotionEvent event) {
        int actionType = event.getAction() & MOTIONEVENT_ACTION_MASK;
        synchronized (field) {
            if (!field.getGameState().isGameInProgress() || field.getGameState().isPaused()) {
                if (startGameAction != null) {
                    startGameAction.run();
                    return true;
                }
            }
            // activate or deactivate flippers
            boolean left = false, right = false;
            if (this.independentFlippers && this.hasMultitouch) {
                try {
                    // Determine whether to activate left and/or right flippers,
                    // using reflection for Android 2.2 multitouch APIs.
                    if (actionType != MotionEvent.ACTION_UP) {
                        int npointers = (Integer) getPointerCountMethod.invoke(event);
                        // If pointer was lifted (ACTION_POINTER_UP), get its index so we don't
                        // count it as pressed.
                        int liftedPointerIndex = -1;
                        if (actionType == MOTIONEVENT_ACTION_POINTER_UP) {
                            liftedPointerIndex =
                                    (event.getAction() & MOTIONEVENT_ACTION_POINTER_INDEX_MASK) >>
                                            MOTIONEVENT_ACTION_POINTER_INDEX_SHIFT;
                        }
                        float halfwidth = fieldRenderer.getWidth() / 2.0f;
                        for (int i = 0; i < npointers; i++) {
                            if (i != liftedPointerIndex) {
                                float touchx = (Float) getXMethod.invoke(event, i);
                                if (touchx < halfwidth) left = true;
                                else right = true;
                            }
                        }
                    }
                }
                catch (Exception ignored) {
                }
            }
            else {
                left = right = !(event.getAction() == MotionEvent.ACTION_UP);
            }
            if (actionType == MotionEvent.ACTION_DOWN) {
                launchBallIfNeeded();
            }
            updateFlippersFromTouchEvent(left, right);
        }
        return true;
    }

    // These instance variables and the annoyingly complex logic in `updateFlippersFromTouchEvent`
    // are a workaround for an edge case in Android's touch behavior. On some devices, if you touch
    // near the edge of the screen, Android will not immediately send a touch event because that
    // might just be incidental contact from how you're holding it. A touch event is only sent if
    // you break contact quickly, and in that case Android sends a "touch down" event followed very
    // quickly by a "touch up" event. On my Pixel 3a, the time between down and up events is around
    // 10ms, compared to 50-100ms for quick touches not at the edge of the screen.
    //
    // If we handled those events normally, then the flipper would be engaged and then almost
    // immediately disengaged, so it would barely move at all and not be able to apply force to
    // the ball. Instead, we keep track of the most recent time that the left and right flippers
    // were engaged. If we get a "touch up" event that is too soon after that, rather than
    // disengaging the flipper right away, we schedule a future action. This ensures that flippers
    // will be engaged for at least 50ms, which is typically long enough for them to fully rotate.
    //
    // Edge touches are still not ideal because the flipper will only start to move after you
    // release your finger, rather than when you first touch the screen. But this at least gives you
    // a chance of hitting the ball, and it doesn't affect non-edge touches.
    private Long leftFlipperActivationMillis = null;
    private Long rightFlipperActivationMillis = null;
    private final long minFlipperActivationMillis = 50;
    private final Handler handler = new Handler();

    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    private void updateFlippersFromTouchEvent(boolean left, boolean right) {
        if (left && leftFlipperActivationMillis == null) {
            leftFlipperActivationMillis = currentTimeMillis();
        }
        if (right && rightFlipperActivationMillis == null) {
            rightFlipperActivationMillis = currentTimeMillis();
        }
        // Treat both active separately because setAllFlippersEngaged will cycle the rollovers,
        // as opposed to separate calls to set(Left|Right)FlippersEngaged which would result in
        // cycling one way and then immediately back the other, for no net change.
        if (left && right) {
            field.setAllFlippersEngaged(true);
        }
        else {
            if (left) {
                field.setLeftFlippersEngaged(true);
            }
            else {
                Long interval = (leftFlipperActivationMillis != null) ?
                        currentTimeMillis() - leftFlipperActivationMillis : null;
                // if (interval != null) android.util.Log.i("FVM", "FVM Left flipper interval: " + interval);
                if (interval == null || interval >= minFlipperActivationMillis) {
                    field.setLeftFlippersEngaged(false);
                    leftFlipperActivationMillis = null;
                }
                else {
                    long delay = minFlipperActivationMillis - interval;
                    // android.util.Log.i("FVM", "Delaying left flipper release: " + delay);
                    final long start = leftFlipperActivationMillis;
                    handler.postDelayed(() -> {
                        // Only deactivate the flipper if its state hasn't changed.
                        if (leftFlipperActivationMillis != null && leftFlipperActivationMillis == start) {
                            // long actualDelay = currentTimeMillis() - start;
                            // android.util.Log.i("FVM", "Releasing left flipper after delay: " + actualDelay);
                            field.setLeftFlippersEngaged(false);
                            leftFlipperActivationMillis = null;
                        }
                    }, delay);
                }
            }

            if (right) {
                field.setRightFlippersEngaged(true);
            }
            else {
                Long interval = (rightFlipperActivationMillis != null) ?
                        currentTimeMillis() - rightFlipperActivationMillis : null;
                // if (interval != null) android.util.Log.i("FVM", "FVM Right flipper interval: " + interval);
                if (interval == null || interval >= minFlipperActivationMillis) {
                    field.setRightFlippersEngaged(false);
                    rightFlipperActivationMillis = null;
                }
                else {
                    long delay = minFlipperActivationMillis - interval;
                    // android.util.Log.i("FVM", "Delaying right flipper release: " + delay);
                    final long start = rightFlipperActivationMillis;
                    handler.postDelayed(() -> {
                        if (rightFlipperActivationMillis != null && rightFlipperActivationMillis == start) {
                            // long actualDelay = currentTimeMillis() - start;
                            // android.util.Log.i("FVM", "Releasing right flipper after delay: " + actualDelay);
                            field.setRightFlippersEngaged(false);
                            rightFlipperActivationMillis = null;
                        }
                    }, delay);
                }
            }
        }
    }

    static List<Integer> LEFT_FLIPPER_KEYS = Arrays.asList(
            KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_DPAD_LEFT);
    static List<Integer> RIGHT_FLIPPER_KEYS = Arrays.asList(
            KeyEvent.KEYCODE_SLASH, KeyEvent.KEYCODE_DPAD_RIGHT);
    static List<Integer> ALL_FLIPPER_KEYS = Arrays.asList(
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER);

    public boolean handleKeyDown(int keyCode, KeyEvent event) {
        // android.util.Log.i("FVM", "key down: " + event.getKeyCode());
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            synchronized (field) {
                // Don't let a pressed flipper key start a game, but do launch a ball if needed.
                if (!field.getGameState().isGameInProgress() || field.getGameState().isPaused()) {
                    return false;
                }
                boolean isActionKey = updateFlippersForKeyCode(keyCode, true);
                if (isActionKey) launchBallIfNeeded();
                return isActionKey;
            }
        }
        return false;
    }

    public boolean handleKeyUp(int keyCode, KeyEvent event) {
        // android.util.Log.i("FVM", "key up: " + event.getKeyCode());
        if (event.getAction() == KeyEvent.ACTION_UP) {
            synchronized (field) {
                if (!field.getGameState().isGameInProgress() || field.getGameState().isPaused()) {
                    return false;
                }
                return updateFlippersForKeyCode(keyCode, false);
            }
        }
        return false;
    }

    private boolean updateFlippersForKeyCode(int keyCode, boolean isPressed) {
        if (LEFT_FLIPPER_KEYS.contains(keyCode)) {
            field.setLeftFlippersEngaged(isPressed);
            return true;
        }
        if (RIGHT_FLIPPER_KEYS.contains(keyCode)) {
            field.setRightFlippersEngaged(isPressed);
            return true;
        }
        if (ALL_FLIPPER_KEYS.contains(keyCode)) {
            field.setAllFlippersEngaged(isPressed);
            return true;
        }
        return false;
    }

    public void draw() {
        cacheScaleAndOffsets();
        fieldRenderer.doDraw();
    }
 }
