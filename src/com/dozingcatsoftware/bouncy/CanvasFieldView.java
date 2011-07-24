package com.dozingcatsoftware.bouncy;

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
public class CanvasFieldView extends SurfaceView implements IFieldRenderer {

	public CanvasFieldView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	FieldViewManager manager;
	
	Paint paint = new Paint(); {paint.setAntiAlias(true);}
	Paint textPaint = new Paint(); {textPaint.setARGB(255, 255, 255, 255);}
	Canvas canvas;

	public void setManager(FieldViewManager value) {
		this.manager = value;
	}
	

    /** Called when the view is touched. Activates flippers, starts a new game if one is not in progress, and
     * launches a ball if one is not in play.
     */
	@Override
    public boolean onTouchEvent(MotionEvent event) {
		return manager.handleTouchEvent(event);
    }
	

	/** Main draw method, called from FieldDriver's game thread. Calls each FieldElement's draw() method passing
	 * itself as the IFieldRenderer implementation.
	 */
	public void doDraw() {
		Canvas c = this.getHolder().lockCanvas();
		c.drawARGB(255, 0, 0, 0);
		paint.setStrokeWidth(manager.highQuality ? 2 : 0);
		// call draw() on each FieldElement, draw balls separately
		this.canvas = c;
		
		for(FieldElement element : manager.getField().getFieldElementsArray()) {
			element.draw(this);
		}

		manager.getField().drawBalls(this);
		
		if (manager.showFPS()) {
			if (manager.getDebugMessage()!=null) {
				c.drawText(""+manager.getDebugMessage(), 10, 10, textPaint);
			}
		}
		this.getHolder().unlockCanvasAndPost(c);
	}
	
	
	// Implementation of IFieldRenderer drawing methods that FieldElement classes can call. Assumes cacheScaleAndOffsets has been called.
	@Override
	public void drawLine(float x1, float y1, float x2, float y2, int r, int g, int b) {
		this.paint.setARGB(255, r, g, b);
		this.canvas.drawLine(manager.world2pixelX(x1), manager.world2pixelY(y1), manager.world2pixelX(x2), manager.world2pixelY(y2), this.paint);
	}
	
	@Override
	public void fillCircle(float cx, float cy, float radius, int r, int g, int b) {
		drawCircle(cx, cy, radius, r, g, b, Paint.Style.FILL);
	}
	
	@Override
	public void frameCircle(float cx, float cy, float radius, int r, int g, int b) {
		drawCircle(cx, cy, radius, r, g, b, Paint.Style.STROKE);
	}

	void drawCircle(float cx, float cy, float radius, int r, int g, int b, Paint.Style style) {
		this.paint.setARGB(255, r, g, b);
		this.paint.setStyle(style);
		float rad = radius * manager.getCachedScale();
		this.canvas.drawCircle(manager.world2pixelX(cx), manager.world2pixelY(cy), rad, paint);
	}
	
}
