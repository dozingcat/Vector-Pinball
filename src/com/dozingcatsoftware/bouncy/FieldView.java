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
public class FieldView extends SurfaceView implements IFieldRenderer {

	public FieldView(Context context, AttributeSet attrs) {
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
	
	// GL implementation start, this is apparently very slow on some devices (HTC Desire?)
	/*
	GLVertexListManager vertexListManager = new GLVertexListManager();
	GLVertexList lineVertexList;
	
	
	static float[] SIN_VALUES = new float[20];
	static float[] COS_VALUES = new float[20];
	static {
		for(int i=0; i<SIN_VALUES.length; i++) {
			double angle = 2*Math.PI * i / SIN_VALUES.length;
			SIN_VALUES[i] = (float)Math.sin(angle);
			COS_VALUES[i] = (float)Math.cos(angle);
		}
	}
	
	void startGLElements(GL10 gl) {
		vertexListManager.begin();
		lineVertexList = vertexListManager.addVertexListForMode(GL10.GL_LINES);
	}
	
	void endGLElements(GL10 gl) {
		vertexListManager.end();
		
        gl.glEnable(GL10.GL_DITHER);        
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glMatrixMode(GL10.GL_MODELVIEW); 
        gl.glLoadIdentity();
        
        gl.glLineWidth(2);
        
        vertexListManager.render(gl);
	}

	// Implementation of IFieldRenderer drawing methods that FieldElement classes can call. Assumes cacheScaleAndOffsets has been called.
	@Override
	public void drawLine(float x1, float y1, float x2, float y2, int r, int g, int b) {
		drawLineGL(x1, y1, x2, y2, r, g, b);
	}
	
	void drawLineGL(float x1, float y1, float x2, float y2, int r, int g, int b) {
		lineVertexList.addVertex(world2pixelX(x1), world2pixelY(y1));
		lineVertexList.addVertex(world2pixelX(x2), world2pixelY(y2));

		float rf = r/255f;
		float gf = g/255f;
		float bf = b/255f;
		lineVertexList.addColor(rf, gf, bf);
		lineVertexList.addColor(rf, gf, bf);
	}
	
	@Override
	public void fillCircle(float cx, float cy, float radius, int r, int g, int b) {
		fillCircleGL(cx, cy, radius, r, g, b);
	}
	
	void fillCircleGL(float cx, float cy, float radius, int r, int g, int b) {
		GLVertexList circleVertexList = vertexListManager.addVertexListForMode(GL10.GL_TRIANGLE_FAN);
		circleVertexList.addColor(r/255f, g/255f, b/255f);

		for(int i=0; i<SIN_VALUES.length; i++) {
			float x = cx + radius*COS_VALUES[i];
			float y = cy + radius*SIN_VALUES[i];
			circleVertexList.addVertex(world2pixelX(x), world2pixelY(y));
		}
	}
	
	@Override
	public void frameCircle(float cx, float cy, float radius, int r, int g, int b) {
		frameCircleGL(cx, cy, radius, r, g, b);
	}
	
	void frameCircleGL(float cx, float cy, float radius, int r, int g, int b) {
		GLVertexList circleVertexList = vertexListManager.addVertexListForMode(GL10.GL_LINE_LOOP);
		circleVertexList.addColor(r/255f, g/255f, b/255f);

		for(int i=0; i<SIN_VALUES.length; i++) {
			float x = cx + radius*COS_VALUES[i];
			float y = cy + radius*SIN_VALUES[i];
			circleVertexList.addVertex(world2pixelX(x), world2pixelY(y));
		}
	}

	public com.dozingcatsoftware.bouncy.util.FrameRateManager frManager = new com.dozingcatsoftware.bouncy.util.FrameRateManager(0);
	
	@Override
	public void onDrawFrame(GL10 gl) {
		if (field==null) return;
		synchronized(field) {
			frManager.frameStarted();
			cacheScaleAndOffsets();

			startGLElements(gl);
			
			for(FieldElement element : field.getFieldElementsArray()) {
				element.draw(this);
			}

			field.drawBalls(this);

			endGLElements(gl);
		}
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		gl.glViewport(0, 0, width, height);
    }

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
	    gl.glClearColor(0.0f, 0.0f, 0.0f, 1);
	    gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
	    gl.glShadeModel(GL10.GL_FLAT);
	    gl.glDisable(GL10.GL_DEPTH_TEST);
	    //gl.glEnable(GL10.GL_BLEND);
	    //gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA); 

	    gl.glMatrixMode(GL10.GL_PROJECTION);
	    gl.glLoadIdentity();

	    GLU.gluOrtho2D(gl, 0, getWidth(), getHeight(), 0);
	    //GLU.gluPerspective(gl, 45.0f, (float)getWidth() / (float)getHeight(), 0.1f, 100f);
	}
	*/

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
		this.paint.setARGB(255, r, g, b);
		this.paint.setStyle(Paint.Style.FILL);
		float rad = radius * manager.getCachedScale();
		this.canvas.drawCircle(manager.world2pixelX(cx), manager.world2pixelY(cy), rad, paint);
	}
	
	@Override
	public void frameCircle(float cx, float cy, float radius, int r, int g, int b) {
		this.paint.setARGB(255, r, g, b);
		this.paint.setStyle(Paint.Style.STROKE);
		float rad = radius * manager.getCachedScale();
		this.canvas.drawCircle(manager.world2pixelX(cx), manager.world2pixelY(cy), rad, paint);
	}

}
