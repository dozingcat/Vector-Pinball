package com.dozingcatsoftware.bouncy;

import android.annotation.TargetApi;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Build;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.function.Function;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

@TargetApi(Build.VERSION_CODES.FROYO)
public class GL20Renderer implements IFieldRenderer, GLSurfaceView.Renderer {

    private final GLFieldView glView;
    private final Function<String, String> shaderLookupFn;

    private FieldViewManager fvManager;

    private Integer fillCircleProgramId = null;
    private Integer outlineCircleProgramId;

    public GL20Renderer(GLFieldView view, Function<String, String> shaderLookupFn) {
        this.glView = view;
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
        fillCircleProgramId = createProgram("shaders/circle.vert", "shaders/circle.frag");
    }

    private final float[] vPMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];



    private void startDraw() {

    }

    private void endDraw() {

    }

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
        if (fillCircleProgramId == null) {
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

        /*
        float triangleCoords[] = {   // in counterclockwise order:
                0.f,  .5f, 0.0f, // top
                0.f, 0.f, 0.0f, // bottom left
                0.5f, 0.f, 0.0f,  // bottom right
                0f, .5f, 0f,
                .5f, 0f, 0f,
                .95f, .5f, 0f,
        };
        */
        float triangleCoords[] = {
                world2glX(1), world2glY(1), 0f,
                world2glX(19), world2glY(29), 0f,
                world2glX(19), world2glY(1), 0f,
                world2glX(5), world2glY(20), 0f,
                world2glX(5), world2glY(22), 0f,
                world2glX(7), world2glY(22), 0f,
                // -1f, -1f, 0,
                // 1f, -1f, 0,
                // 1f, 1f, 0,
        };

        int numVertices = triangleCoords.length / COORDS_PER_VERTEX;

        // Set color with red, green, blue and alpha (opacity) values
        float color[] = { 0.63671875f, 0.76953125f, 0.22265625f, 1.0f };

            // initialize vertex byte buffer for shape coordinates
            ByteBuffer bb = ByteBuffer.allocateDirect(
                    // (number of coordinate values * 4 bytes per float)
                    triangleCoords.length * 4);
            // use the device hardware's native byte order
            bb.order(ByteOrder.nativeOrder());

            // create a floating point buffer from the ByteBuffer
            vertexBuffer = bb.asFloatBuffer();
            // add the coordinates to the FloatBuffer
            vertexBuffer.put(triangleCoords);
            // set the buffer to read the first coordinate
            vertexBuffer.position(0);


        GLES20.glUseProgram(fillCircleProgramId);

        // get handle to vertex shader's vPosition member
        int positionHandle = GLES20.glGetAttribLocation(fillCircleProgramId, "position");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(positionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                12, vertexBuffer);

        // get handle to fragment shader's vColor member
        // colorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // Set color for drawing the triangle
        // GLES20.glUniform4fv(colorHandle, 1, color, 0);

        // get handle to shape's transformation matrix
        int vPMatrixHandle = GLES20.glGetUniformLocation(fillCircleProgramId, "uMVPMatrix");

        // Pass the projection and view transformation to the shader
        GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, vPMatrix, 0);


        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numVertices);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    @Override public void setManager(FieldViewManager manager) {
        this.fvManager = manager;
        this.glView.setManager(manager);
    }

    @Override public void drawLine(float x1, float y1, float x2, float y2, int color) {

    }

    @Override public void drawLinePath(float[] xEndpoints, float[] yEndpoints, int color) {

    }

    @Override public void fillCircle(float cx, float cy, float radius, int color) {

    }

    @Override public void frameCircle(float cx, float cy, float radius, int color) {

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
        float ratio = (float) width / height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);

    }

    @Override public void onDrawFrame(GL10 gl10) {
        drawTest();
        Field field = fvManager.getField();
        if (field == null) return;
        synchronized (field) {
            // startGLElements(gl);
            field.draw(this);
            // endGLElements(gl);
        }
        synchronized (renderLock) {
            renderDone = true;
            renderLock.notify();
        }
    }
}
