package com.dozingcatsoftware.bouncy;

import android.annotation.TargetApi;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Build;

import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.model.Field;
import com.dozingcatsoftware.vectorpinball.model.IFieldRenderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Function;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

@TargetApi(Build.VERSION_CODES.FROYO)
public class GL20Renderer implements IFieldRenderer, GLSurfaceView.Renderer {
    static final double TAU = 2 * Math.PI;

    private final GLFieldView glView;
    private final Function<String, String> shaderLookupFn;

    private FieldViewManager fvManager;

    private final float[] vPMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];

    private Integer circleProgramId = null;
    private int circleMvpMatrixHandle;
    private int circlePositionHandle;
    private int circleColorHandle;
    private int circleCenterHandle;
    private int circleRadiusSquaredHandle;
    private int circleInnerRadiusSquaredHandle;

    private Integer lineProgramId = null;
    private int lineMvpMatrixHandle;
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

    private static ByteBuffer makeByteBuffer(int cap) {
        // glVertexAttribPointer requires a direct-allocated buffer.
        ByteBuffer bb = ByteBuffer.allocateDirect(cap);
        bb.order(ByteOrder.nativeOrder());
        return bb;
    }

    private static ByteBuffer ensureRemaining(ByteBuffer buffer, int requiredRemaining) {
        if (buffer.remaining() >= requiredRemaining) {
            return buffer;
        }
        int newSize = Math.max(buffer.capacity() + requiredRemaining * 2, buffer.capacity() * 2);
        ByteBuffer newBuffer = makeByteBuffer(newSize);
        buffer.position(0);
        newBuffer.put(buffer);
        return newBuffer;
    }

    private static final boolean LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    private static int packColor(int color) {
        // For little endian this is ABGR, for big endian RGBA.
        int red = Color.getRed(color);
        int green = Color.getGreen(color);
        int blue = Color.getBlue(color);
        int alpha = Color.getAlpha(color);
        return LITTLE_ENDIAN ?
            (alpha << 24) | (blue << 16) | (green << 8) | red :
            (red << 24) | (green << 16) | (blue << 8) | alpha;
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
        circleProgramId = createProgram("shaders/circle.vert", "shaders/circle.frag");
        circleMvpMatrixHandle = GLES20.glGetUniformLocation(circleProgramId, "uMVPMatrix");
        circlePositionHandle = GLES20.glGetAttribLocation(circleProgramId, "position");
        circleColorHandle = GLES20.glGetAttribLocation(circleProgramId, "inColor");
        circleCenterHandle = GLES20.glGetAttribLocation(circleProgramId, "inCenter");
        circleRadiusSquaredHandle = GLES20.glGetAttribLocation(circleProgramId, "inRadiusSquared");
        circleInnerRadiusSquaredHandle = GLES20.glGetAttribLocation(circleProgramId, "inInnerRadiusSquared");

        lineProgramId = createProgram("shaders/line.vert", "shaders/line.frag");
        lineMvpMatrixHandle = GLES20.glGetUniformLocation(lineProgramId, "uMVPMatrix");
        linePositionHandle = GLES20.glGetAttribLocation(lineProgramId, "position");
        lineColorHandle = GLES20.glGetAttribLocation(lineProgramId, "inColor");
    }

    // Line layout is 3 floats for vertex position, then 4 unsigned bytes for color.
    private static final int LINE_VERTEX_STRIDE_BYTES = 16;
    private static final int VERTICES_PER_LINE = 6;
    private ByteBuffer lineVertices = makeByteBuffer(2000);
    private int numLineVertices = 0;

    // Circle layout is 3 floats for vertex position, 4 unsigned bytes for color,
    // 2 floats for center, float for radius, float for inner radius.
    // The floats for center and radii are in pixel coordinates.
    private static final int CIRCLE_VERTEX_STRIDE_BYTES = 32;
    private static final int VERTICES_PER_CIRCLE = 6;
    private ByteBuffer circleVertices = makeByteBuffer(2000);
    private int numCircleVertices = 0;

    int cachedWidth;
    int cachedHeight;
    int cachedLineWidth;

    // The OpenGL coordinate system has a visible region from -1 to +1. The projection matrix
    // applies a scaling factor so that +1 and -1 are at the edges of the largest dimension
    // (most likely height), while the smaller dimension has 0 and the midpoint and the visible
    // edge will be less than 1. For example, if the height is 800 and the width is 600, the X axis
    // has 3/4 the visible range of the Y axis, and the visible range of X coordinates will be
    // -0.75 to +0.75.
    float world2glX(float x) {
        float scale = Math.max(cachedWidth, cachedHeight);
        float wx = fvManager.world2pixelX(x);
        float offset = 1 - cachedWidth / scale;
        float glx = ((2 * wx) / scale - 1) + offset;
        return glx;
    }

    float world2glY(float y) {
        float scale = Math.max(cachedWidth, cachedHeight);
        float wy = fvManager.world2pixelY(y);
        float offset = 1 - cachedHeight / scale;
        float gly = -(((2 * wy) / scale - 1) + offset);
        return gly;
    }

    private float worldToGLPixelX(float x) {
        return fvManager.world2pixelX(x);
    }

    private float worldToGLPixelY(float y) {
        // FieldViewManager assumes positive Y is down, but here it's up.
        return cachedHeight - fvManager.world2pixelY(y);
    }

    private void startDraw() {
        cachedWidth = getWidth();
        cachedHeight = getHeight();
        cachedLineWidth = fvManager.getLineWidth();

        lineVertices.position(0);
        numLineVertices = 0;

        circleVertices.position(0);
        numCircleVertices = 0;
    }

    private void endDraw() {
        if (circleProgramId == null) {
            initShaders();
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, 3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        drawLines();
        drawCircles();
    }

    private void drawCircles() {
        GLES20.glUseProgram(circleProgramId);

        GLES20.glEnableVertexAttribArray(circlePositionHandle);
        circleVertices.position(0);
        GLES20.glVertexAttribPointer(circlePositionHandle, 3,
                GLES20.GL_FLOAT, false,
                CIRCLE_VERTEX_STRIDE_BYTES, circleVertices);

        GLES20.glEnableVertexAttribArray(circleColorHandle);
        circleVertices.position(12);
        GLES20.glVertexAttribPointer(circleColorHandle, 4,
                GLES20.GL_UNSIGNED_BYTE, true,
                CIRCLE_VERTEX_STRIDE_BYTES, circleVertices);

        GLES20.glEnableVertexAttribArray(circleCenterHandle);
        circleVertices.position(16);
        GLES20.glVertexAttribPointer(circleCenterHandle, 2,
                GLES20.GL_FLOAT, false,
                CIRCLE_VERTEX_STRIDE_BYTES, circleVertices);

        GLES20.glEnableVertexAttribArray(circleRadiusSquaredHandle);
        circleVertices.position(24);
        GLES20.glVertexAttribPointer(circleRadiusSquaredHandle, 1,
                GLES20.GL_FLOAT, false,
                CIRCLE_VERTEX_STRIDE_BYTES, circleVertices);

        GLES20.glEnableVertexAttribArray(circleInnerRadiusSquaredHandle);
        circleVertices.position(28);
        GLES20.glVertexAttribPointer(circleInnerRadiusSquaredHandle, 1,
                GLES20.GL_FLOAT, false,
                CIRCLE_VERTEX_STRIDE_BYTES, circleVertices);

        GLES20.glUniformMatrix4fv(circleMvpMatrixHandle, 1, false, vPMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numCircleVertices);

        // Disable vertex arrays.
        GLES20.glDisableVertexAttribArray(circlePositionHandle);
        GLES20.glDisableVertexAttribArray(circleColorHandle);
        GLES20.glDisableVertexAttribArray(circleCenterHandle);
        GLES20.glDisableVertexAttribArray(circleRadiusSquaredHandle);
        GLES20.glDisableVertexAttribArray(circleInnerRadiusSquaredHandle);
    }

    private void drawLines() {

        GLES20.glUseProgram(lineProgramId);

        GLES20.glEnableVertexAttribArray(linePositionHandle);
        lineVertices.position(0);
        GLES20.glVertexAttribPointer(linePositionHandle, 3,
                GLES20.GL_FLOAT, false,
                LINE_VERTEX_STRIDE_BYTES, lineVertices);

        GLES20.glEnableVertexAttribArray(lineColorHandle);
        lineVertices.position(12);
        GLES20.glVertexAttribPointer(lineColorHandle, 4,
                GLES20.GL_UNSIGNED_BYTE, true,
                LINE_VERTEX_STRIDE_BYTES, lineVertices);

        GLES20.glUniformMatrix4fv(lineMvpMatrixHandle, 1, false, vPMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numLineVertices);

        GLES20.glDisableVertexAttribArray(linePositionHandle);
        GLES20.glDisableVertexAttribArray(lineColorHandle);
    }

    @Override public void setManager(FieldViewManager manager) {
        this.fvManager = manager;
        this.glView.setManager(manager);
    }

    @Override public void drawLine(float x1, float y1, float x2, float y2, int color) {
        lineVertices = ensureRemaining(lineVertices, LINE_VERTEX_STRIDE_BYTES * VERTICES_PER_LINE);

        float glx1 = world2glX(x1);
        float gly1 = world2glY(y1);
        float glx2 = world2glX(x2);
        float gly2 = world2glY(y2);
        // Extend at right angles from the endpoints and draw a rectangle.
        // TODO: antialiasing?
        double angle = Math.atan2(gly2 - gly1, glx2 - glx1);
        // You'd expect there to be a 0.5 factor here so that the total width is getLineWidth(),
        // but that seems to make the lines too narrow.
        double perpDist = 1.0 * cachedLineWidth / cachedHeight;
        float perpDx = (float) (perpDist * Math.cos(angle + TAU / 4));
        float perpDy = (float) (perpDist * Math.sin(angle + TAU / 4));

        // We want to write a single int rather than 4 bytes to reduce the number of put() calls.
        int packedColor = packColor(color);

        lineVertices.putFloat(glx1 - perpDx);
        lineVertices.putFloat(gly1 - perpDy);
        lineVertices.putFloat(0f);
        lineVertices.putInt(packedColor);

        lineVertices.putFloat(glx2 - perpDx);
        lineVertices.putFloat(gly2 - perpDy);
        lineVertices.putFloat(0f);
        lineVertices.putInt(packedColor);

        lineVertices.putFloat(glx1 + perpDx);
        lineVertices.putFloat(gly1 + perpDy);
        lineVertices.putFloat(0f);
        lineVertices.putInt(packedColor);

        lineVertices.putFloat(glx2 - perpDx);
        lineVertices.putFloat(gly2 - perpDy);
        lineVertices.putFloat(0f);
        lineVertices.putInt(packedColor);

        lineVertices.putFloat(glx1 + perpDx);
        lineVertices.putFloat(gly1 + perpDy);
        lineVertices.putFloat(0f);
        lineVertices.putInt(packedColor);

        lineVertices.putFloat(glx2 + perpDx);
        lineVertices.putFloat(gly2 + perpDy);
        lineVertices.putFloat(0f);
        lineVertices.putInt(packedColor);

        numLineVertices += VERTICES_PER_LINE;
    }

    @Override public void drawLinePath(float[] xEndpoints, float[] yEndpoints, int color) {
        // TODO: Actual arcs.
        for (int i = 1; i < xEndpoints.length; i++) {
            drawLine(xEndpoints[i - 1], yEndpoints[i - 1], xEndpoints[i], yEndpoints[i], color);
        }
    }

    void recordCircle(float cx, float cy, float radius, int color, boolean filled) {
        circleVertices = ensureRemaining(
                circleVertices, CIRCLE_VERTEX_STRIDE_BYTES * VERTICES_PER_CIRCLE);
        float glx = world2glX(cx);
        float gly = world2glY(cy);
        float glrad = world2glX(radius) - world2glX(0);
        int packedColor = packColor(color);

        float centerPixelX = worldToGLPixelX(cx);
        float centerPixelY = worldToGLPixelY(cy);

        float radiusInPixels = worldToGLPixelX(radius) - worldToGLPixelX(0);
        float radiusSq = radiusInPixels * radiusInPixels;
        float innerRadiusSq = filled ?
                0f : (radiusInPixels - cachedLineWidth) * (radiusInPixels - cachedLineWidth);

        // Draw a square covering the circle. For large circle outlines, we could define vertices
        // for inner and outer polygons to reduce the number of fragment shader calls. Seems to not
        // be necessary yet.
        circleVertices.putFloat(glx - glrad);
        circleVertices.putFloat(gly - glrad);
        circleVertices.putFloat(0f);
        circleVertices.putInt(packedColor);
        circleVertices.putFloat(centerPixelX);
        circleVertices.putFloat(centerPixelY);
        circleVertices.putFloat(radiusSq);
        circleVertices.putFloat(innerRadiusSq);

        circleVertices.putFloat(glx + glrad);
        circleVertices.putFloat(gly - glrad);
        circleVertices.putFloat(0f);
        circleVertices.putInt(packedColor);
        circleVertices.putFloat(centerPixelX);
        circleVertices.putFloat(centerPixelY);
        circleVertices.putFloat(radiusSq);
        circleVertices.putFloat(innerRadiusSq);

        circleVertices.putFloat(glx - glrad);
        circleVertices.putFloat(gly + glrad);
        circleVertices.putFloat(0f);
        circleVertices.putInt(packedColor);
        circleVertices.putFloat(centerPixelX);
        circleVertices.putFloat(centerPixelY);
        circleVertices.putFloat(radiusSq);
        circleVertices.putFloat(innerRadiusSq);

        circleVertices.putFloat(glx - glrad);
        circleVertices.putFloat(gly + glrad);
        circleVertices.putFloat(0f);
        circleVertices.putInt(packedColor);
        circleVertices.putFloat(centerPixelX);
        circleVertices.putFloat(centerPixelY);
        circleVertices.putFloat(radiusSq);
        circleVertices.putFloat(innerRadiusSq);

        circleVertices.putFloat(glx + glrad);
        circleVertices.putFloat(gly - glrad);
        circleVertices.putFloat(0f);
        circleVertices.putInt(packedColor);
        circleVertices.putFloat(centerPixelX);
        circleVertices.putFloat(centerPixelY);
        circleVertices.putFloat(radiusSq);
        circleVertices.putFloat(innerRadiusSq);

        circleVertices.putFloat(glx + glrad);
        circleVertices.putFloat(gly + glrad);
        circleVertices.putFloat(0f);
        circleVertices.putInt(packedColor);
        circleVertices.putFloat(centerPixelX);
        circleVertices.putFloat(centerPixelY);
        circleVertices.putFloat(radiusSq);
        circleVertices.putFloat(innerRadiusSq);

        numCircleVertices += 6;
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
        GLES20.glClearColor( 0.0f, 0.0f, 0.0f, 1.0f );
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
