package com.dozingcatsoftware.bouncy;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.dozingcatsoftware.bouncy.elements.FieldElement;
import com.dozingcatsoftware.bouncy.util.GLVertexList;
import com.dozingcatsoftware.bouncy.util.GLVertexListManager;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class GLFieldView extends GLSurfaceView implements IFieldRenderer, GLSurfaceView.Renderer {
	
	public GLFieldView(Context context, AttributeSet attrs) {
		super(context, attrs);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}
	
	FieldViewManager manager;
	
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
		lineVertexList.addVertex(manager.world2pixelX(x1), manager.world2pixelY(y1));
		lineVertexList.addVertex(manager.world2pixelX(x2), manager.world2pixelY(y2));

		float rf = r/255f;
		float gf = g/255f;
		float bf = b/255f;
		lineVertexList.addColor(rf, gf, bf);
		lineVertexList.addColor(rf, gf, bf);
	}
	
	@Override
	public void fillCircle(float cx, float cy, float radius, int r, int g, int b) {
		drawCircle(cx, cy, radius, r, g, b, GL10.GL_TRIANGLE_FAN);
	}
	
	@Override
	public void frameCircle(float cx, float cy, float radius, int r, int g, int b) {
		drawCircle(cx, cy, radius, r, g, b, GL10.GL_LINE_LOOP);
	}
	
	void drawCircle(float cx, float cy, float radius, int r, int g, int b, int mode) {
		GLVertexList circleVertexList = vertexListManager.addVertexListForMode(mode);
		circleVertexList.addColor(r/255f, g/255f, b/255f);

		for(int i=0; i<SIN_VALUES.length; i++) {
			float x = cx + radius*COS_VALUES[i];
			float y = cy + radius*SIN_VALUES[i];
			circleVertexList.addVertex(manager.world2pixelX(x), manager.world2pixelY(y));
		}
	}
	
	Object renderLock = new Object();
	boolean renderDone;

	@Override
	public void onDrawFrame(GL10 gl) {
		Field field = manager.getField();
		if (field==null) return;
		synchronized(field) {
			startGLElements(gl);
			
			for(FieldElement element : field.getFieldElementsArray()) {
				element.draw(this);
			}

			field.drawBalls(this);

			endGLElements(gl);
		}

		synchronized(renderLock) {
			renderDone = true;
			renderLock.notify();
		}
	}
	
	/* requestRender() returns immediately and schedules onDrawFrame for execution on a separate thread. In this case,
	 * we want to block until onDrawFrame returns so that the simulation thread in FieldDriver stays in sync with the
	 * rendering thread. (Without the locks, FieldDriver registers 60fps even if the actual drawing is much slower).
	 */
	@Override
	public void doDraw() {
		synchronized(renderLock) {
			renderDone = false;
		}
		
		this.requestRender();
		
		synchronized(renderLock) {
			while (!renderDone) {
				try {
					renderLock.wait();
				}
				catch(InterruptedException ex) {}
			}
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
}
