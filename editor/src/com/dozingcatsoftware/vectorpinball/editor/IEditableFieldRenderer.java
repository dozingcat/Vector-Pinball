package com.dozingcatsoftware.vectorpinball.editor;

import com.dozingcatsoftware.vectorpinball.model.IFieldRenderer;

public interface IEditableFieldRenderer extends IFieldRenderer {

    double getRelativeScale();

    void fillPolygon(double[] xPoints, double[] yPoints, int color);
}
