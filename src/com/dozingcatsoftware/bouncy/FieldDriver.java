package com.dozingcatsoftware.bouncy;

import java.util.LinkedList;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

/** Class to manage the game thread which updates the game's internal state and draws to the FieldView. Controls the
 * frame rate and attempts to keep it as consistent as possible. Because this class manipulates the Field object in a
 * separate thread, all access to the Field from the game thread and main thread must be synchronized.
 * 
 * @author brian
 */
public class FieldDriver implements SurfaceHolder.Callback {
	
	FieldView fieldView;
	SurfaceHolder surfaceHolder;
	Field field;
	
	boolean running;
	Thread gameThread;
	boolean canDraw = false;
	
	// Possible frame rates. The thread loop will start with the fastest, and increase frame intervals
	// if the device can't generate frames quickly enough as determined by frameRateMinimums.
	// For example, the game thread will start at the first setting, 16 ms per frame (62.5 fps). In order to stay at
	// that setting, it must maintain at least 58 frames per second. If it fails to perform at that rate for a 
	// sufficient number of frames (see MAX_SLOW_FRAMES below), it will move to the next setting of 20 ms per frame
	// (50 fps) and must maintain 48 fps or drop to the next slowest speed.
	long[] possibleMillisPerFrame = new long[] {16, 20, 22, 25, 33}; // approximately 62.5, 50, 45.4, 40, 30.3 fps
	double[] frameRateMinimums = new double[] {58.0, 48.0, 43.0, 38.0};
	
	int millisPerFrameIndex = 0;
	boolean frameRateLocked = false; // set to true once we have a good frame rate
	
	static long INACTIVE_FRAME_MSECS = 250; // sleep this long when field.hasActiveElements() is false
	
	public void setFieldView(FieldView value) {
		this.fieldView = value;
		this.surfaceHolder = this.fieldView.getHolder();
		this.surfaceHolder.addCallback(this);
	}
	
	public void setField(Field value) {
		this.field = value;
	}
	
	/** Starts the game thread running. Does not actually start a new game.
	 */
	public void start() {
		running = true;
		gameThread = new Thread() {
			public void run() {
				threadMain();
			}
		};
		gameThread.start();
	}
	
	/** Stops the game thread, which will pause updates to the game state and view redraws.
	 */
	public void stop() {
		running = false;
		try {
			gameThread.join();
		}
		catch(InterruptedException ex) {}
	}
	
	
	int nticks = 0;
	long fpsTime = 0;
	
	// record start times of last 10 frames, so we can adjust sleep time between frames to maintain desired rate
	LinkedList<Long> processingTimes = new LinkedList<Long>();
	int START_TIME_HISTORY_SIZE = 10;
	long MAX_SLEEP_ADJUST = 5;
	long debugSleepDiff;
	
	int slowFrames, goodFrames;
	int MAX_SLOW_FRAMES = 150; // if average time per frame is greater than the limit for this number of frames, slow down frame rate
	int MAX_GOOD_FRAMES = 500; // once we have this many frames at an acceptable rate, use that rate permanently
	
	void clearFrameRateHistory() {
		processingTimes.clear();
		slowFrames = goodFrames = 0;
	}
	
	/** Records the start time for the current frame, and determines whether the frame rate needs to be adjusted.
	 */
	void recordStartTime(long startTime) {
		// record start time and remove most distant time
		processingTimes.addLast(startTime);
		if (processingTimes.size()>START_TIME_HISTORY_SIZE) {
			long previousStartTime = processingTimes.removeFirst();
			if (!frameRateLocked) {
				// see if our time per frame is below acceptable limit
				double fps = START_TIME_HISTORY_SIZE / ((startTime - previousStartTime) / 1000.0);
				if (fps < frameRateMinimums[millisPerFrameIndex]) {
					++slowFrames;
					if (slowFrames >+ MAX_SLOW_FRAMES) {
						// we've been too slow for too long, increase frame interval
						++millisPerFrameIndex;
						if (millisPerFrameIndex >= frameRateMinimums.length) {
							// this is the slowest frame rate, no point in checking any further
							frameRateLocked = true;
						}
						clearFrameRateHistory();
					}
				}
				else {
					// Once we've had a long enough run, we're pretty sure the device can handle this frame rate.
					// Any future slowdowns are probably temporary, so don't drop frame rate from current setting. 
					++goodFrames;
					if (goodFrames >= MAX_GOOD_FRAMES) {
						frameRateLocked = true;
					}
				}
			}
		}
	}

	/** Main loop for the game thread. Repeatedly calls field.tick to advance the game simulation, redraws the field,
	 * and sleeps until it's time for the next frame. Dynamically adjusts sleep times in an attempt to maintain a
	 * consistent frame rate.
	 */
	void threadMain() {
		while (running) {
			long startTime = System.currentTimeMillis();
			this.recordStartTime(startTime);
			
			long millisPerFrame = possibleMillisPerFrame[millisPerFrameIndex];
			boolean fieldActive = true;
			long t2=0, t3=0;
			if (field!=null && canDraw) {
				try {
					synchronized(field) {
						long fieldTickMillis = (long)(millisPerFrame*field.getTargetTimeRatio());
						// if field isn't doing anything, sleep for a long time
						fieldActive = field.hasActiveElements();
						if (!fieldActive) {
							fieldTickMillis = (long)(INACTIVE_FRAME_MSECS*field.getTargetTimeRatio());
						}
						field.tick(fieldTickMillis, 4);
					}
					t2 = System.currentTimeMillis();
					drawField();
				}
				catch(Exception ex) {
					ex.printStackTrace();
				}
			}
			
			// if field is inactive, clear start time history and bail
			if (!fieldActive) {
				clearFrameRateHistory();
				try {
					Thread.sleep(INACTIVE_FRAME_MSECS);
				}
				catch(InterruptedException ignored) {}
				continue;
			}
			
			t3 = System.currentTimeMillis();
			long elapsedTime = t3 - startTime;
			long sleepTime = millisPerFrame - elapsedTime;
			// if we know when we started N frames ago, adjust so we end up closer to goal time
			if (processingTimes.size()==START_TIME_HISTORY_SIZE) {
				long goalTime = processingTimes.peek() + START_TIME_HISTORY_SIZE*millisPerFrame;
				// if timing is perfect, now+sleepTime should equal goalTime
				long diff = goalTime - (t3+sleepTime);
				// diff<0 means we're behind schedule, diff>0 means we're ahead
				// never sleep much longer than we're supposed to
				if (diff>1) diff = 1;
				else if (diff < -MAX_SLEEP_ADJUST) diff = -MAX_SLEEP_ADJUST;
				sleepTime += diff;
				
				debugSleepDiff = diff;
			}
			if (sleepTime<1) sleepTime = 1;

			try {
				Thread.sleep(sleepTime);
			}
			catch(InterruptedException ignored) {}
			
			// for debugging, show frames per second and other info
			if (FieldView.DEBUG) {
				nticks++;
				if (nticks==100) {
					nticks = 0;
					int fps = 0;
					if (fpsTime>0) {
						fps = (int)(1000.0 / ((t3-fpsTime)/1000.0));
					}
					fpsTime = t3;
					if (t2>0) fieldView.setDebugMessage(elapsedTime + ":" + debugSleepDiff + ":" + frameRateLocked + ":" + millisPerFrame + ":" + fps);
				}
			}
		}
	}
	
	/** Calls FieldView.doDraw to render the game field to the SurfaceView, and draws the view to the screen.
	 */
	void drawField() {
		Canvas c = fieldView.getHolder().lockCanvas();
		c.drawARGB(255, 0, 0, 0);
		fieldView.doDraw(c);
		fieldView.getHolder().unlockCanvasAndPost(c);
	}
	
	// SurfaceHolder.Callback methods
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		canDraw = true;
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		canDraw = false;
	}

}
