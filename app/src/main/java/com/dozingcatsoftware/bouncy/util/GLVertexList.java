package com.dozingcatsoftware.bouncy.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class GLVertexList {

    FloatBuffer vertexBuffer = null;
    FloatBuffer colorBuffer = null;
    int numVertices;

    int vertexIndex;
    int colorIndex;

    float[] vertexCoords = null;
    float[] colorComponents = null;

    int glMode;

    public void setGLMode(int glMode) {
        this.glMode = glMode;
    }

    public void begin() {
        numVertices = 0;
        vertexIndex = colorIndex = 0;
    }

    public void addVertex(float x, float y) {
        if (vertexCoords==null) {
            vertexCoords = new float[10];
        }
        else if (vertexIndex+1 >= vertexCoords.length) {
            float[] newArray = new float[2*vertexCoords.length];
            System.arraycopy(vertexCoords, 0, newArray, 0, vertexIndex);
            vertexCoords = newArray;
            vertexBuffer = null;
        }
        vertexCoords[vertexIndex++] = x;
        vertexCoords[vertexIndex++] = y;

        numVertices++;
    }

    public void addColor(float r, float g, float b, float alpha) {
        if (colorComponents==null) {
            colorComponents = new float[20];
        }
        else if (colorIndex+4 >= colorComponents.length) {
            float[] newArray = new float[2*colorComponents.length];
            System.arraycopy(colorComponents, 0, newArray, 0, colorIndex);
            colorComponents = newArray;
            colorBuffer = null;
        }
        colorComponents[colorIndex++] = r;
        colorComponents[colorIndex++] = g;
        colorComponents[colorIndex++] = b;
        colorComponents[colorIndex++] = alpha;
    }

    public void addColor(float r, float g, float b) {
        addColor(r, g, b, 1.0f);
    }

    public void end() {
        // update buffers, (re)creating if needed
        if (vertexBuffer==null || vertexBuffer.capacity()<vertexIndex) {
            vertexBuffer = makeFloatBuffer(vertexIndex);
        }
        vertexBuffer.put(vertexCoords, 0, vertexIndex);
        vertexBuffer.position(0);

        if (colorIndex>0) {
            if (colorBuffer==null || colorBuffer.capacity()<colorIndex) {
                colorBuffer = makeFloatBuffer(colorIndex);
            }
            colorBuffer.put(colorComponents, 0, colorIndex);
            colorBuffer.position(0);
        }
    }

    public void render(GL10 gl) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        if (colorIndex>4) {
            gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
        }
        else {
            gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
        }

        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vertexBuffer);
        if (colorIndex>4) {
            gl.glColorPointer(4, GL10.GL_FLOAT, 0, colorBuffer);
        }
        else if (colorIndex==4) {
            // single color
            gl.glColor4f(
                    colorComponents[0], colorComponents[1], colorComponents[2], colorComponents[3]);
        }

        gl.glDrawArrays(glMode, 0, numVertices);
    }

    public FloatBuffer getVertexBuffer() {
        return vertexBuffer;
    }
    public FloatBuffer getColorBuffer() {
        return colorBuffer;
    }

    public int getVertexCount() {
        return numVertices;
    }

    static FloatBuffer makeFloatBuffer(int size) {
        ByteBuffer vbb = ByteBuffer.allocateDirect(size * 4);
        vbb.order(ByteOrder.nativeOrder());
        FloatBuffer texBuffer = vbb.asFloatBuffer();
        texBuffer.position(0);
        return texBuffer;
    }


}
