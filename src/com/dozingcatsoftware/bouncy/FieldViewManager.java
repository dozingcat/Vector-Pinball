package com.dozingcatsoftware.bouncy;

import java.lang.reflect.Method;
import java.util.List;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/** This class handles the common functionality for Canvas and OpenGL-based views, including mapping world coordinates
 * to pixels and handling touch events.
 */

public class FieldViewManager implements SurfaceHolder.Callback {
	
	IFieldRenderer view;
	boolean canDraw;
	Runnable startGameAction;

	public void setFieldView(IFieldRenderer view) {
		if (this.view!=view) {
			this.view = view;
			view.setManager(this);
			if (view instanceof SurfaceView) {
				canDraw = false;
				((SurfaceView)view).getHolder().addCallback(this);
			}
			else {
				canDraw = true;
			}
		}
	}
	
	public boolean canDraw() {
		return canDraw;
	}

	boolean showFPS;
	boolean highQuality;
	boolean independentFlippers;
	
	Field field;
	
	float zoom = 1.0f;
	float maxZoom = 1.0f;
	
	// x/y offsets and scale are cached at the beginning of draw(), to avoid repeated calls as elements are drawn.
	// They shouldn't change between calls to draw() either, but better safe than sorry. 
	float cachedXOffset, cachedYOffset, cachedScale, cachedHeight;
	
	String debugMessage;
	double fps;
	
	public void setField(Field value) {
		field = value;
	}
	public Field getField() {
		return field;
	}
	
	public void setDebugMessage(String value) {
		debugMessage = value;
	}
	public String getDebugMessage() {
		return debugMessage;
	}
	
	public void setShowFPS(boolean value) {
		showFPS = value;
	}
	public boolean showFPS() {
		return showFPS;
	}
	
	public void setIndependentFlippers(boolean value) {
		independentFlippers = value;
	}
	
	public void setHighQuality(boolean value) {
		highQuality = value;
	}
	public boolean isHighQuality() {
		return highQuality;
	}
	
	public void setStartGameAction(Runnable action) {
		startGameAction = action;
	}
	
	float getScale() {
		float xs = view.getWidth() / field.getWidth();
		float ys = view.getHeight() / field.getHeight();
		return Math.min(xs, ys) * this.zoom;
	}
	
	float getCachedScale() {
		return cachedScale;
	}

	// Sets maxZoom ivar, zoom will still be 1 when game is not in progress.
	public void setZoom(float value) {
		maxZoom = value;
	}
	
	/** Saves scale and x and y offsets for use by world2pixel methods, avoiding repeated method calls and math operations. */
	public void cacheScaleAndOffsets() {
		zoom = maxZoom;
		if (zoom<=1.0f || !field.getGameState().isGameInProgress()) {
			cachedXOffset = 0;
			cachedYOffset = 0;
			zoom = 1.0f;
		}
		else {
			List<Body> balls = field.getBalls();
			float x=-1, y=-1;
			if (balls.size()==1) {
				Body b = balls.get(0);
				x = b.getPosition().x;
				y = b.getPosition().y;
			}
			else if (balls.size()==0) {
				// use launch position
				List<Number> position = field.layout.getLaunchPosition();
				x = position.get(0).floatValue();
				y = position.get(1).floatValue();
			}
			else {
				// for multiple balls, take position with smallest y
				for(Body b : balls) {
					Vector2 pos = b.getPosition();
					if (y<0 || pos.y<y) {
						x = pos.x;
						y = pos.y;
					}
				}
			}
			float maxOffsetRatio = 1.0f - 1.0f/zoom;
			cachedXOffset = x - field.getWidth()/(2.0f * zoom);
			if (cachedXOffset<0) cachedXOffset = 0;
			if (cachedXOffset>field.getWidth()*maxOffsetRatio) cachedXOffset = field.getWidth() * maxOffsetRatio;
			cachedYOffset = y - field.getHeight()/(2.0f * zoom);
			if (cachedYOffset<0) cachedYOffset = 0;
			if (cachedYOffset>field.getHeight()*maxOffsetRatio) cachedYOffset = field.getHeight() * maxOffsetRatio;
		}

		cachedScale = getScale();
		cachedHeight = view.getHeight();
	}
	
	// world2pixel methods assume cacheScaleAndOffsets has been called previously
	/** Converts an x coordinate from world coordinates to the view's pixel coordinates. 
	 */
	public float world2pixelX(float x) {
		return (x-cachedXOffset) * cachedScale;
		//return x * getScale() + getXOffset();
	}
	
	/** Converts an x coordinate from world coordinates to the view's pixel coordinates. In world coordinates, positive y is up,
	 * in pixel coordinates, positive y is down. 
	 */
	public float world2pixelY(float y) {
		return cachedHeight - ((y-cachedYOffset) * cachedScale);
		//return getHeight() - (y * getScale()) - getYOffset();
	}
	
	// for compatibility with Android 1.6, use reflection for multitouch features
	boolean hasMultitouch;
	Method getPointerCountMethod;
	Method getXMethod;
	int MOTIONEVENT_ACTION_MASK = 0xffffffff; // defaults to no-op AND mask
	int MOTIONEVENT_ACTION_POINTER_UP;
	int MOTIONEVENT_ACTION_POINTER_INDEX_MASK;
	int MOTIONEVENT_ACTION_POINTER_INDEX_SHIFT;
	
	{
		try {
			getPointerCountMethod = MotionEvent.class.getMethod("getPointerCount");
			getXMethod = MotionEvent.class.getMethod("getX", int.class);
			MOTIONEVENT_ACTION_MASK = MotionEvent.class.getField("ACTION_MASK").getInt(null);
			MOTIONEVENT_ACTION_POINTER_UP = MotionEvent.class.getField("ACTION_POINTER_UP").getInt(null);
			MOTIONEVENT_ACTION_POINTER_INDEX_MASK = MotionEvent.class.getField("ACTION_POINTER_INDEX_MASK").getInt(null);
			MOTIONEVENT_ACTION_POINTER_INDEX_SHIFT = MotionEvent.class.getField("ACTION_POINTER_INDEX_SHIFT").getInt(null);
			hasMultitouch = true;
		}
		catch(Exception ex) {
			hasMultitouch = false;
		}
	}
	

    /** Called when the view is touched. Activates flippers, starts a new game if one is not in progress, and
     * launches a ball if one is not in play.
     */
    public boolean handleTouchEvent(MotionEvent event) {
		int actionType = event.getAction() & MOTIONEVENT_ACTION_MASK;
    	synchronized(field) {
    		if (!field.getGameState().isGameInProgress()) {
    			if (startGameAction!=null) {
    				startGameAction.run();
    				return true;
    			}
    		}
        	if (actionType==MotionEvent.ACTION_DOWN) {
            	// remove "dead" balls and launch if none already in play
        		field.handleDeadBalls();
        		if (field.getBalls().size()==0) field.launchBall();
        	}
        	// activate or deactivate flippers
        	if (this.independentFlippers && this.hasMultitouch) {
        		try {
        			// determine whether to activate left and/or right flippers (using reflection for Android 2.2 multitouch APIs)
            		boolean left=false, right=false;
            		if (actionType!=MotionEvent.ACTION_UP) {
            			int npointers = (Integer)getPointerCountMethod.invoke(event);
            			// if pointer was lifted (ACTION_POINTER_UP), get its index so we don't count it as pressed
            			int liftedPointerIndex = -1;
            			if (actionType==MOTIONEVENT_ACTION_POINTER_UP){
            				liftedPointerIndex = (event.getAction() & MOTIONEVENT_ACTION_POINTER_INDEX_MASK) >> MOTIONEVENT_ACTION_POINTER_INDEX_SHIFT;
            				//this.setDebugMessage("Lifted " + liftedPointerIndex);
            			}
            			//this.setDebugMessage("Pointers: " + npointers);
            			float halfwidth = view.getWidth() / 2;
            			for(int i=0; i<npointers; i++) {
            				if (i!=liftedPointerIndex) {
                				float touchx = (Float)getXMethod.invoke(event, i);
                				if (touchx < halfwidth) left = true;
                				else right = true;
            				}
            			}
            		}
            		field.setLeftFlippersEngaged(left);
            		field.setRightFlippersEngaged(right);
        		}
        		catch(Exception ignored) {}
        	}
        	else {
            	boolean flipperState = !(event.getAction()==MotionEvent.ACTION_UP);
            	field.setAllFlippersEngaged(flipperState);
        	}
    	}
    	return true;
    }
    
    public void draw() {
    	cacheScaleAndOffsets();
    	view.doDraw();
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
