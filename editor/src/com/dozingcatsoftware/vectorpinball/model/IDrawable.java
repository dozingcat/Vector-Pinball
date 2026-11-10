package com.dozingcatsoftware.vectorpinball.model;

/**
 * Anything that can be drawn, including field elements, balls, and static shapes.
 */
public interface IDrawable {
    void draw(Field field, IFieldRenderer renderer);
    int getLayer();
}
