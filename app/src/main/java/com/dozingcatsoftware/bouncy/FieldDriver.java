package com.dozingcatsoftware.bouncy;

import com.dozingcatsoftware.bouncy.util.FrameRateManager;

/**
 * Class to manage the game thread which updates the game's internal state and draws to the
 * FieldView. Controls the frame rate and attempts to keep it as consistent as possible. Because
 * this class manipulates the Field object in a separate thread, all access to the Field from the
 * game thread and main thread must be synchronized.
 */
public class FieldDriver {

    FieldViewManager fieldViewManager;
    Field field;

    boolean running;
    Thread gameThread;
    boolean canDraw = false;

    FrameRateManager frameRateManager = new FrameRateManager(
            new double[] {60, 50, 45, 40, 30},
            new double[] {57, 48, 43, 38});
    double averageFPS;

    // Sleep this long when field.hasActiveElements() is false.
    static long INACTIVE_FRAME_MSECS = 250;

    public void setFieldViewManager(FieldViewManager value) {
        this.fieldViewManager = value;
    }

    public void setField(Field value) {
        this.field = value;
    }

    /** Starts the game thread running. Does not actually start a new game. */
    public void start() {
        running = true;
        gameThread = new Thread() {
            @Override
            public void run() {
                threadMain();
            }
        };
        gameThread.start();
    }

    /** Stops the game thread, which will pause updates to the game state and view redraws. */
    public void stop() {
        running = false;
        try {
            gameThread.join();
        }
        catch(InterruptedException ex) {}
    }


    /**
     * Main loop for the game thread. Repeatedly calls field.tick to advance the game simulation,
     * redraws the field, and sleeps until it's time for the next frame. Dynamically adjusts sleep
     * times in an attempt to maintain a consistent frame rate.
     */
    void threadMain() {
        while (running) {
            frameRateManager.frameStarted();
            boolean fieldActive = true;
            if (field!=null && fieldViewManager.canDraw()) {
                try {
                    synchronized(field) {
                        long nanosPerFrame =
                                (long)(1000000000L / frameRateManager.targetFramesPerSecond());
                        long fieldTickNanos = (long)(nanosPerFrame*field.getTargetTimeRatio());
                        // If field isn't doing anything, sleep for a long time.
                        fieldActive = field.hasActiveElements();
                        if (!fieldActive) {
                            fieldTickNanos =
                                    (long)(INACTIVE_FRAME_MSECS*1000000*field.getTargetTimeRatio());
                        }
                        field.tick(fieldTickNanos, 4);
                    }
                    drawField();
                }
                catch(Exception ex) {
                    ex.printStackTrace();
                }
            }

            // If field is inactive, clear start time history and bail.
            if (!fieldActive) {
                frameRateManager.clearTimestamps();
                setAverageFPS(0);
                try {
                    Thread.sleep(INACTIVE_FRAME_MSECS);
                }
                catch(InterruptedException ignored) {}
                continue;
            }

            frameRateManager.sleepUntilNextFrame();

            // For debugging, show frames per second and other info.
            if (frameRateManager.getTotalFrames() % 100 == 0) {
                fieldViewManager.setDebugMessage(frameRateManager.fpsDebugInfo());
                setAverageFPS(frameRateManager.currentFramesPerSecond());
            }
        }
    }

    /** Calls FieldViewManager.doDraw to render the game field to the display. */
    void drawField() {
        fieldViewManager.draw();
    }

    /**
     * Resets the frame rate and forgets any locked rate, called when rendering quality is changed.
     */
    public void resetFrameRate() {
        frameRateManager.resetFrameRate();
    }

    public double getAverageFPS() {
        return averageFPS;
    }
    public void setAverageFPS(double value) {
        averageFPS = value;
    }

}
