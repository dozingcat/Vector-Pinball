package com.dozingcatsoftware.bouncy.util;

import javax.microedition.khronos.opengles.GL10;

public class GLVertexListManager {
	
	GLVertexList[] vertexLists;
	int[] glModes;
	int vertexListCount;
	
	public GLVertexListManager() {
		vertexLists = new GLVertexList[10];
		for(int i=0; i<vertexLists.length; i++) {
			vertexLists[i] = new GLVertexList();
		}
	}
	
	public void begin() {
		vertexListCount = 0;
	}
	
	public void end() {
		for(int i=0; i<vertexListCount; i++) {
			vertexLists[i].end();
		}
	}
	
	public void render(GL10 gl) {
		for(int i=0; i<vertexListCount; i++) {
			vertexLists[i].render(gl);
		}
	}

	public GLVertexList addVertexListForMode(int glMode) {
		if (vertexListCount>=vertexLists.length) {
			GLVertexList[] newArray = new GLVertexList[2*vertexLists.length];
			System.arraycopy(vertexLists, 0, newArray, 0, vertexLists.length);
			this.vertexLists = newArray;
		}
		GLVertexList vl = vertexLists[vertexListCount];
		if (vl==null) {
			vertexLists[vertexListCount] = vl = new GLVertexList();
		}
		vl.setGLMode(glMode);
		vl.begin();
		vertexListCount++;
		return vl;
	}
}
