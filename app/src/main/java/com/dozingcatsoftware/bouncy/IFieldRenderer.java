package com.dozingcatsoftware.bouncy;

/**
 * This interface defines methods that draw graphical elements such as lines as circles to display
 * the field. An implementation of this interface is passed to FieldElement objects so they can draw
 * themselves without depending directly on Android UI classes.
 */
public interface IFieldRenderer {

    void setManager(FieldViewManager manager);

    void drawLine(float x1, float y1, float x2, float y2, Color color);

    void fillCircle(float cx, float cy, float radius, Color color);

    void frameCircle(float cx, float cy, float radius, Color color);

    void doDraw();

    int getWidth();

    int getHeight();
}
