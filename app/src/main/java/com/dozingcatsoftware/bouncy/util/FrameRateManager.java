package com.dozingcatsoftware.bouncy.util;

import java.util.LinkedList;

/**
 * This class records how long frames take to generate and display, for assistance in maintaining a
 * consistent frame rate. A FrameRateManager object is created with a list of target frames per
 * second, and a list with the minimum frames per second for each target. Before each frame is
 * generated, the client should call frameStarted(). The FrameRateManager will determine whether the
 * client is maintaining the desired frame rate; if not, it will reduce the target frame rate if
 * possible. The client can call nanosToWaitUntilNextFrame() to determine the optimum number of
 * nanoseconds to wait before starting the next frame, and sleepUntilNextFrame() to sleep the
 * current thread for that interval.
 */

public class FrameRateManager {

    double[] targetFrameRates;
    double[] minimumFrameRates;
    int currentRateIndex = 0;
    long currentNanosPerFrame;

    // Apply fudge factor to requested frame rates; if 60 fps requested, internally aim for 60.9.
    double targetFrameRateFudgeFactor = 1.015;
    double[] unfudgedTargetFrameRates; // Report un-fudged target frame rates to client.

    LinkedList<Long> previousFrameTimestamps = new LinkedList<Long>();

    int frameHistorySize = 10;
    boolean allowReducingFrameRate = true;
    boolean allowLockingFrameRate = true;

    boolean frameRateLocked = false;

    int maxGoodFrames = 500; // After maintaining target FPS for this many frames, lock frame rate.
    int maxSlowFrames = 150; // After this many slow frames, reduce target FPS if possible.

    double currentFPS = -1;
    int goodFrames = 0;
    int slowFrames = 0;

    long totalFrames = 0;

    final static long BILLION = 1000000000L; // nanoseconds per second.
    final static long MILLION = 1000000L; // nanoseconds per millisecond.

    /**
     * Creates a new FrameRateManager object with the specified target frame rates. The first array
     * contains the desired frame rates, and the second array contains the minimum frame rates that
     * must be maintained in order to consider the target rate met. For example, if the targetRates
     * array is {60, 45, 30} and the minRates array is {57, 43}, the FrameRateManager will initially
     * attempt to maintain a rate of 60 fps. If the measured frame rate falls below 57 fps for a
     * sufficient number of frames, the FrameRateManager will switch the target rate to 45 fps. If
     * the measured frame rate subsequently falls below 43 fps, the FrameRateManager will switch the
     * target rate to the final 30 fps, where it will remain until reset. The length of minRates
     * must be at least targetRates.length-1. (It can be longer, extra values are ignored).
     */
    public FrameRateManager(double[] targetRates, double[] minRates) {
        if (targetRates==null || minRates==null || minRates.length < targetRates.length-1) {
            throw new IllegalArgumentException("Must specify as many minimum rates as target rates minus one");
        }

        this.unfudgedTargetFrameRates = targetRates;
        this.minimumFrameRates = minRates;

        this.targetFrameRates = new double[targetRates.length];
        for(int i=0; i<targetRates.length; i++) {
            this.targetFrameRates[i] = targetFrameRateFudgeFactor * targetRates[i];
        }

        setCurrentRateIndex(0);
    }

    /**
     * Creates a new FrameRateManager with the given target frame rate. Will not attempt to reduce
     * the target frame rate if the measured frame rate is lower.
     */
    public FrameRateManager(double frameRate) {
        this(new double[] {frameRate}, new double[0]);
    }

    /**
     * Clears the history of frame starting times. Should be called when the app is paused or
     * otherwise not generating frames, to avoid inaccurate frame rates when it starts again.
     */
    public void clearTimestamps() {
        previousFrameTimestamps.clear();
        goodFrames = 0;
        slowFrames = 0;
        currentFPS = -1;
    }

    void setCurrentRateIndex(int index) {
        currentRateIndex = index;
        currentNanosPerFrame = (long)(BILLION / targetFrameRates[currentRateIndex]);
    }

    /** Internal method to reduce the target frame rate to the next lower value. */
    void reduceFPS() {
        setCurrentRateIndex(currentRateIndex + 1);
        goodFrames = 0;
        slowFrames = 0;
        frameRateLocked = false;
    }

    /**
     * Restores the target frame rate to the maximum value, and clears the history of frame starting
     * times. Should be called when the app changes state such that frame rendering times may be
     * different from the past (e.g. a new game level is started), and the previous target frame
     * rate may no longer be ideal.
     */
    public void resetFrameRate() {
        clearTimestamps();
        setCurrentRateIndex(0);
        frameRateLocked = false;
    }


    /**
     * Records the given frame start time in nanoseconds. Normally clients will call the no-argument
     * frameStarted(), which calls this method with System.nanoTime(). Updates current frame rate,
     * and adjusts target rate if it has not been met for a sufficient number of frames. Should be
     * called at the beginning of the frame generation.
     */
    public void frameStarted(long time) {
        ++totalFrames;
        previousFrameTimestamps.add(time);
        if (previousFrameTimestamps.size() > frameHistorySize) {
            long firstTime = previousFrameTimestamps.removeFirst();
            double seconds = (time - firstTime) / (double)BILLION;
            currentFPS = frameHistorySize / seconds;

            if (!frameRateLocked && currentRateIndex < minimumFrameRates.length) {
                if (currentFPS < minimumFrameRates[currentRateIndex]) {
                    // Too slow; increment slow frame counter and reduce FPS if hit limit.
                    ++slowFrames;
                    if (slowFrames >= maxSlowFrames) {
                        reduceFPS();
                    }
                }
                else {
                    ++goodFrames;
                    if (maxGoodFrames > 0 && goodFrames >= maxGoodFrames) {
                        // Enough good frames to lock frame rate. Future slowdowns will be assumed
                        // to be temporary.
                        if (allowLockingFrameRate) {
                            frameRateLocked = true;
                        }
                        // Reset frame counters in any case, so we won't slow down after 150 bad
                        // frames and a million good ones.
                        slowFrames = 0;
                        goodFrames = 0;
                    }
                }
            }
        }
    }

    /** Calls frameStarted() with the current system time. */
    public void frameStarted() {
        frameStarted(System.nanoTime());
    }

    /**
     * Returns the current frames per second, based on previously recorded frame times. If there
     * have not been a sufficient number of times recorded, returns -1.
     */
    public double currentFramesPerSecond() {
        return currentFPS;
    }

    /** Returns the target frame rate. */
    public double targetFramesPerSecond() {
        return unfudgedTargetFrameRates[currentRateIndex];
    }

    /** Returns a string with the current frames per second, formatted to 1 decimal place. */
    public String formattedCurrentFramesPerSecond() {
        return String.format("%.1f", currentFPS);
    }

    /**
     * Returns a String with debugging info, including current frame rate, target rate, and
     * whether the rate is locked.
     */
    public String fpsDebugInfo() {
        return String.format("FPS: %.1f target: %.1f %s",
                currentFPS, targetFramesPerSecond(), (frameRateLocked) ? "(locked)" : "");
    }

    /** Returns the time of the last call to frameStarted(). */
    public long lastFrameStartTime() {
        return previousFrameTimestamps.getLast();
    }

    /**
     * Returns the best number of nanoseconds to wait before starting the next frame, based on
     * previously recorded frame start times. The argument is the system time in nanoseconds;
     * clients should normally use the no-argument nanosToWaitUntilNextFrame(), which calls this
     * method with an argument of System.nanoTime().
     */
    public long nanosToWaitUntilNextFrame(long time) {
        long lastStartTime = previousFrameTimestamps.getLast();
        long singleFrameGoalTime = lastStartTime + currentNanosPerFrame;
        long waitTime = singleFrameGoalTime - time;
        // adjust based on previous frame rates
        if (previousFrameTimestamps.size()==frameHistorySize) {
            long multiFrameGoalTime =
                    previousFrameTimestamps.getFirst() + frameHistorySize*currentNanosPerFrame;
            long behind = singleFrameGoalTime - multiFrameGoalTime;
            // behind>0 means we're behind schedule and should decrease wait time
            // behind<0 means we're ahead of schedule, but don't adjust
            if (behind > 0) waitTime -= behind;
        }

        // always wait for at least 1 millisecond
        if (waitTime < MILLION) waitTime = MILLION;
        return waitTime;
    }

    /** Calls nanosToWaitUntilNextFrame() with the current system time. */
    public long nanosToWaitUntilNextFrame() {
        return nanosToWaitUntilNextFrame(System.nanoTime());
    }

    /**
     * Sleeps the current thread until the next frame should start generation. The time the current
     * thread sleeps is the number of nanoseconds returned by nanosToWaitUntilNextFrame(). Returns
     * immediately if an InterruptedException is raised during sleep. Returns the number of
     * nanoseconds slept.
     */
    public long sleepUntilNextFrame() {
        long nanos = nanosToWaitUntilNextFrame(System.nanoTime());
        try {
            Thread.sleep(nanos/MILLION, (int)(nanos%MILLION));
        }
        catch(InterruptedException ignored) {}
        return nanos;
    }

    public boolean allowReducingFrameRate() {
        return allowReducingFrameRate;
    }

    /**
     * Sets whether the FrameRateManager should reduce the target frame rate if the current rate
     * falls below the minimum value set in the constructor. Defaults to true.
     */
    public void setAllowReducingFrameRate(boolean value) {
        allowReducingFrameRate = value;
    }

    public boolean allowLockingFrameRate() {
        return allowLockingFrameRate;
    }

    /**
     * Sets whether the FrameRateManager should lock the current frame rate if it has been
     * successfully maintained for a sufficient number of frames. This is useful so that a
     * temporary slowdown won't permanently reduce the target frame rate, once the app has
     * demonstrated that it can normally maintain the existing rate. Defaults to true.
     */
    public void setAllowLockingFrameRate(boolean value) {
        allowLockingFrameRate = value;
    }

    /**
     * Returns the total number of frames recorded by calls to frameStarted(). This can be used
     * when clients want to perform an action every N frames, such as updating an FPS display.
     */
    public long getTotalFrames() {
        return totalFrames;
    }

}
