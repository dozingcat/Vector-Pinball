package com.dozingcatsoftware.bouncy;

public interface IDrawable {
    void draw(Field field, IFieldRenderer renderer);
    int getLayer();
}
