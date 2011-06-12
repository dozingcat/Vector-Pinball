package com.dozingcatsoftware.bouncy;

import com.dozingcatsoftware.bouncy.util.FrameRateManager;

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
	
	FrameRateManager frameRateManager = new FrameRateManager(new double[] {60, 50, 45, 40, 30}, new double[] {57, 48, 43, 38});
	double averageFPS;
	
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
	

	/** Main loop for the game thread. Repeatedly calls field.tick to advance the game simulation, redraws the field,
	 * and sleeps until it's time for the next frame. Dynamically adjusts sleep times in an attempt to maintain a
	 * consistent frame rate.
	 */
	void threadMain() {
		while (running) {
			frameRateManager.frameStarted();
			boolean fieldActive = true;
			if (field!=null && canDraw) {
				try {
					synchronized(field) {
						long nanosPerFrame = (long)(1000000000L / frameRateManager.targetFramesPerSecond());
						long fieldTickNanos = (long)(nanosPerFrame*field.getTargetTimeRatio());
						// if field isn't doing anything, sleep for a long time
						fieldActive = field.hasActiveElements();
						if (!fieldActive) {
							fieldTickNanos = (long)(INACTIVE_FRAME_MSECS*1000000*field.getTargetTimeRatio());
						}
						field.tick(fieldTickNanos, 4);
					}
					drawField();
				}
				catch(Exception ex) {
					ex.printStackTrace();
				}
			}
			
			// if field is inactive, clear start time history and bail
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
			
			// for debugging, show frames per second and other info
			if (frameRateManager.getTotalFrames() % 100 == 0) {
				//fieldView.setDebugMessage(frameRateManager.fpsDebugInfo());
				//setAverageFPS(frameRateManager.currentFramesPerSecond());
				setAverageFPS(fieldView.frManager.currentFramesPerSecond());
			}
		}
	}
	
	/** Calls FieldView.doDraw to render the game field to the SurfaceView, and draws the view to the screen.
	 */
	void drawField() {
		fieldView.requestRender();
		/*
		Canvas c = fieldView.getHolder().lockCanvas();
		c.drawARGB(255, 0, 0, 0);
		fieldView.doDraw(c);
		fieldView.getHolder().unlockCanvasAndPost(c);
		*/
	}
	
	/** Resets the frame rate and forgets any locked rate, called when rendering quality is changed.
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
