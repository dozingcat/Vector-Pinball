package com.dozingcatsoftware.bouncy;

import android.opengl.GLSurfaceView;
import android.opengl.GLU;

import com.dozingcatsoftware.bouncy.util.GLVertexList;
import com.dozingcatsoftware.bouncy.util.GLVertexListManager;
import com.dozingcatsoftware.bouncy.util.TrigLookupTable;
import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.model.Field;
import com.dozingcatsoftware.vectorpinball.model.IFieldRenderer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GL10Renderer implements IFieldRenderer.FloatOnlyRenderer, GLSurfaceView.Renderer {
    private final GLFieldView glView;
    private GLVertexListManager vertexListManager = new GLVertexListManager();
    private GLVertexList lineVertexList;

    private FieldViewManager manager;

    public GL10Renderer(GLFieldView view) {
        this.glView = view;
        view.setRenderer(this);
        view.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void setManager(FieldViewManager value) {
        this.manager = value;
        this.glView.setManager(value);
    }

    // Lookup tables for sin/cos, used to draw circles by approximating with polygons.
    // Larger circles are drawn with more points.
    static final TrigLookupTable trigTable = new TrigLookupTable(8, 20, 60);

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

        gl.glLineWidth(manager.getLineWidth());

        vertexListManager.render(gl);
    }

    private static void addColorToVertexList(GLVertexList vl, int color) {
        vl.addColor(
                Color.getRed(color) / 255f, Color.getGreen(color) / 255f,
                Color.getBlue(color) / 255f, Color.getAlpha(color) / 255f);
    }

    // Implementation of IFieldRenderer drawing methods that FieldElement classes can call.
    // Assumes cacheScaleAndOffsets has been called.
    @Override public void drawLine(float x1, float y1, float x2, float y2, int color) {
        lineVertexList.addVertex(manager.world2pixelX(x1), manager.world2pixelY(y1));
        lineVertexList.addVertex(manager.world2pixelX(x2), manager.world2pixelY(y2));
        addColorToVertexList(lineVertexList, color);
        addColorToVertexList(lineVertexList, color);
    }

    @Override public void drawLinePath(float[] xEndpoints, float[] yEndpoints, int color) {
        GLVertexList pathList = vertexListManager.addVertexListForMode(GL10.GL_LINE_STRIP);
        addColorToVertexList(pathList, color);
        for (int i = 0; i < xEndpoints.length; i++) {
            pathList.addVertex(
                    manager.world2pixelX(xEndpoints[i]), manager.world2pixelY(yEndpoints[i]));
        }
    }

    @Override public void fillCircle(float cx, float cy, float radius, int color) {
        drawCircle(cx, cy, radius, color, GL10.GL_TRIANGLE_FAN);
    }

    @Override public void frameCircle(float cx, float cy, float radius, int color) {
        drawCircle(cx, cy, radius, color, GL10.GL_LINE_LOOP);
    }

    void drawCircle(float cx, float cy, float radius, int color, int mode) {
        GLVertexList circleVertexList = vertexListManager.addVertexListForMode(mode);
        addColorToVertexList(circleVertexList, color);

        int radPixels = (int) Math.ceil(manager.world2pixelX(radius) - manager.world2pixelX(0));
        // Approximate circle with polygon. Use 8 or 20 sides for <60 pixel radius.
        int minPolySides = (radPixels < 60) ? Math.min(radPixels, 20) : radPixels;
        TrigLookupTable.SinCosValues sinCosValues = trigTable.valuesWithSizeAtLeast(minPolySides);
        int actualPolySides = sinCosValues.size();
        for (int i = 0; i < actualPolySides; i++) {
            float x = cx + radius * sinCosValues.cosAtIndex(i);
            float y = cy + radius * sinCosValues.sinAtIndex(i);
            circleVertexList.addVertex(manager.world2pixelX(x), manager.world2pixelY(y));
        }
    }

    // OpenGL can only draw arcs by drawing individual line segments. We could determine the line
    // segment endpoints in drawArc, but currently the only caller is WallArcElement which already
    // has the endpoints, so it's better to not implement it and have clients call drawLinePath.

    final Object renderLock = new Object();
    boolean renderDone;

    @Override public void onDrawFrame(GL10 gl) {
        Field field = manager.getField();
        if (field == null) {
            return;
        }
        startGLElements(gl);
        synchronized (field) {
            field.draw(this);
        }
        endGLElements(gl);
        synchronized (renderLock) {
            renderDone = true;
            renderLock.notify();
        }
    }

    /* requestRender() returns immediately and schedules onDrawFrame for execution on a separate
     * thread. In this case, we want to block until onDrawFrame returns so that the simulation
     * thread in FieldDriver stays in sync with the rendering thread. (Without the locks,
     * FieldDriver registers 60fps even if the actual drawing is much slower).
     */
    @Override public void doDraw() {
        synchronized (renderLock) {
            renderDone = false;
        }

        this.glView.requestRender();

        synchronized (renderLock) {
            while (!renderDone) {
                try {
                    renderLock.wait();
                }
                catch (InterruptedException ex) {
                }
            }
        }
    }

    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);
    }

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
        gl.glShadeModel(GL10.GL_FLAT);
        gl.glDisable(GL10.GL_DEPTH_TEST);

        // Alpha support.
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

        // This is supposed to enable antialiased lines, but it seems to both not do that and
        // to make the lines narrow, ignoring the glLineWidth call above.
        // gl.glEnable(GL10.GL_LINE_SMOOTH);

        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();

        GLU.gluOrtho2D(gl, 0, getWidth(), getHeight(), 0);
    }

    @Override public int getWidth() {
        return this.glView.getWidth();
    }

    @Override public int getHeight() {
        return this.glView.getHeight();
    }
}
