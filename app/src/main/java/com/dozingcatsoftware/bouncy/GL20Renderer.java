package com.dozingcatsoftware.bouncy;

import android.annotation.TargetApi;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Build;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

@TargetApi(Build.VERSION_CODES.FROYO)
public class GL20Renderer implements IFieldRenderer, GLSurfaceView.Renderer {
    static final double TAU = 2 * Math.PI;

    private final GLFieldView glView;
    private final Function<String, String> shaderLookupFn;

    private FieldViewManager fvManager;

    private Integer circleProgramId = null;
    private int circlePositionHandle;
    private int circleColorHandle;
    private int circleCenterHandle;
    private int circleRadiusHandle;
    private int circleFilledHandle;
    private int circleLineWidthHandle;

    private Integer lineProgramId = null;
    private int linePositionHandle;
    private int lineColorHandle;

    public GL20Renderer(GLFieldView view, Function<String, String> shaderLookupFn) {
        this.glView = view;
        view.getHolder().setFormat(PixelFormat.RGB_565);
        view.getHolder().setFormat(PixelFormat.TRANSPARENT);
        view.setEGLConfigChooser(8,8,8,8,16,0);
        view.setEGLContextClientVersion(2);
        view.setRenderer(this);
        view.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        this.shaderLookupFn = shaderLookupFn;
    }

    private int loadShader(int type, String shaderPath) {
        String src = this.shaderLookupFn.apply(shaderPath);
        int shaderId = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shaderId, src);
        GLES20.glCompileShader(shaderId);
        return shaderId;
    }

    private int createProgram(String vertexShaderPath, String fragmentShaderPath) {
        int programId = GLES20.glCreateProgram();
        GLES20.glAttachShader(programId, loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderPath));
        GLES20.glAttachShader(programId, loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderPath));
        GLES20.glLinkProgram(programId);
        return programId;
    }

    private void initShaders() {
        circleProgramId = createProgram("shaders/base_vertex.vert", "shaders/circle.frag");
        circlePositionHandle = GLES20.glGetAttribLocation(circleProgramId, "position");
        circleColorHandle = GLES20.glGetUniformLocation(circleProgramId, "color");
        circleCenterHandle = GLES20.glGetUniformLocation(circleProgramId, "center");
        circleRadiusHandle = GLES20.glGetUniformLocation(circleProgramId, "radius");
        circleFilledHandle = GLES20.glGetUniformLocation(circleProgramId, "filled");
        circleLineWidthHandle = GLES20.glGetUniformLocation(circleProgramId, "lineWidth");

        lineProgramId = createProgram("shaders/base_vertex.vert", "shaders/line.frag");
        linePositionHandle = GLES20.glGetAttribLocation(lineProgramId, "position");
        lineColorHandle = GLES20.glGetUniformLocation(lineProgramId, "color");
    }

    private final float[] vPMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];

    private static class Circle {
        float cx;
        float cy;
        float radius;
        float[] color = new float[4];
        boolean filled;
    }

    private static class Line {
        float x1;
        float y1;
        float x2;
        float y2;
        float[] color = new float[4];
    }

    static void setRgba(float[] rgba, int color) {
        rgba[0] = Color.getRed(color) / 255f;
        rgba[1] = Color.getGreen(color) / 255f;
        rgba[2] = Color.getBlue(color) / 255f;
        rgba[3] = Color.getAlpha(color) / 255f;
    }

    private List<Circle> circles = new ArrayList<>();
    private List<Line> lines = new ArrayList<>();


    private void startDraw() {
        circles.clear();
        lines.clear();
    }

    private void endDraw() {
        drawTest();
    }

    // The OpenGL coordinate system has a visible region from -1 to +1. The projection matrix
    // applies a scaling factor so that +1 and -1 are at the edges of the largest dimension
    // (most likely height), while the smaller dimension has 0 and the midpoint and the visible
    // edge will be less than 1. For example, if the height is 800 and the width is 600, the X axis
    // has 3/4 the visible range of the Y axis, and the visible range of X coordinates will be
    // -0.75 to +0.75.
    float world2glX(float x) {
        float scale = Math.max(getWidth(), getHeight());
        float wx = fvManager.world2pixelX(x);
        float offset = 1 - getWidth() / scale;
        float glx = ((2 * wx) / scale - 1) + offset;
        return glx;
    }

    float world2glY(float y) {
        float scale = Math.max(getWidth(), getHeight());
        float wy = fvManager.world2pixelY(y);
        float offset = 1 - getHeight() / scale;
        float gly = -(((2 * wy) / scale - 1) + offset);
        return gly;
    }

    private float worldToGLPixelX(float x) {
        return fvManager.world2pixelX(x);
    }

    private float worldToGLPixelY(float y) {
        // FieldViewManager assumes positive Y is down, but here it's up.
        return this.getHeight() - fvManager.world2pixelY(y);
    }

    private void drawCircles() {
        FloatBuffer vertexBuffer;

        // number of coordinates per vertex in this array
        final int COORDS_PER_VERTEX = 3;
        float coords[] = new float[18];
        float center[] = new float[2];

        int numVertices = coords.length / COORDS_PER_VERTEX;

        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                coords.length * 4);
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());

        // create a floating point buffer from the ByteBuffer
        vertexBuffer = bb.asFloatBuffer();

        GLES20.glUseProgram(circleProgramId);

        for (Circle c : circles) {
            float glx = world2glX(c.cx);
            float gly = world2glY(c.cy);
            float glrad = world2glX(c.radius) - world2glX(0);

            coords[0] = glx - glrad;
            coords[1] = gly - glrad;
            coords[3] = glx + glrad;
            coords[4] = gly - glrad;
            coords[6] = glx - glrad;
            coords[7] = gly + glrad;
            coords[9] = glx - glrad;
            coords[10] = gly + glrad;
            coords[12] = glx + glrad;
            coords[13] = gly - glrad;
            coords[15] = glx + glrad;
            coords[16] = gly + glrad;

            vertexBuffer.put(coords);
            vertexBuffer.position(0);

            float radiusInPixels = worldToGLPixelX(c.radius) - worldToGLPixelX(0);

            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(circlePositionHandle);

            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(circlePositionHandle, COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    12, vertexBuffer);

            // get handle to fragment shader's vColor member
            // colorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

            // Set position/radius/color.
            center[0] = worldToGLPixelX(c.cx);
            center[1] = worldToGLPixelY(c.cy);
            GLES20.glUniform2fv(circleCenterHandle, 1, center, 0);
            GLES20.glUniform1f(circleRadiusHandle, radiusInPixels);
            GLES20.glUniform1f(circleLineWidthHandle, fvManager.getLineWidth());
            GLES20.glUniform1i(circleFilledHandle, c.filled ? 1 : 0);
            GLES20.glUniform4fv(circleColorHandle, 1, c.color, 0);

            // get handle to shape's transformation matrix
            int vPMatrixHandle = GLES20.glGetUniformLocation(circleProgramId, "uMVPMatrix");

            // Pass the projection and view transformation to the shader
            GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, vPMatrix, 0);

            // Draw the triangle
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numVertices);

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(circlePositionHandle);
        }
    }

    private void drawLines() {
        FloatBuffer vertexBuffer;

        // number of coordinates per vertex in this array
        final int COORDS_PER_VERTEX = 3;
        float coords[] = new float[18];
        float start[] = new float[2];
        float end[] = new float[2];
        int numVertices = coords.length / COORDS_PER_VERTEX;

        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                coords.length * 4);
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());

        // create a floating point buffer from the ByteBuffer
        vertexBuffer = bb.asFloatBuffer();

        GLES20.glUseProgram(lineProgramId);
        int vPMatrixHandle = GLES20.glGetUniformLocation(lineProgramId, "uMVPMatrix");

        for (Line line : lines) {
            float glx1 = world2glX(line.x1);
            float gly1 = world2glY(line.y1);
            float glx2 = world2glX(line.x2);
            float gly2 = world2glY(line.y2);
            // Extend at right angles from the endpoints and draw a rectangle.
            // TODO: antialiasing.
            double angle = Math.atan2(gly2 - gly1, glx2 - glx1);
            // You'd expect there to be a 0.5 factor here so that the total width is getLineWidth(),
            // but that seems to make the lines too narrow.
            double perpDist = 1.0 * fvManager.getLineWidth() / getHeight();
            float perpDx = (float) (perpDist * Math.cos(angle + TAU / 4));
            float perpDy = (float) (perpDist * Math.sin(angle + TAU / 4));

            coords[0] = glx1 - perpDx;
            coords[1] = gly1 - perpDy;
            coords[3] = glx2 - perpDx;
            coords[4] = gly2 - perpDy;
            coords[6] = glx1 + perpDx;
            coords[7] = gly1 + perpDy;
            coords[9] = glx2 - perpDx;
            coords[10] = gly2 - perpDy;
            coords[12] = glx1 + perpDx;
            coords[13] = gly1 + perpDy;
            coords[15] = glx2 + perpDx;
            coords[16] = gly2 + perpDy;

            vertexBuffer.put(coords);
            vertexBuffer.position(0);

            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(linePositionHandle);

            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(linePositionHandle, COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    12, vertexBuffer);

            // Set color, and eventually line endpoints and width.
            GLES20.glUniform4fv(lineColorHandle, 1, line.color, 0);

            // Pass the projection and view transformation to the shader
            GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, vPMatrix, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numVertices);
            GLES20.glDisableVertexAttribArray(linePositionHandle);
        }
    }

    private void drawTest() {
        if (circleProgramId == null) {
            initShaders();
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, 3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        drawCircles();
        drawLines();
    }

    @Override public void setManager(FieldViewManager manager) {
        this.fvManager = manager;
        this.glView.setManager(manager);
    }

    @Override public void drawLine(float x1, float y1, float x2, float y2, int color) {
        Line line = new Line();
        line.x1 = x1;
        line.y1 = y1;
        line.x2 = x2;
        line.y2 = y2;
        setRgba(line.color, color);
        this.lines.add(line);
    }

    @Override public void drawLinePath(float[] xEndpoints, float[] yEndpoints, int color) {
        // TODO: Actual arcs.
        for (int i = 1; i < xEndpoints.length; i++) {
            drawLine(xEndpoints[i - 1], yEndpoints[i - 1], xEndpoints[i], yEndpoints[i], color);
        }
    }

    void recordCircle(float cx, float cy, float radius, int color, boolean filled) {
        Circle c = new Circle();
        c.cx = cx;
        c.cy = cy;
        c.radius = radius;
        c.filled = filled;
        setRgba(c.color, color);
        this.circles.add(c);
    }

    @Override public void fillCircle(float cx, float cy, float radius, int color) {
        recordCircle(cx, cy, radius, color, true);
    }

    @Override public void frameCircle(float cx, float cy, float radius, int color) {
        recordCircle(cx, cy, radius, color, false);
    }

    @Override public boolean canDrawArc() {
        return false;
    }

    @Override public void drawArc(float cx, float cy, float xRadius, float yRadius, float startAngle, float sweepAngle, int color) {

    }

    final Object renderLock = new Object();
    boolean renderDone;

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

    @Override public int getWidth() {
        return glView.getWidth();
    }

    @Override public int getHeight() {
        return glView.getHeight();
    }

    @Override public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        initShaders();
    }

    @Override public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        GLES20.glClearColor( 0.0f, 0.0f, 0.0f, 0.0f );
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // This projection matrix is applied to object coordinates in onDrawFrame().
        float ratio = (float) width / height;
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
    }

    @Override public void onDrawFrame(GL10 gl10) {
        Field field = fvManager.getField();
        if (field == null) return;
        synchronized (field) {
            startDraw();
            field.draw(this);
            endDraw();
        }
        synchronized (renderLock) {
            renderDone = true;
            renderLock.notify();
        }
    }
}
