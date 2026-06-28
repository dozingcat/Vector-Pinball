package com.dozingcatsoftware.bouncy;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.model.Field;
import com.dozingcatsoftware.vectorpinball.model.IFieldRenderer;

/**
 * Draws a static image of a {@link Field}'s layout into a {@link Bitmap}, used for the table
 * selection grid. Unlike {@link CanvasFieldView}, this does its own simple world-to-pixel transform
 * with the whole table scaled to fit and centered, so it doesn't depend on a
 * {@link FieldViewManager} or any zoom/touch state.
 */
public class TableThumbnailRenderer {

    /** Renders {@code field} (already loaded via resetForLayoutMap) into a new bitmap. */
    public static Bitmap render(Field field, int widthPx, int heightPx) {
        Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawARGB(255, 0, 0, 0);
        BitmapFieldRenderer renderer = new BitmapFieldRenderer(
                canvas, widthPx, heightPx, field.getWidth(), field.getHeight());
        field.draw(renderer);
        return bitmap;
    }

    private static class BitmapFieldRenderer implements IFieldRenderer.FloatOnlyRenderer {
        private final Canvas canvas;
        private final int widthPx;
        private final int heightPx;
        private final float scale;
        private final float xOffsetPx;
        private final float yOffsetPx;
        private final Paint paint = new Paint();
        private final RectF rect = new RectF();

        BitmapFieldRenderer(Canvas canvas, int widthPx, int heightPx,
                float fieldWidth, float fieldHeight) {
            this.canvas = canvas;
            this.widthPx = widthPx;
            this.heightPx = heightPx;
            this.scale = Math.min(widthPx / fieldWidth, heightPx / fieldHeight);
            this.xOffsetPx = (widthPx - fieldWidth * scale) / 2f;
            this.yOffsetPx = (heightPx - fieldHeight * scale) / 2f;
            paint.setAntiAlias(true);
            paint.setStrokeWidth(Math.max(1.5f, widthPx / 150f));
        }

        private float world2pixelX(float x) {
            return xOffsetPx + x * scale;
        }

        // In world coordinates positive y is up; in pixel coordinates positive y is down.
        private float world2pixelY(float y) {
            return heightPx - yOffsetPx - y * scale;
        }

        @Override public void drawLine(float x1, float y1, float x2, float y2, int color) {
            paint.setColor(Color.toARGB(color));
            canvas.drawLine(world2pixelX(x1), world2pixelY(y1),
                    world2pixelX(x2), world2pixelY(y2), paint);
        }

        @Override public void drawLinePath(float[] xEndpoints, float[] yEndpoints, int color) {
            paint.setColor(Color.toARGB(color));
            float x1 = world2pixelX(xEndpoints[0]);
            float y1 = world2pixelY(yEndpoints[0]);
            for (int i = 1; i < xEndpoints.length; i++) {
                float x2 = world2pixelX(xEndpoints[i]);
                float y2 = world2pixelY(yEndpoints[i]);
                canvas.drawLine(x1, y1, x2, y2, paint);
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

        private void drawCircle(float cx, float cy, float radius, int color, Paint.Style style) {
            paint.setColor(Color.toARGB(color));
            paint.setStyle(style);
            canvas.drawCircle(world2pixelX(cx), world2pixelY(cy), radius * scale, paint);
        }

        @Override public boolean canDrawArc() {
            return true;
        }

        @Override public void drawArc(float cx, float cy, float xRadius, float yRadius,
                float startAngle, float endAngle, int color) {
            // Mirrors CanvasFieldView.drawArc: Android drawArc uses degrees clockwise with 0 at the
            // top, while the arguments are radians counterclockwise with 0 to the right.
            paint.setColor(Color.toARGB(color));
            paint.setStyle(Paint.Style.STROKE);
            float wcx = world2pixelX(cx);
            float wcy = world2pixelY(cy);
            float wxrad = xRadius * scale;
            float wyrad = yRadius * scale;
            rect.set(wcx - wxrad, wcy - wyrad, wcx + wxrad, wcy + wyrad);
            float startDegrees = (float) (360 - Math.toDegrees(endAngle));
            float sweepDegrees = (float) Math.toDegrees(endAngle - startAngle);
            canvas.drawArc(rect, startDegrees, sweepDegrees, false, paint);
        }

        @Override public void doDraw() {}

        @Override public int getWidth() {
            return widthPx;
        }

        @Override public int getHeight() {
            return heightPx;
        }
    }
}
