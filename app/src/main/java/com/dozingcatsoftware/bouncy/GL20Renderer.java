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

    private final GLFieldView glView;
    private final Function<String, String> shaderLookupFn;

    private FieldViewManager fvManager;

    private Integer circleProgramId = null;

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
        circleProgramId = createProgram("shaders/circle.vert", "shaders/circle.frag");
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

    private List<Circle> circles = new ArrayList<>();


    private void startDraw() {
        circles.clear();
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

    private void drawTest() {
        if (circleProgramId == null) {
            initShaders();
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, 3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0);


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
        // add the coordinates to the FloatBuffer
        // vertexBuffer.put(coords);
        // set the buffer to read the first coordinate
        vertexBuffer.position(0);


        GLES20.glUseProgram(circleProgramId);

        int positionHandle = GLES20.glGetAttribLocation(circleProgramId, "position");
        int colorHandle = GLES20.glGetUniformLocation(circleProgramId, "color");
        int centerXHandle = GLES20.glGetUniformLocation(circleProgramId, "centerX");
        int centerYHandle = GLES20.glGetUniformLocation(circleProgramId, "centerY");
        int radiusSqHandle = GLES20.glGetUniformLocation(circleProgramId, "radiusSquared");
        int filledHandle = GLES20.glGetUniformLocation(circleProgramId, "filled");
        int lineWidthHandle = GLES20.glGetUniformLocation(circleProgramId, "lineWidth");

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

            float radiusInPixels = fvManager.world2pixelX(c.radius) - fvManager.world2pixelX(0);

            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(positionHandle);

            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    12, vertexBuffer);

            // get handle to fragment shader's vColor member
            // colorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

            // Set position/radius/color.
            center[0] = fvManager.world2pixelX(c.cx);
            center[1] = fvManager.world2pixelY(c.cy);
            GLES20.glUniform1f(centerXHandle, fvManager.world2pixelX(c.cx));
            GLES20.glUniform1f(centerYHandle, getHeight() - fvManager.world2pixelY(c.cy));
            GLES20.glUniform1f(radiusSqHandle, radiusInPixels * radiusInPixels);
            GLES20.glUniform1f(lineWidthHandle, fvManager.getLineWidth());
            GLES20.glUniform1i(filledHandle, c.filled ? 1 : 0);
            GLES20.glUniform4fv(colorHandle, 1, c.color, 0);


            // get handle to shape's transformation matrix
            int vPMatrixHandle = GLES20.glGetUniformLocation(circleProgramId, "uMVPMatrix");

            // Pass the projection and view transformation to the shader
            GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, vPMatrix, 0);


            // Draw the triangle
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numVertices);

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(positionHandle);
        }
    }

    @Override public void setManager(FieldViewManager manager) {
        this.fvManager = manager;
        this.glView.setManager(manager);
    }

    @Override public void drawLine(float x1, float y1, float x2, float y2, int color) {

    }

    @Override public void drawLinePath(float[] xEndpoints, float[] yEndpoints, int color) {

    }

    void recordCircle(float cx, float cy, float radius, int color, boolean filled) {
        Circle c = new Circle();
        c.cx = cx;
        c.cy = cy;
        c.radius = radius;
        c.filled = filled;
        c.color[0] = Color.getRed(color) / 255f;
        c.color[1] = Color.getGreen(color) / 255f;
        c.color[2] = Color.getBlue(color) / 255f;
        c.color[3] = Color.getAlpha(color) / 255f;
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
