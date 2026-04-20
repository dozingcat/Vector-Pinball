package com.dozingcatsoftware.bouncy;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.model.Field;
import com.dozingcatsoftware.vectorpinball.model.IField3DRenderer;
import com.dozingcatsoftware.vectorpinball.model.IFieldRenderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.function.Function;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 3D renderer for the pinball field using GLES20. Implements both IField3DRenderer (for 3D
 * drawing calls from elements) and IFieldRenderer (so FieldViewManager can use it for doDraw/
 * getWidth/getHeight). The 2D IFieldRenderer methods are no-ops since we use draw3D exclusively.
 */
public class GL3DRenderer implements IField3DRenderer, IFieldRenderer.FloatOnlyRenderer,
        GLSurfaceView.Renderer {

    private final GLFieldView glView;
    private final Function<String, String> shaderLookupFn;
    private FieldViewManager fvManager;

    // Matrices.
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] vpMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // Shader program handles.
    private int programId;
    private int mvpMatrixHandle;
    private int modelMatrixHandle;
    private int positionHandle;
    private int normalHandle;
    private int colorHandle;
    private int lightDirHandle;

    // Pre-built unit geometry stored in GPU VBOs.
    // Each mesh has a vertex VBO (positions), normal VBO, index VBO, and index count.
    private int sphereVertVbo, sphereNormalVbo, sphereIndexVbo, sphereIndexCount;
    private int cylinderVertVbo, cylinderNormalVbo, cylinderIndexVbo, cylinderIndexCount;
    private int boxVertVbo, boxNormalVbo, boxIndexVbo, boxIndexCount;

    private int cachedWidth, cachedHeight;

    // Overlay state: when active, draws are lifted above the field with depth test disabled.
    private boolean overlayActive;
    private static final float OVERLAY_Z_OFFSET = 5f;

    // Normalized light direction (pointing toward the scene from above-front).
    private final float[] lightDir = new float[3];

    // Render synchronization (same pattern as GL20Renderer).
    final Object renderLock = new Object();
    boolean renderDone;

    public GL3DRenderer(GLFieldView view, Function<String, String> shaderLookupFn) {
        this.glView = view;
        this.shaderLookupFn = shaderLookupFn;
        view.setEGLContextClientVersion(2);
        view.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        view.setRenderer(this);
        view.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void setManager(FieldViewManager manager) {
        this.fvManager = manager;
        this.glView.setManager(manager);
    }

    // --- GLSurfaceView.Renderer ---

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initShaders();
        buildSphereGeometry(16, 32);
        buildCylinderGeometry(32);
        buildBoxGeometry();

        // Normalize light direction: coming from slightly in front and above.
        float lx = 0.2f, ly = -0.3f, lz = -0.9f;
        float len = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
        lightDir[0] = lx / len;
        lightDir[1] = ly / len;
        lightDir[2] = lz / len;
    }

    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        cachedWidth = width;
        cachedHeight = height;

        float ratio = (float) width / height;
        Matrix.perspectiveM(projectionMatrix, 0, 45f, ratio, 1f, 200f);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClearColor(0.02f, 0.02f, 0.05f, 1.0f);
    }

    @Override public void onDrawFrame(GL10 gl) {
        Field field = fvManager.getField();
        if (field == null) return;

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(programId);

        synchronized (field) {
            setupCamera(field);
            GLES20.glUniform3fv(lightDirHandle, 1, lightDir, 0);
            drawTableSurface(field);
            field.draw3D(this);
        }

        synchronized (renderLock) {
            renderDone = true;
            renderLock.notify();
        }
    }

    // --- Camera ---

    private void setupCamera(Field field) {
        float fw = field.getWidth();
        float fh = field.getHeight();
        float centerX = fw / 2f;
        float centerY = fh / 2f;

        // Camera above the table, tilted to look down at it.
        float eyeX = centerX;
        float eyeY = centerY - 18f;
        float eyeZ = 45f;
        float lookY = centerY;

        Matrix.setLookAtM(viewMatrix, 0,
                eyeX, eyeY, eyeZ,
                centerX, lookY, 0f,
                0f, 1f, 0f);
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
    }

    // --- Table surface ---

    private void drawTableSurface(Field field) {
        float w = field.getWidth();
        float h = field.getHeight();
        // Dark green table surface.
        int color = Color.fromRGB(10, 30, 10);
        drawBox(0, 0, -0.1f, w, h, 0f, color);
    }

    // --- IField3DRenderer ---

    @Override public void begin3DFrame() {
    }

    @Override public void end3DFrame() {
    }

    @Override public void beginOverlay() {
        overlayActive = true;
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
    }

    @Override public void endOverlay() {
        overlayActive = false;
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    @Override public void drawSphere(float cx, float cy, float cz, float radius, int color) {
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, cx, cy, cz);
        Matrix.scaleM(modelMatrix, 0, radius, radius, radius);
        setMvpAndColor(color);
        drawVboMesh(sphereVertVbo, sphereNormalVbo, sphereIndexVbo, sphereIndexCount);
    }

    @Override public void drawBox(float x1, float y1, float z1,
                                   float x2, float y2, float z2, int color) {
        float sx = x2 - x1;
        float sy = y2 - y1;
        float sz = z2 - z1;
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x1, y1, z1);
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz);
        setMvpAndColor(color);
        drawVboMesh(boxVertVbo, boxNormalVbo, boxIndexVbo, boxIndexCount);
    }

    @Override public void drawCylinder(float cx, float cy, float zBottom, float zTop,
                                        float radius, int color) {
        float height = zTop - zBottom;
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, cx, cy, zBottom);
        Matrix.scaleM(modelMatrix, 0, radius, radius, height);
        setMvpAndColor(color);
        drawVboMesh(cylinderVertVbo, cylinderNormalVbo, cylinderIndexVbo, cylinderIndexCount);
    }

    @Override public void drawWallBox(float x1, float y1, float x2, float y2,
                                       float zBottom, float zTop, float thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.hypot(dx, dy);
        if (length < 0.001f) return;

        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        float midX = (x1 + x2) / 2f;
        float midY = (y1 + y2) / 2f;
        float height = zTop - zBottom;

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, midX, midY, zBottom);
        Matrix.rotateM(modelMatrix, 0, angle, 0, 0, 1);
        Matrix.scaleM(modelMatrix, 0, length, thickness * 2, height);
        Matrix.translateM(modelMatrix, 0, -0.5f, -0.5f, 0f);

        setMvpAndColor(color);
        drawVboMesh(boxVertVbo, boxNormalVbo, boxIndexVbo, boxIndexCount);
    }

    @Override public void drawQuad(float x1, float y1, float x2, float y2,
                                    float x3, float y3, float x4, float y4,
                                    float z, int color) {
        float minX = Math.min(Math.min(x1, x2), Math.min(x3, x4));
        float maxX = Math.max(Math.max(x1, x2), Math.max(x3, x4));
        float minY = Math.min(Math.min(y1, y2), Math.min(y3, y4));
        float maxY = Math.max(Math.max(y1, y2), Math.max(y3, y4));
        drawBox(minX, minY, z - 0.01f, maxX, maxY, z + 0.01f, color);
    }

    // --- Helpers ---

    private void setMvpAndColor(int color) {
        if (overlayActive) {
            // Lift the model above the field so it renders on top of all elements.
            Matrix.translateM(modelMatrix, 0, 0f, 0f, OVERLAY_Z_OFFSET);
        }
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0);

        float r = Color.getRed(color) / 255f;
        float g = Color.getGreen(color) / 255f;
        float b = Color.getBlue(color) / 255f;
        float a = Color.getAlpha(color) / 255f;
        GLES20.glUniform4f(colorHandle, r, g, b, a);
    }

    private void drawVboMesh(int vertVbo, int normalVbo, int indexVbo, int indexCount) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertVbo);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glEnableVertexAttribArray(positionHandle);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, normalVbo);
        GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glEnableVertexAttribArray(normalHandle);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexVbo);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    // --- Shader init ---

    private void initShaders() {
        programId = createProgram("shaders/3d.vert", "shaders/3d.frag");
        mvpMatrixHandle = GLES20.glGetUniformLocation(programId, "uMVPMatrix");
        modelMatrixHandle = GLES20.glGetUniformLocation(programId, "uModelMatrix");
        positionHandle = GLES20.glGetAttribLocation(programId, "aPosition");
        normalHandle = GLES20.glGetAttribLocation(programId, "aNormal");
        colorHandle = GLES20.glGetUniformLocation(programId, "uColor");
        lightDirHandle = GLES20.glGetUniformLocation(programId, "uLightDir");
    }

    private int createProgram(String vertPath, String fragPath) {
        int pid = GLES20.glCreateProgram();
        GLES20.glAttachShader(pid, loadShader(GLES20.GL_VERTEX_SHADER, vertPath));
        GLES20.glAttachShader(pid, loadShader(GLES20.GL_FRAGMENT_SHADER, fragPath));
        GLES20.glLinkProgram(pid);
        return pid;
    }

    private int loadShader(int type, String path) {
        String src = shaderLookupFn.apply(path);
        int id = GLES20.glCreateShader(type);
        GLES20.glShaderSource(id, src);
        GLES20.glCompileShader(id);
        return id;
    }

    // --- Geometry generation ---

    /** Creates a GL buffer, uploads float data, and returns the buffer ID. */
    private static int createFloatVbo(float[] data) {
        FloatBuffer fb = ByteBuffer.allocateDirect(data.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        fb.put(data).position(0);
        int[] id = new int[1];
        GLES20.glGenBuffers(1, id, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, id[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, data.length * 4, fb, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        return id[0];
    }

    /** Creates a GL element buffer, uploads short data, and returns the buffer ID. */
    private static int createShortElementVbo(short[] data) {
        ShortBuffer sb = ByteBuffer.allocateDirect(data.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        sb.put(data).position(0);
        int[] id = new int[1];
        GLES20.glGenBuffers(1, id, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, id[0]);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, data.length * 2, sb,
                GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        return id[0];
    }

    /** Builds a unit sphere centered at origin with radius 1. */
    private void buildSphereGeometry(int latSegments, int lonSegments) {
        int numVerts = (latSegments + 1) * (lonSegments + 1);
        float[] verts = new float[numVerts * 3];
        float[] normals = new float[numVerts * 3];

        int vi = 0;
        for (int lat = 0; lat <= latSegments; lat++) {
            float theta = (float) (lat * Math.PI / latSegments);
            float sinT = (float) Math.sin(theta);
            float cosT = (float) Math.cos(theta);
            for (int lon = 0; lon <= lonSegments; lon++) {
                float phi = (float) (lon * 2 * Math.PI / lonSegments);
                float x = sinT * (float) Math.cos(phi);
                float y = sinT * (float) Math.sin(phi);
                float z = cosT;
                verts[vi] = x;
                normals[vi++] = x;
                verts[vi] = y;
                normals[vi++] = y;
                verts[vi] = z;
                normals[vi++] = z;
            }
        }

        int numIndices = latSegments * lonSegments * 6;
        short[] indices = new short[numIndices];
        int ii = 0;
        for (int lat = 0; lat < latSegments; lat++) {
            for (int lon = 0; lon < lonSegments; lon++) {
                int first = lat * (lonSegments + 1) + lon;
                int second = first + lonSegments + 1;
                indices[ii++] = (short) first;
                indices[ii++] = (short) second;
                indices[ii++] = (short) (first + 1);
                indices[ii++] = (short) (second);
                indices[ii++] = (short) (second + 1);
                indices[ii++] = (short) (first + 1);
            }
        }

        sphereVertVbo = createFloatVbo(verts);
        sphereNormalVbo = createFloatVbo(normals);
        sphereIndexVbo = createShortElementVbo(indices);
        sphereIndexCount = numIndices;
    }

    /** Builds a unit cylinder from z=0 to z=1, radius=1, with caps. */
    private void buildCylinderGeometry(int segments) {
        // Side vertices: 2 rings of (segments+1) vertices.
        // Top cap: center + segments+1 ring vertices.
        // Bottom cap: center + segments+1 ring vertices.
        int sideVerts = (segments + 1) * 2;
        int capVerts = (segments + 1) + 1; // ring + center
        int totalVerts = sideVerts + capVerts * 2;
        float[] verts = new float[totalVerts * 3];
        float[] normals = new float[totalVerts * 3];
        int vi = 0;

        // Side vertices.
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            float x = (float) Math.cos(angle);
            float y = (float) Math.sin(angle);
            // Bottom ring.
            verts[vi] = x; normals[vi++] = x;
            verts[vi] = y; normals[vi++] = y;
            verts[vi] = 0; normals[vi++] = 0;
            // Top ring.
            verts[vi] = x; normals[vi++] = x;
            verts[vi] = y; normals[vi++] = y;
            verts[vi] = 1; normals[vi++] = 0;
        }

        // Top cap.
        int topCenterIdx = vi / 3;
        verts[vi] = 0; normals[vi++] = 0;
        verts[vi] = 0; normals[vi++] = 0;
        verts[vi] = 1; normals[vi++] = 1;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            float x = (float) Math.cos(angle);
            float y = (float) Math.sin(angle);
            verts[vi] = x; normals[vi++] = 0;
            verts[vi] = y; normals[vi++] = 0;
            verts[vi] = 1; normals[vi++] = 1;
        }

        // Bottom cap.
        int bottomCenterIdx = vi / 3;
        verts[vi] = 0; normals[vi++] = 0;
        verts[vi] = 0; normals[vi++] = 0;
        verts[vi] = 0; normals[vi++] = -1;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            float x = (float) Math.cos(angle);
            float y = (float) Math.sin(angle);
            verts[vi] = x; normals[vi++] = 0;
            verts[vi] = y; normals[vi++] = 0;
            verts[vi] = 0; normals[vi++] = -1;
        }

        // Indices: side quads + top cap tris + bottom cap tris.
        int sideIndices = segments * 6;
        int capIndices = segments * 3;
        int totalIndices = sideIndices + capIndices * 2;
        short[] indices = new short[totalIndices];
        int ii = 0;

        // Side.
        for (int i = 0; i < segments; i++) {
            int bl = i * 2;
            int tl = bl + 1;
            int br = bl + 2;
            int tr = bl + 3;
            indices[ii++] = (short) bl;
            indices[ii++] = (short) br;
            indices[ii++] = (short) tl;
            indices[ii++] = (short) tl;
            indices[ii++] = (short) br;
            indices[ii++] = (short) tr;
        }

        // Top cap.
        for (int i = 0; i < segments; i++) {
            indices[ii++] = (short) topCenterIdx;
            indices[ii++] = (short) (topCenterIdx + 1 + i);
            indices[ii++] = (short) (topCenterIdx + 2 + i);
        }

        // Bottom cap (reversed winding).
        for (int i = 0; i < segments; i++) {
            indices[ii++] = (short) bottomCenterIdx;
            indices[ii++] = (short) (bottomCenterIdx + 2 + i);
            indices[ii++] = (short) (bottomCenterIdx + 1 + i);
        }

        cylinderVertVbo = createFloatVbo(verts);
        cylinderNormalVbo = createFloatVbo(normals);
        cylinderIndexVbo = createShortElementVbo(indices);
        cylinderIndexCount = totalIndices;
    }

    /** Builds a unit box from (0,0,0) to (1,1,1). */
    private void buildBoxGeometry() {
        // 24 vertices (4 per face, with distinct normals).
        float[] verts = {
                // Front face (z=1)
                0,0,1, 1,0,1, 1,1,1, 0,1,1,
                // Back face (z=0)
                1,0,0, 0,0,0, 0,1,0, 1,1,0,
                // Top face (y=1)
                0,1,1, 1,1,1, 1,1,0, 0,1,0,
                // Bottom face (y=0)
                0,0,0, 1,0,0, 1,0,1, 0,0,1,
                // Right face (x=1)
                1,0,1, 1,0,0, 1,1,0, 1,1,1,
                // Left face (x=0)
                0,0,0, 0,0,1, 0,1,1, 0,1,0,
        };
        float[] normals = {
                0,0,1, 0,0,1, 0,0,1, 0,0,1,
                0,0,-1, 0,0,-1, 0,0,-1, 0,0,-1,
                0,1,0, 0,1,0, 0,1,0, 0,1,0,
                0,-1,0, 0,-1,0, 0,-1,0, 0,-1,0,
                1,0,0, 1,0,0, 1,0,0, 1,0,0,
                -1,0,0, -1,0,0, -1,0,0, -1,0,0,
        };
        short[] indices = {
                0,1,2, 0,2,3,       // front
                4,5,6, 4,6,7,       // back
                8,9,10, 8,10,11,    // top
                12,13,14, 12,14,15, // bottom
                16,17,18, 16,18,19, // right
                20,21,22, 20,22,23, // left
        };

        boxVertVbo = createFloatVbo(verts);
        boxNormalVbo = createFloatVbo(normals);
        boxIndexVbo = createShortElementVbo(indices);
        boxIndexCount = indices.length;
    }

    // --- IFieldRenderer (minimal 2D stubs so FieldViewManager can use this) ---

    @Override public void doDraw() {
        synchronized (renderLock) {
            renderDone = false;
        }
        glView.requestRender();
        synchronized (renderLock) {
            while (!renderDone) {
                try {
                    renderLock.wait();
                } catch (InterruptedException ex) {
                    // Ignored.
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

    @Override public void drawLine(float x1, float y1, float x2, float y2, int color) {
        // 2D fallback: draw as a thin wall on the table surface.
        drawWallBox(x1, y1, x2, y2, 0f, 0.05f, 0.03f, color);
    }

    @Override public void drawLinePath(float[] xEndpoints, float[] yEndpoints, int color) {
        for (int i = 1; i < xEndpoints.length; i++) {
            drawLine(xEndpoints[i - 1], yEndpoints[i - 1], xEndpoints[i], yEndpoints[i], color);
        }
    }

    @Override public void fillCircle(float cx, float cy, float radius, int color) {
        drawCylinder(cx, cy, 0f, 0.05f, radius, color);
    }

    @Override public void frameCircle(float cx, float cy, float radius, int color) {
        drawCylinder(cx, cy, 0f, 0.03f, radius, color);
    }
}
