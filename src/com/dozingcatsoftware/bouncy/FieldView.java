package com.dozingcatsoftware.bouncy;

import java.lang.reflect.Method;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;

import com.dozingcatsoftware.bouncy.elements.FieldElement;

/** Draws the game field. Field elements are defined in world coordinates, which this view transforms to screen/pixel coordinates.
 * @author brian
 */
public class FieldView extends SurfaceView implements IFieldRenderer {

	public FieldView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	boolean showFPS;
	boolean highQuality;
	boolean independentFlippers;
	
	Field field;
	
	Paint paint = new Paint(); {paint.setAntiAlias(true);}
	Paint textPaint = new Paint(); {textPaint.setARGB(255, 255, 255, 255);}
	float zoom = 1.0f;
	Canvas canvas;
	
	// x/y offsets and scale are cached at the beginning of draw(), to avoid repeated calls as elements are drawn.
	// They shouldn't change between calls to draw() either, but better safe than sorry. 
	float cachedXOffset, cachedYOffset, cachedScale, cachedHeight;
	
	String debugMessage;
	
	public void setField(Field value) {
		field = value;
	}
	
	public void setDebugMessage(String value) {
		debugMessage = value;
	}
	
	public void setShowFPS(boolean value) {
		showFPS = value;
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
	
	float getScale() {
		float xs = this.getWidth() / field.getWidth();
		float ys = this.getHeight() / field.getHeight();
		return Math.min(xs, ys) * this.zoom;
	}
	
	float getXOffset() {
		float scale = this.getScale();
		return (this.getWidth() - scale*field.getWidth()) / 2.0f;
	}
	float getYOffset() {
		float scale = this.getScale();
		return (this.getHeight() - scale*field.getHeight()) / 2.0f;
	}
	
	public float getZoom() {
		return zoom;
	}
	public void setZoom(float value) {
		zoom = value;
	}
	
	/** Saves scale and x and y offsets for use by world2pixel methods, avoiding repeated method calls and math operations. */
	void cacheScaleAndOffsets() {
		cachedXOffset = getXOffset();
		cachedYOffset = getYOffset();
		cachedScale = getScale();
		cachedHeight = getHeight();
	}
	
	// world2pixel methods assume cacheScaleAndOffsets has been called previously
	/** Converts an x coordinate from world coordinates to the view's pixel coordinates. 
	 */
	float world2pixelX(float x) {
		return x * cachedScale + cachedXOffset;
		//return x * getScale() + getXOffset();
	}
	
	/** Converts an x coordinate from world coordinates to the view's pixel coordinates. In world coordinates, positive y is up,
	 * in pixel coordinates, positive y is down. 
	 */
	float world2pixelY(float y) {
		return cachedHeight - (y * cachedScale) - cachedYOffset;
		//return getHeight() - (y * getScale()) - getYOffset();
	}
	
	/** Main draw method, called from FieldDriver's game thread. Calls each FieldElement's draw() method passing
	 * itself as the IFieldRenderer implementation.
	 */
	public void doDraw(Canvas c) {
		cacheScaleAndOffsets();
		paint.setStrokeWidth(this.highQuality ? 2 : 0);
		// call draw() on each FieldElement, draw balls separately
		this.canvas = c;
		
		for(FieldElement element : field.getFieldElementsArray()) {
			element.draw(this);
		}

		field.drawBalls(this);
		
		if (this.showFPS) {
			if (debugMessage!=null) {
				c.drawText(""+debugMessage, 10, 10, textPaint);
			}
		}
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
	@Override
    public boolean onTouchEvent(MotionEvent event) {
		int actionType = event.getAction() & MOTIONEVENT_ACTION_MASK;
    	synchronized(field) {
        	if (actionType==MotionEvent.ACTION_DOWN) {
        		// start game if not in progress
        		if (!field.getGameState().isGameInProgress()) {
        			field.resetForLevel(this.getContext(), 1);
        			field.startGame();
        		}
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
            			float halfwidth = this.getWidth() / 2;
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

	// Implementation of IFieldRenderer drawing methods that FieldElement classes can call. Assumes cacheScaleAndOffsets has been called.
	@Override
	public void drawLine(float x1, float y1, float x2, float y2, int r, int g, int b) {
		this.paint.setARGB(255, r, g, b);
		this.canvas.drawLine(world2pixelX(x1), world2pixelY(y1), world2pixelX(x2), world2pixelY(y2), this.paint);
	}
	
	@Override
	public void fillCircle(float cx, float cy, float radius, int r, int g, int b) {
		this.paint.setARGB(255, r, g, b);
		this.paint.setStyle(Paint.Style.FILL);
		float rad = radius * cachedScale;
		this.canvas.drawCircle(world2pixelX(cx), world2pixelY(cy), rad, paint);
	}
	
	@Override
	public void frameCircle(float cx, float cy, float radius, int r, int g, int b) {
		this.paint.setARGB(255, r, g, b);
		this.paint.setStyle(Paint.Style.STROKE);
		float rad = radius * cachedScale;
		this.canvas.drawCircle(world2pixelX(cx), world2pixelY(cy), rad, paint);
	}
	
}
