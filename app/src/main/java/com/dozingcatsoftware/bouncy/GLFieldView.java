package com.dozingcatsoftware.bouncy;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

public class GLFieldView extends GLSurfaceView {

    // This view class just handles input events. OpenGL calls to draw field elements are done in
    // GL10Renderer and GL20Renderer.
    public GLFieldView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
    }

    FieldViewManager manager;

    public void setManager(FieldViewManager value) {
        this.manager = value;
    }

    /**
     * Called when the view is touched. Activates flippers, starts a new game if one is not in
     * progress, and launches a ball if one is not in play.
     */
    @Override public boolean onTouchEvent(MotionEvent event) {
        return manager.handleTouchEvent(event);
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        return manager.handleKeyDown(keyCode, event);
    }

    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        return manager.handleKeyUp(keyCode, event);
    }
}
