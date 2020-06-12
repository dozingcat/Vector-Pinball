package com.dozingcatsoftware.bouncy;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;

import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.model.IFieldRenderer;

public class CanvasFieldView extends SurfaceView implements IFieldRenderer.FloatOnlyRenderer {

    public CanvasFieldView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
    }

    FieldViewManager manager;

    Paint paint = new Paint();
    RectF rect = new RectF();

    {
        paint.setAntiAlias(true);
    }

    Canvas canvas;

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

    /**
     * Main draw method, called from FieldDriver's game thread. Calls each FieldElement's draw()
     * method passing itself as the IFieldRenderer implementation.
     */
    @Override public void doDraw() {
        Canvas c = this.getHolder().lockCanvas();
        if (c == null) return;
        c.drawARGB(255, 0, 0, 0);
        paint.setStrokeWidth(manager.getLineWidth());
        this.canvas = c;
        try {
            manager.getField().draw(this);
        }
        finally {
            this.getHolder().unlockCanvasAndPost(c);
        }
    }

    // Implementation of IFieldRenderer drawing methods that FieldElement classes can call.
    // Assumes cacheScaleAndOffsets has been called.
    @Override public void drawLine(float x1, float y1, float x2, float y2, int color) {
        this.paint.setColor(Color.toARGB(color));
        this.canvas.drawLine(
                manager.world2pixelX(x1), manager.world2pixelY(y1),
                manager.world2pixelX(x2), manager.world2pixelY(y2),
                this.paint);
    }

    @Override public void drawLinePath(float[] xEndpoints, float[] yEndpoints, int color) {
        this.paint.setColor(Color.toARGB(color));
        float x1 = manager.world2pixelX(xEndpoints[0]);
        float y1 = manager.world2pixelY(yEndpoints[0]);
        for (int i = 1; i < xEndpoints.length; i++) {
            float x2 = manager.world2pixelX(xEndpoints[i]);
            float y2 = manager.world2pixelY(yEndpoints[i]);
            this.canvas.drawLine(x1, y1, x2, y2, this.paint);
            x1 = x2;
            y1 = y2;
        }
    }

    @Override public void fillCircle(float cx, float cy, float radius, int color) {
        drawCircle(cx, cy, radius, color, Paint.Style.FILL);
    }

    @Override public void frameCircle(float cx, float cy, float radius, int color) {
        drawCircle(cx, cy, radius, color, Paint.Style.STROKE);
    }

    void drawCircle(float cx, float cy, float radius, int color, Paint.Style style) {
        this.paint.setColor(Color.toARGB(color));
        this.paint.setStyle(style);
        float rad = radius * manager.getCachedScale();
        this.canvas.drawCircle(manager.world2pixelX(cx), manager.world2pixelY(cy), rad, paint);
    }

    @Override public boolean canDrawArc() {
        return true;
    }

    @Override public void drawArc(float cx, float cy, float xRadius, float yRadius,
            float startAngle, float endAngle, int color) {
        // Android drawArc draws in degrees clockwise with 0 at the top. Arguments to this function
        // are in radians, counterclockwise with 0 to the right.
        this.paint.setColor(Color.toARGB(color));
        this.paint.setStyle(Paint.Style.STROKE);
        float wcx = manager.world2pixelX(cx);
        float wcy = manager.world2pixelY(cy);
        float wxrad = xRadius * manager.getCachedScale();
        float wyrad = yRadius * manager.getCachedScale();
        this.rect.set(wcx - wxrad, wcy - wyrad, wcx + wxrad, wcy + wyrad);
        float startDegrees = (float) (360 - Math.toDegrees(endAngle));
        float sweepDegrees = (float) Math.toDegrees(endAngle - startAngle);
        this.canvas.drawArc(this.rect, startDegrees, sweepDegrees, false, paint);
    }
}
