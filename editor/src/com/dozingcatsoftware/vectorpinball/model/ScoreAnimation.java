package com.dozingcatsoftware.vectorpinball.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an animated floating score display that moves upward and fades out over time.
 * Implements IDrawable so it can be rendered alongside other game elements.
 */
public class ScoreAnimation implements IDrawable {
    private final long score;
    private final double startX, startY;
    private final Double minX, maxX;
    private final long startTimeNanos;
    private final long durationNanos;
    private final int layer;
    private final int baseColor;
    
    private final List<Shape> digitShapes;
    private boolean expired = false;
    
    private static final long DEFAULT_DURATION_NANOS = 2_000_000_000L; // 2 seconds
    private static final double FLOAT_DISTANCE = 3.0; // World units to float upward
    private static final double DIGIT_WIDTH = 0.45;
    private static final double DIGIT_HEIGHT = 0.75;
    private static final double DIGIT_SPACING = 0.675; // Spacing between digits
    
    /**
     * Creates a new score animation.
     * @param score The score value to display
     * @param x Starting X position (typically ball position)
     * @param y Starting Y position (typically ball position)
     * @param currentTimeNanos Current game time in nanoseconds
     * @param layer Rendering layer (higher values render on top)
     * @param color Base color for the score text
     * @param minX Optional minimum X position for all digits
     * @param maxX Optional maximum X position for all digits
     */
    public ScoreAnimation(
            long score,
            double x,
            double y,
            long currentTimeNanos,
            int layer,
            int color,
            Double minX,
            Double maxX) {
        this.score = score;
        this.startX = x;
        this.startY = y;
        this.startTimeNanos = currentTimeNanos;
        this.durationNanos = DEFAULT_DURATION_NANOS;
        this.layer = layer;
        this.baseColor = color;
        this.minX = minX;
        this.maxX = maxX;
        
        this.digitShapes = new ArrayList<>();
        createDigitShapes(startY, baseColor, 1.0);
    }
    
    private void createDigitShapes(double currentY, int currentColor, double scale) {
        digitShapes.clear();
        String scoreText = String.valueOf(score);
        double totalWidth = scale * ((scoreText.length() - 1) * DIGIT_SPACING + DIGIT_WIDTH);
        double startX = this.startX - totalWidth / 2;
        if (startX < minX) {
            startX = minX;
        }
        else if (startX + totalWidth > maxX) {
            startX = maxX - totalWidth;
        }
        
        for (int i = 0; i < scoreText.length(); i++) {
            int digit = Character.getNumericValue(scoreText.charAt(i));
            // DigitShape.createDigit takes the center point so we add half the width.
            double digitX = startX + ((i * DIGIT_SPACING + DIGIT_WIDTH / 2) * scale);
            digitShapes.addAll(DigitShapes.createDigit(
                    digit,
                    digitX,
                    currentY,
                    DIGIT_WIDTH * scale,
                    DIGIT_HEIGHT * scale,
                    layer,
                    currentColor));
        }
    }
    
    /**
     * Updates the animation state based on current time. Over the animation duration the
     * score digits move up, shrink, and have reduced opacity.
     */
    public void updateAnimation(long currentTimeNanos) {
        if (currentTimeNanos >= startTimeNanos + durationNanos) {
            expired = true;
            return;
        }
        
        double progress = (double)(currentTimeNanos - startTimeNanos) / durationNanos;
        double currentY = startY + (FLOAT_DISTANCE * progress);

        // Shrink to 50% of maximum size at the end of the animation.
        double scale = 1 - 0.5 * progress;
        // Fade out in the last 50% of animation.
        double alpha = progress < 0.5 ? 1.0 : (1.0 - (progress - 0.5) * 2.0);
        int currentColor = applyAlpha(baseColor, alpha);
        
        // Update all digit shapes with new position and color
        createDigitShapes(currentY, currentColor, scale);
    }
    
    private int applyAlpha(int color, double alpha) {
        int a = (int) Math.round(255 * Math.max(0.0, Math.min(1.0, alpha)));
        return Color.withAlpha(color, a);
    }
    
    /**
     * @return true if the animation has completed and should be removed
     */
    public boolean isExpired() {
        return expired;
    }
    
    @Override
    public void draw(Field field, IFieldRenderer renderer) {
        for (Shape shape : digitShapes) {
            shape.draw(field, renderer);
        }
    }
    
    @Override
    public int getLayer() {
        return layer;
    }
}
