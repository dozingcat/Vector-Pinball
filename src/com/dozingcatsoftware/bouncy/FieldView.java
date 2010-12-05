package com.dozingcatsoftware.bouncy;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceView;

import com.dozingcatsoftware.bouncy.elements.FieldElement;

/** Draws the game field. Field elements are defined in world coordinates, which this view transforms to screen/pixel coordinates.
 * @author brian
 */
public class FieldView extends SurfaceView implements IFieldRenderer {

	public FieldView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	final static boolean DEBUG = false;
	
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
		// call draw() on each FieldElement, draw balls separately
		this.canvas = c;
		
		for(FieldElement element : field.getFieldElementsArray()) {
			element.draw(this);
		}

		field.drawBalls(this);
		
		if (DEBUG) {
			if (debugMessage!=null) {
				paint.setARGB(255,255,255,255);
				c.drawText(""+debugMessage, 50, 25, paint);
			}
		}
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
