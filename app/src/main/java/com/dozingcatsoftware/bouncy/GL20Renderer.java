package com.dozingcatsoftware.bouncy;

import android.annotation.TargetApi;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Build;

import com.dozingcatsoftware.bouncy.util.TrigLookupTable;
import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.model.Field;
import com.dozingcatsoftware.vectorpinball.model.IFieldRenderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Function;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class GL20Renderer implements IFieldRenderer.FloatOnlyRenderer, GLSurfaceView.Renderer {
    static final double TAU = 2 * Math.PI;

    private final GLFieldView glView;
    private final Function<String, String> shaderLookupFn;

    private FieldViewManager fvManager;

    private int lineVertexBufferId;
    private int lineIndexBufferId;
    private int circleVertexBufferId;
    private int circleIndexBufferId;

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

    TrigLookupTable trigTable = new TrigLookupTable(16, 32, 64, 128);

    public GL20Renderer(GLFieldView view, Function<String, String> shaderLookupFn) {
        this.glView = view;
        view.getHolder().setFormat(PixelFormat.RGBA_8888);
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

        int[] bufferIds = new int[4];
        GLES20.glGenBuffers(4, bufferIds, 0);
        lineVertexBufferId = bufferIds[0];
        lineIndexBufferId = bufferIds[1];
        circleVertexBufferId = bufferIds[2];
        circleIndexBufferId = bufferIds[3];
    }

    // Line layout is 3 floats for vertex position, then 4 unsigned bytes for color.
    private static final int LINE_VERTEX_STRIDE_BYTES = 16;
    private ByteBuffer lineVertices = makeByteBuffer(2048);
    private ByteBuffer lineVertexIndices = makeByteBuffer(512);
    private int numLineVertices = 0;
    private int numLineVertexIndices = 0;

    // Circle layout is 3 floats for vertex position, 4 unsigned bytes for color,
    // 2 floats for center, float for radius, float for inner radius.
    // The floats for center and radii are in pixel coordinates.
    private static final int CIRCLE_VERTEX_STRIDE_BYTES = 32;
    private ByteBuffer circleVertices = makeByteBuffer(2048);
    private ByteBuffer circleVertexIndices = makeByteBuffer(512);
    private int numCircleVertices = 0;
    private int numCircleVertexIndices = 0;

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

        lineVertices.clear();
        numLineVertices = 0;
        lineVertexIndices.clear();
        numLineVertexIndices = 0;

        circleVertices.clear();
        numCircleVertices = 0;
        circleVertexIndices.clear();
        numCircleVertexIndices = 0;
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
        GLES20.glUniformMatrix4fv(circleMvpMatrixHandle, 1, false, vPMatrix, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, circleVertexBufferId);
        circleVertices.flip();
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
                circleVertices.limit(), circleVertices, GLES20.GL_STATIC_DRAW);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, circleIndexBufferId);
        circleVertexIndices.flip();
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER,
                circleVertexIndices.limit(), circleVertexIndices, GLES20.GL_STATIC_DRAW);

        GLES20.glEnableVertexAttribArray(circlePositionHandle);
        GLES20.glVertexAttribPointer(circlePositionHandle, 3,
                GLES20.GL_FLOAT, false,
                CIRCLE_VERTEX_STRIDE_BYTES, 0);

        GLES20.glEnableVertexAttribArray(circleColorHandle);
        GLES20.glVertexAttribPointer(circleColorHandle, 4,
                GLES20.GL_UNSIGNED_BYTE, true,
                CIRCLE_VERTEX_STRIDE_BYTES, 12);

        GLES20.glEnableVertexAttribArray(circleCenterHandle);
        GLES20.glVertexAttribPointer(circleCenterHandle, 2,
                GLES20.GL_FLOAT, false,
                CIRCLE_VERTEX_STRIDE_BYTES, 16);

        GLES20.glEnableVertexAttribArray(circleRadiusSquaredHandle);
        GLES20.glVertexAttribPointer(circleRadiusSquaredHandle, 1,
                GLES20.GL_FLOAT, false,
                CIRCLE_VERTEX_STRIDE_BYTES, 24);

        GLES20.glEnableVertexAttribArray(circleInnerRadiusSquaredHandle);
        GLES20.glVertexAttribPointer(circleInnerRadiusSquaredHandle, 1,
                GLES20.GL_FLOAT, false,
                CIRCLE_VERTEX_STRIDE_BYTES, 28);

        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, numCircleVertexIndices, GLES20.GL_UNSIGNED_INT, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        GLES20.glDisableVertexAttribArray(circlePositionHandle);
        GLES20.glDisableVertexAttribArray(circleColorHandle);
        GLES20.glDisableVertexAttribArray(circleCenterHandle);
        GLES20.glDisableVertexAttribArray(circleRadiusSquaredHandle);
        GLES20.glDisableVertexAttribArray(circleInnerRadiusSquaredHandle);
    }

    private void drawLines() {
        GLES20.glUseProgram(lineProgramId);
        GLES20.glUniformMatrix4fv(lineMvpMatrixHandle, 1, false, vPMatrix, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, lineVertexBufferId);
        lineVertices.flip();
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
                lineVertices.limit(), lineVertices, GLES20.GL_STATIC_DRAW);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, lineIndexBufferId);
        lineVertexIndices.flip();
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER,
                lineVertexIndices.limit(), lineVertexIndices, GLES20.GL_STATIC_DRAW);

        GLES20.glEnableVertexAttribArray(linePositionHandle);
        GLES20.glVertexAttribPointer(
                linePositionHandle, 3, GLES20.GL_FLOAT, false, LINE_VERTEX_STRIDE_BYTES, 0);

        GLES20.glEnableVertexAttribArray(lineColorHandle);
        GLES20.glVertexAttribPointer(
                lineColorHandle, 4, GLES20.GL_UNSIGNED_BYTE, true, LINE_VERTEX_STRIDE_BYTES, 12);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, numLineVertexIndices, GLES20.GL_UNSIGNED_INT, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        GLES20.glDisableVertexAttribArray(linePositionHandle);
        GLES20.glDisableVertexAttribArray(lineColorHandle);
    }

    public void setManager(FieldViewManager manager) {
        this.fvManager = manager;
        this.glView.setManager(manager);
    }

    private void addLine(
            float x1, float y1, float x2, float y2,
            float coreWidthPixels, float aaWidthPixels, int color) {
        boolean useAA = (aaWidthPixels > coreWidthPixels);
        int numVerticesToAdd = useAA ? 8 : 4;
        int numIndicesToAdd = useAA ? 18 : 6;
        int baseIndex = this.numLineVertices;
        lineVertices = ensureRemaining(
                lineVertices, LINE_VERTEX_STRIDE_BYTES * numVerticesToAdd);
        lineVertexIndices = ensureRemaining(lineVertexIndices, numIndicesToAdd * 4);

        float glx1 = world2glX(x1);
        float gly1 = world2glY(y1);
        float glx2 = world2glX(x2);
        float gly2 = world2glY(y2);
        int packedColor = packColor(color);
        // Extend at right angles from the endpoints and draw a rectangle.
        double perpAngle = Math.atan2(gly2 - gly1, glx2 - glx1) + TAU / 4;
        float cosPerp = (float) Math.cos(perpAngle);
        float sinPerp = (float) Math.sin(perpAngle);
        float corePerpDistGl = coreWidthPixels / cachedHeight;
        float coreDx = corePerpDistGl * cosPerp;
        float coreDy = corePerpDistGl * sinPerp;

        // Relative vertex indices. 0-3 form the "core" quad, 4-7 add the quads
        // that fade out if antialiasing is enabled.
        // 6--7
        // 2--3
        // 0--1
        // 4--5
        lineVertices.putFloat(glx1 - coreDx);
        lineVertices.putFloat(gly1 - coreDy);
        lineVertices.putFloat(0f);
        lineVertices.putInt(packedColor);

        lineVertices.putFloat(glx2 - coreDx);
        lineVertices.putFloat(gly2 - coreDy);
        lineVertices.putFloat(0f);
        lineVertices.putInt(packedColor);

        lineVertices.putFloat(glx1 + coreDx);
        lineVertices.putFloat(gly1 + coreDy);
        lineVertices.putFloat(0f);
        lineVertices.putInt(packedColor);

        lineVertices.putFloat(glx2 + coreDx);
        lineVertices.putFloat(gly2 + coreDy);
        lineVertices.putFloat(0f);
        lineVertices.putInt(packedColor);

        lineVertexIndices.putInt(baseIndex + 0);
        lineVertexIndices.putInt(baseIndex + 1);
        lineVertexIndices.putInt(baseIndex + 2);
        lineVertexIndices.putInt(baseIndex + 1);
        lineVertexIndices.putInt(baseIndex + 2);
        lineVertexIndices.putInt(baseIndex + 3);

        if (useAA) {
            int alphaZeroColor = packColor(Color.withAlpha(color, 0));
            float aaPerpDistGl = aaWidthPixels / cachedHeight;
            float aaDx = aaPerpDistGl * cosPerp;
            float aaDy = aaPerpDistGl * sinPerp;

            lineVertices.putFloat(glx1 - aaDx);
            lineVertices.putFloat(gly1 - aaDy);
            lineVertices.putFloat(0f);
            lineVertices.putInt(alphaZeroColor);

            lineVertices.putFloat(glx2 - aaDx);
            lineVertices.putFloat(gly2 - aaDy);
            lineVertices.putFloat(0f);
            lineVertices.putInt(alphaZeroColor);

            lineVertices.putFloat(glx1 + aaDx);
            lineVertices.putFloat(gly1 + aaDy);
            lineVertices.putFloat(0f);
            lineVertices.putInt(alphaZeroColor);

            lineVertices.putFloat(glx2 + aaDx);
            lineVertices.putFloat(gly2 + aaDy);
            lineVertices.putFloat(0f);
            lineVertices.putInt(alphaZeroColor);

            lineVertexIndices.putInt(baseIndex + 0);
            lineVertexIndices.putInt(baseIndex + 1);
            lineVertexIndices.putInt(baseIndex + 4);
            lineVertexIndices.putInt(baseIndex + 1);
            lineVertexIndices.putInt(baseIndex + 4);
            lineVertexIndices.putInt(baseIndex + 5);

            lineVertexIndices.putInt(baseIndex + 2);
            lineVertexIndices.putInt(baseIndex + 3);
            lineVertexIndices.putInt(baseIndex + 6);
            lineVertexIndices.putInt(baseIndex + 3);
            lineVertexIndices.putInt(baseIndex + 6);
            lineVertexIndices.putInt(baseIndex + 7);
        }

        this.numLineVertices += numVerticesToAdd;
        this.numLineVertexIndices += numIndicesToAdd;
    }

    @Override public void drawLine(float x1, float y1, float x2, float y2, int color) {
        // Use antialiasing if lines are thick enough.
        if (cachedLineWidth >= 5) {
            addLine(x1, y1, x2, y2, cachedLineWidth - 2, cachedLineWidth + 2, color);
        }
        else {
            addLine(x1, y1, x2, y2, cachedLineWidth, 0, color);
        }
    }

    @Override public void drawLinePath(float[] xEndpoints, float[] yEndpoints, int color) {
        // We can't reliably share vertex positions because we extend perpendicularly from the
        // line segments, and successive segments may have different angles.
        for (int i = 1; i < xEndpoints.length; i++) {
            drawLine(xEndpoints[i - 1], yEndpoints[i - 1], xEndpoints[i], yEndpoints[i], color);
        }
    }

    private void addFilledCircle(float cx, float cy, float coreRadius, float aaRadius, int color) {
        circleVertices = ensureRemaining(
                circleVertices, CIRCLE_VERTEX_STRIDE_BYTES * 4);
        float glx = world2glX(cx);
        float gly = world2glY(cy);
        float glrad = world2glX(aaRadius) - world2glX(0);
        int packedColor = packColor(color);

        float centerPixelX = worldToGLPixelX(cx);
        float centerPixelY = worldToGLPixelY(cy);

        float coreRadiusInPixels = worldToGLPixelX(coreRadius) - worldToGLPixelX(0);
        float coreRadiusSq = coreRadiusInPixels * coreRadiusInPixels;
        float aaRadiusInPixels = worldToGLPixelX(aaRadius) - worldToGLPixelX(0);
        float aaRadiusSq = aaRadiusInPixels * aaRadiusInPixels;

        // Draw a square covering the circle. For large circle outlines, we could define vertices
        // for inner and outer polygons to reduce the number of fragment shader calls. Seems to not
        // be necessary yet. We pass aaRadiusSq as the "outer" radius. Pixels are transparent
        // outside of it, and between aaRadiusSq and coreRadiusSq there's an alpha gradient.
        circleVertices.putFloat(glx - glrad);
        circleVertices.putFloat(gly - glrad);
        circleVertices.putFloat(0f);
        circleVertices.putInt(packedColor);
        circleVertices.putFloat(centerPixelX);
        circleVertices.putFloat(centerPixelY);
        circleVertices.putFloat(aaRadiusSq);
        circleVertices.putFloat(coreRadiusSq);

        circleVertices.putFloat(glx + glrad);
        circleVertices.putFloat(gly - glrad);
        circleVertices.putFloat(0f);
        circleVertices.putInt(packedColor);
        circleVertices.putFloat(centerPixelX);
        circleVertices.putFloat(centerPixelY);
        circleVertices.putFloat(aaRadiusSq);
        circleVertices.putFloat(coreRadiusSq);

        circleVertices.putFloat(glx - glrad);
        circleVertices.putFloat(gly + glrad);
        circleVertices.putFloat(0f);
        circleVertices.putInt(packedColor);
        circleVertices.putFloat(centerPixelX);
        circleVertices.putFloat(centerPixelY);
        circleVertices.putFloat(aaRadiusSq);
        circleVertices.putFloat(coreRadiusSq);

        circleVertices.putFloat(glx + glrad);
        circleVertices.putFloat(gly + glrad);
        circleVertices.putFloat(0f);
        circleVertices.putInt(packedColor);
        circleVertices.putFloat(centerPixelX);
        circleVertices.putFloat(centerPixelY);
        circleVertices.putFloat(aaRadiusSq);
        circleVertices.putFloat(coreRadiusSq);

        circleVertexIndices = ensureRemaining(circleVertexIndices, 6 * 4);
        int baseIndex = this.numCircleVertices;
        circleVertexIndices.putInt(baseIndex);
        circleVertexIndices.putInt(baseIndex + 1);
        circleVertexIndices.putInt(baseIndex + 2);
        circleVertexIndices.putInt(baseIndex + 1);
        circleVertexIndices.putInt(baseIndex + 2);
        circleVertexIndices.putInt(baseIndex + 3);

        this.numCircleVertices += 4;
        this.numCircleVertexIndices += 6;
    }

    @Override public void fillCircle(float cx, float cy, float radius, int color) {
        float radiusInPixels = worldToGLPixelX(radius) - worldToGLPixelX(0);
        if (radiusInPixels >= 10) {
            // A bit icky because we need to pass world coordinates rather than GL or pixels.
            // Move the "core" radius one pixel in and the AA radius one pixel out.
            float pixelsPerWorldUnit = fvManager.world2pixelX(1) - fvManager.world2pixelX(0);
            float worldDelta = 1 / pixelsPerWorldUnit;
            addFilledCircle(cx, cy, radius - worldDelta, radius + worldDelta, color);
        }
        else {
            addFilledCircle(cx, cy, radius, radius, color);
        }
    }

    private void addPolygonOutline(
            float cx, float cy, float radius, int minPolySides,
            float coreWidthPixels, float aaWidthPixels, int color) {
        TrigLookupTable.SinCosValues sinCosValues = trigTable.valuesWithSizeAtLeast(minPolySides);
        int polySides = sinCosValues.size();
        boolean useAA = (aaWidthPixels > coreWidthPixels);
        // With or without antialiasing, the inner and outer polygons have vertex indices of:
        // 1--3--...--2n-1
        // 0--2--...--2n-2
        // With antialiasing, there are additional polygons above and below to fade out:
        // 2n+1 -- 2n+3 -- ... -- 4n-1
        //    1 --  3   -- ... -- 2n-1
        //    0 --  2   -- ... -- 2n-2
        //   2n -- 2n+2 -- ... -- 4n-2
        int numVerticesToAdd = polySides * (useAA ? 4 : 2);
        int numIndicesToAdd = polySides * (useAA ? 18 : 6);
        lineVertices = ensureRemaining(
                lineVertices, LINE_VERTEX_STRIDE_BYTES * numVerticesToAdd);
        lineVertexIndices = ensureRemaining(lineVertexIndices, numIndicesToAdd * 4);

        float glcx = world2glX(cx);
        float glcy = world2glY(cy);
        float glrad = world2glX(radius) - world2glX(0);
        float corePerpDistGl = coreWidthPixels / cachedHeight;
        float innerRadius = glrad - corePerpDistGl;
        float outerRadius = glrad + corePerpDistGl;
        int packedColor = packColor(color);
        for (int i = 0; i < polySides; i++) {
            lineVertices.putFloat(glcx + innerRadius * sinCosValues.cosAtIndex(i));
            lineVertices.putFloat(glcy + innerRadius * sinCosValues.sinAtIndex(i));
            lineVertices.putFloat(0);
            lineVertices.putInt(packedColor);

            lineVertices.putFloat(glcx + outerRadius * sinCosValues.cosAtIndex(i));
            lineVertices.putFloat(glcy + outerRadius * sinCosValues.sinAtIndex(i));
            lineVertices.putFloat(0);
            lineVertices.putInt(packedColor);

            int baseIndex = this.numLineVertices + 2 * i;
            if (i < polySides - 1) {
                lineVertexIndices.putInt(baseIndex + 0);
                lineVertexIndices.putInt(baseIndex + 1);
                lineVertexIndices.putInt(baseIndex + 2);
                lineVertexIndices.putInt(baseIndex + 1);
                lineVertexIndices.putInt(baseIndex + 2);
                lineVertexIndices.putInt(baseIndex + 3);
            }
            else {
                // Wrap around to start.
                lineVertexIndices.putInt(baseIndex + 0);
                lineVertexIndices.putInt(baseIndex + 1);
                lineVertexIndices.putInt(this.numLineVertices);
                lineVertexIndices.putInt(baseIndex + 1);
                lineVertexIndices.putInt(this.numLineVertices);
                lineVertexIndices.putInt(this.numLineVertices + 1);
            }
        }

        if (useAA) {
            float aaPerpDistGl = aaWidthPixels / cachedHeight;
            float aaInnerRadius = glrad - aaPerpDistGl;
            float aaOuterRadius = glrad + aaPerpDistGl;
            int alphaZeroColor = packColor(Color.withAlpha(color, 0));
            for (int i = 0; i < polySides; i++) {
                lineVertices.putFloat(glcx + aaInnerRadius * sinCosValues.cosAtIndex(i));
                lineVertices.putFloat(glcy + aaInnerRadius * sinCosValues.sinAtIndex(i));
                lineVertices.putFloat(0);
                lineVertices.putInt(alphaZeroColor);

                lineVertices.putFloat(glcx + aaOuterRadius * sinCosValues.cosAtIndex(i));
                lineVertices.putFloat(glcy + aaOuterRadius * sinCosValues.sinAtIndex(i));
                lineVertices.putFloat(0);
                lineVertices.putInt(alphaZeroColor);

                int baseCoreIndex = this.numLineVertices + 2 * i;
                int baseAaIndex = baseCoreIndex + 2 * polySides;
                if (i < polySides - 1) {
                    lineVertexIndices.putInt(baseAaIndex + 0);
                    lineVertexIndices.putInt(baseCoreIndex + 0);
                    lineVertexIndices.putInt(baseAaIndex + 2);
                    lineVertexIndices.putInt(baseCoreIndex + 0);
                    lineVertexIndices.putInt(baseAaIndex + 2);
                    lineVertexIndices.putInt(baseCoreIndex + 2);

                    lineVertexIndices.putInt(baseAaIndex + 1);
                    lineVertexIndices.putInt(baseCoreIndex + 1);
                    lineVertexIndices.putInt(baseAaIndex + 3);
                    lineVertexIndices.putInt(baseCoreIndex + 1);
                    lineVertexIndices.putInt(baseAaIndex + 3);
                    lineVertexIndices.putInt(baseCoreIndex + 3);
                }
                else {
                    // Wrap around to start.
                    lineVertexIndices.putInt(baseAaIndex + 0);
                    lineVertexIndices.putInt(baseCoreIndex + 0);
                    lineVertexIndices.putInt(this.numLineVertices + 2 * polySides);
                    lineVertexIndices.putInt(baseCoreIndex + 0);
                    lineVertexIndices.putInt(this.numLineVertices + 2 * polySides);
                    lineVertexIndices.putInt(this.numLineVertices);

                    lineVertexIndices.putInt(baseAaIndex + 1);
                    lineVertexIndices.putInt(baseCoreIndex + 1);
                    lineVertexIndices.putInt(this.numLineVertices + 2 * polySides + 1);
                    lineVertexIndices.putInt(baseCoreIndex + 1);
                    lineVertexIndices.putInt(this.numLineVertices + 2 * polySides + 1);
                    lineVertexIndices.putInt(this.numLineVertices + 1);
                }
            }

        }

        this.numLineVertices += numVerticesToAdd;
        this.numLineVertexIndices += numIndicesToAdd;
    }

    @Override public void frameCircle(float cx, float cy, float radius, int color) {
        int radPixels = (int) Math.ceil(fvManager.world2pixelX(radius) - fvManager.world2pixelX(0));
        // Draw a polygon, with antialiasing if the line width is sufficient. A 64-sided polygon
        // is good enough for all but the largest circles.
        int minPolySides = radPixels < 256 ? Math.min(64, radPixels) : radPixels;
        if (cachedLineWidth >= 5) {
            addPolygonOutline(cx, cy, radius, minPolySides,
                    cachedLineWidth - 2, cachedLineWidth + 2, color);
        }
        else {
            addPolygonOutline(cx, cy, radius, minPolySides, cachedLineWidth, 0, color);
        }
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
