package com.dozingcatsoftware.bouncy.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

public class GL2DRenderer {
	
	FloatBuffer vertexBuffer;
	FloatBuffer colorBuffer;
	int numVertices;
	
	int vertexIndex;
	int colorIndex;
	
	List<Float> vertexCoords = new ArrayList<Float>();
	List<Float> colorComponents = new ArrayList<Float>();
	
	public void begin() {
		numVertices = 0;
		vertexIndex = colorIndex = 0;
	}

	public void addVertex(float x, float y) {
		if (vertexIndex < vertexCoords.size()) {
			vertexCoords.set(vertexIndex, x);
			vertexCoords.set(vertexIndex+1, y);
		}
		else {
			vertexCoords.add(x);
			vertexCoords.add(y);
		}
		
		vertexIndex += 2;
		numVertices += 1;
	}
	
	public void addColor(float r, float g, float b) {
		if (colorIndex < colorComponents.size()) {
			colorComponents.set(colorIndex, r);
			colorComponents.set(colorIndex+1, g);
			colorComponents.set(colorIndex+2, b);
			colorComponents.set(colorIndex+3, 1.0f);
		}
		else {
			colorComponents.add(r);
			colorComponents.add(g);
			colorComponents.add(b);
			colorComponents.add(1.0f);
		}
		colorIndex += 4;
	}
	
	public void end() {
		// update buffers, (re)creating if needed
		if (vertexBuffer==null || vertexBuffer.capacity()<vertexIndex) {
			vertexBuffer = makeFloatBuffer(vertexIndex);
		}
		for(int i=0; i<vertexIndex; i++) {
			vertexBuffer.put(i, vertexCoords.get(i));
		}
		vertexBuffer.position(0);
		
		if (colorIndex>0 && (colorBuffer==null || colorBuffer.capacity()<colorIndex)) {
			colorBuffer = makeFloatBuffer(colorIndex);
			for(int i=0; i<colorIndex; i++) {
				colorBuffer.put(i, colorComponents.get(i));
			}
		}
		if (colorBuffer!=null) colorBuffer.position(0);
	}
	
	public void render(GL10 gl, int mode) {
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		if (colorIndex>0) {
			gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
		}
		else {
			gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
		}
		
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vertexBuffer);
		if (colorIndex>0) {
			gl.glColorPointer(4, GL10.GL_FLOAT, 0, colorBuffer);
		}
		
		gl.glDrawArrays(mode, 0, numVertices);
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
