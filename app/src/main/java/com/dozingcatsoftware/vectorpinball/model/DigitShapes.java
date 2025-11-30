package com.dozingcatsoftware.vectorpinball.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Creates digit representations (0-9) using Shape.Line primitives, using a 7-segment style.
 */
public class DigitShapes {
    enum Seg {
        Top, UpperLeft, UpperRight, Middle, LowerLeft, LowerRight, Bottom,
    }

    static Set<Seg>[] DIGIT_SEGMENTS = new Set[] {
            /* 0 */ Set.of(Seg.Top, Seg.UpperLeft, Seg.UpperRight, Seg.LowerLeft, Seg.LowerRight, Seg.Bottom),
            /* 1 */ Set.of(Seg.UpperRight, Seg.LowerRight),
            /* 2 */ Set.of(Seg.Top, Seg.UpperRight, Seg.Middle, Seg.LowerLeft, Seg.Bottom),
            /* 3 */ Set.of(Seg.Top, Seg.UpperRight, Seg.Middle, Seg.LowerRight, Seg.Bottom),
            /* 4 */ Set.of(Seg.UpperLeft, Seg.UpperRight, Seg.Middle, Seg.LowerRight),
            /* 5 */ Set.of(Seg.Top, Seg.UpperLeft, Seg.Middle, Seg.LowerRight, Seg.Bottom),
            /* 6 */ Set.of(Seg.Top, Seg.UpperLeft, Seg.Middle, Seg.LowerLeft, Seg.LowerRight, Seg.Bottom),
            /* 7 */ Set.of(Seg.Top, Seg.UpperRight, Seg.LowerRight),
            /* 8 */ Set.of(Seg.Top, Seg.UpperLeft, Seg.UpperRight, Seg.Middle, Seg.LowerLeft, Seg.LowerRight, Seg.Bottom),
            /* 9 */ Set.of(Seg.Top, Seg.UpperLeft, Seg.Middle, Seg.UpperRight, Seg.LowerRight, Seg.Bottom),
    };
    
    /**
     * Creates a list of Shape objects representing the specified digit.
     * @param digit The digit to create (0-9)
     * @param centerX X coordinate of the digit center
     * @param centerY Y coordinate of the digit center
     * @param layer Layer for rendering order
     * @param color Color of the digit
     * @return List of Shape objects that form the digit
     */
    public static List<Shape> createDigit(
            int digit,
            double centerX,
            double centerY,
            double width,
            double height,
            int layer,
            int color) {
        List<Shape> shapes = new ArrayList<>();
        
        if (digit < 0 || digit > 9) {
            return shapes; // Return empty list for invalid digits
        }

        // Leave gaps between segments. Each vertical segments only covers half the total height.
        double segmentWidth = width * 0.7;
        double segmentHeight = height * 0.4;
        
        // Define the 7 segments of a digit display
        Set<Seg> segments = DIGIT_SEGMENTS[digit];
        
        if (segments.contains(Seg.Top)) { // Top horizontal
            shapes.add(createHorizontalSegment(centerX, centerY + height/2, segmentWidth, layer, color));
        }
        if (segments.contains(Seg.UpperRight)) { // Top right vertical  
            shapes.add(createVerticalSegment(centerX + width/2, centerY + height/4, segmentHeight, layer, color));
        }
        if (segments.contains(Seg.LowerRight)) { // Bottom right vertical
            shapes.add(createVerticalSegment(centerX + width/2, centerY - height/4, segmentHeight, layer, color));
        }
        if (segments.contains(Seg.Bottom)) { // Bottom horizontal
            shapes.add(createHorizontalSegment(centerX, centerY - height/2, segmentWidth, layer, color));
        }
        if (segments.contains(Seg.LowerLeft)) { // Bottom left vertical
            shapes.add(createVerticalSegment(centerX - width/2, centerY - height/4, segmentHeight, layer, color));
        }
        if (segments.contains(Seg.UpperLeft)) { // Top left vertical
            shapes.add(createVerticalSegment(centerX - width/2, centerY + height/4, segmentHeight, layer, color));
        }
        if (segments.contains(Seg.Middle)) { // Middle horizontal
            shapes.add(createHorizontalSegment(centerX, centerY, segmentWidth, layer, color));
        }
        
        return shapes;
    }
    
    private static Shape createHorizontalSegment(double centerX, double centerY, double segmentWidth, int layer, int color) {
        return Shape.Line.create(
            centerX - segmentWidth / 2, centerY,
            centerX + segmentWidth / 2, centerY,
            layer, color, null
        );
    }
    
    private static Shape createVerticalSegment(double centerX, double centerY, double segmentHeight, int layer, int color) {
        return Shape.Line.create(
            centerX, centerY - segmentHeight / 2,
            centerX, centerY + segmentHeight / 2,
            layer, color, null
        );
    }
}
