package com.dozingcatsoftware.vectorpinball.model;

/**
 * This interface defines methods that draw graphical elements such as lines and circles to display
 * the field. An implementation of this interface is passed to FieldElement objects so they can draw
 * themselves without depending directly on platform UI classes.
 */
public interface IFieldRenderer {
    // Some UI libraries (e.g. JavaFX) use doubles for coordinates, and some (e.g. Android) use
    // floats. As an inelegant compromise support both in this interface, and have sub-interfaces
    // that throw exceptions if you pass the wrong type.
    void drawLine(float x1, float y1, float x2, float y2, int color);
    void drawLine(double x1, double y1, double x2, double y2, int color);

    void drawLinePath(float[] xEndpoints, float[] yEndpoints, int color);
    void drawLinePath(double[] xEndpoints, double[] yEndpoints, int color);

    void fillCircle(float cx, float cy, float radius, int color);
    void fillCircle(double cx, double cy, double radius, int color);

    void frameCircle(float cx, float cy, float radius, int color);
    void frameCircle(double cx, double cy, double radius, int color);

    default boolean canDrawArc() {
        return false;
    }

    default void drawArc(float cx, float cy, float xRadius, float yRadius,
                 float startAngle, float sweepAngle, int color) {}
    default void drawArc(double cx, double cy, double xRadius, double yRadius,
                         double startAngle, double sweepAngle, int color) {}

    void doDraw();

    int getWidth();

    int getHeight();

    interface FloatOnlyRenderer extends IFieldRenderer {
        default void drawLine(double x1, double y1, double x2, double y2, int color) {
            throw new UnsupportedOperationException("double arguments not supported");
        }

        default void drawLinePath(double[] xEndpoints, double[] yEndpoints, int color) {
            throw new UnsupportedOperationException("double arguments not supported");
        }

        default void fillCircle(double cx, double cy, double radius, int color) {
            throw new UnsupportedOperationException("double arguments not supported");
        }

        default void frameCircle(double cx, double cy, double radius, int color) {
            throw new UnsupportedOperationException("double arguments not supported");
        }

        default void drawArc(double cx, double cy, double xRadius, double yRadius,
                                     double startAngle, double sweepAngle, int color) {
            throw new UnsupportedOperationException("double arguments not supported");
        }
    }
}
