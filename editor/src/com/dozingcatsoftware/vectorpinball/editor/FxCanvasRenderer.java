package com.dozingcatsoftware.vectorpinball.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcType;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableField;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableFieldElement;
import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.model.Field;

public class FxCanvasRenderer implements IEditableFieldRenderer {

    public enum ScaleSource { PRESET, MANUAL }

    private static final double DEFAULT_SCALE = 25;
    private static final double FIELD_MARGIN = 1.5;
    // Zoom levels greater than 2 result in poor performance using the simple
    // approach of creating a larger canvas.
    private static final double[] SCALE_RATIOS = {1.0/2, 3.0/4, 1.0, 4.0/3, 2.0};
    private static final int DEFAULT_SCALE_RATIO_INDEX = 2;

    private Canvas canvas;
    private GraphicsContext context;
    private Field field;
    private EditableField editableField;
    private UndoStack undoStack;

    private double scale = DEFAULT_SCALE;
    private int scaleRatioIndex = DEFAULT_SCALE_RATIO_INDEX;
    private ScaleSource scaleSource = ScaleSource.PRESET;
    private double xOffset = -1.5;
    private double yOffset = -1.5;

    private Point dragStartPoint;
    private Point lastDragPoint;

    public void setCanvas(Canvas c) {
        canvas = c;
        context = c.getGraphicsContext2D();
    }

    public void setField(Field f) {
        field = f;
        editableField = null;
    }

    public void setEditableField(EditableField f) {
        editableField = f;
        field = null;
    }

    public void setUndoStack(UndoStack stack) {
        undoStack = stack;
    }

    private static Paint toFxPaint(int color) {
        return javafx.scene.paint.Color.rgb(
                Color.getRed(color),
                Color.getGreen(color),
                Color.getBlue(color),
                Color.getAlpha(color) / 255.0);
    }

    private double worldToPixelX(double x) {
        return scale * (x-xOffset);
    }

    private double worldToPixelY(double y) {
        return canvas.getHeight() - (scale * (y-yOffset));
    }

    private double pixelToWorldX(double x) {
        return x/scale + xOffset;
    }

    private double pixelToWorldY(double y) {
        return (canvas.getHeight()-y)/scale + yOffset;
    }

    private double worldToPixelDistance(double dist) {
        return scale * dist;
    }

    @Override public void drawLine(double x1, double y1, double x2, double y2, int color) {
        context.setStroke(toFxPaint(color));
        context.beginPath();
        context.moveTo(worldToPixelX(x1), worldToPixelY(y1));
        context.lineTo(worldToPixelX(x2), worldToPixelY(y2));
        context.stroke();
    }
    @Override public void drawLine(float x1, float y1, float x2, float y2, int color) {
        drawLine((double) x1, y1, x2, y2, color);
    }

    @Override public void drawLinePath(double[] xEndpoints, double[] yEndpoints, int color) {
        context.setStroke(toFxPaint(color));
        context.beginPath();
        context.moveTo(worldToPixelX(xEndpoints[0]), worldToPixelY(yEndpoints[0]));
        for (int i = 1; i < xEndpoints.length; i++) {
            context.lineTo(worldToPixelX(xEndpoints[i]), worldToPixelY(yEndpoints[i]));
        }
        context.stroke();
    }
    @Override public void drawLinePath(float[] xEndpoints, float[] yEndpoints, int color) {
        context.setStroke(toFxPaint(color));
        context.beginPath();
        context.moveTo(worldToPixelX(xEndpoints[0]), worldToPixelY(yEndpoints[0]));
        for (int i = 1; i < xEndpoints.length; i++) {
            context.lineTo(worldToPixelX(xEndpoints[i]), worldToPixelY(yEndpoints[i]));
        }
        context.stroke();
    }

    @Override public void fillCircle(double cx, double cy, double radius, int color) {
        context.setFill(toFxPaint(color));
        context.fillArc(worldToPixelX(cx - radius), worldToPixelY(cy + radius),
                worldToPixelDistance(radius * 2), worldToPixelDistance(radius * 2), 0, 360, ArcType.OPEN);
    }
    @Override public void fillCircle(float cx, float cy, float radius, int color) {
        fillCircle((double) cx, cy, radius, color);
    }

    @Override public void frameCircle(double cx, double cy, double radius, int color) {
        context.setStroke(toFxPaint(color));
        context.strokeArc(worldToPixelX(cx - radius), worldToPixelY(cy + radius),
                worldToPixelDistance(radius * 2), worldToPixelDistance(radius * 2), 0, 360, ArcType.OPEN);
    }
    @Override public void frameCircle(float cx, float cy, float radius, int color) {
        frameCircle((double) cx, cy, radius, color);
    }

    @Override public void fillPolygon(double[] xPoints, double[] yPoints, int color) {
        double[] pixelX = new double[xPoints.length];
        double[] pixelY = new double[yPoints.length];
        for (int i=0; i<xPoints.length; i++) {
            pixelX[i] = worldToPixelX(xPoints[i]);
            pixelY[i] = worldToPixelY(yPoints[i]);
        }
        context.setFill(toFxPaint(color));
        context.fillPolygon(pixelX, pixelY, pixelX.length);
    }

    @Override public boolean canDrawArc() {
        return true;
    }

    @Override public void drawArc(double cx, double cy, double xRadius, double yRadius,
                                  double startAngle, double endAngle, int color) {
        context.setStroke(toFxPaint(color));
        context.strokeArc(worldToPixelX(cx - xRadius), worldToPixelY(cy + yRadius),
                worldToPixelDistance(xRadius * 2), worldToPixelDistance(yRadius * 2),
                Math.toDegrees(startAngle), Math.toDegrees(endAngle - startAngle), ArcType.OPEN);
    }

    @Override public void drawArc(float cx, float cy, float xRadius, float yRadius,
                                  float startAngle, float endAngle, int color) {
        drawArc((double) cx, cy, xRadius, yRadius, startAngle, endAngle, color);
    }

    @Override public void doDraw() {
        if (Platform.isFxApplicationThread()) {
            draw();
        }
        else {
            Platform.runLater(this::draw);
        }
    }

    void draw() {
        context.setFill(javafx.scene.paint.Color.BLACK);
        context.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        // When editing, use thin lines so they can be positioned accurately.
        // When playing, use the same 1/216 ratio as the mobile app.
        if (editableField != null) {
            context.setLineWidth(1);
            editableField.drawForEditor(this);
        }
        else if (field != null) {
            context.setLineWidth(((int) Math.min(canvas.getWidth(), canvas.getHeight())) / 216);
            synchronized(field) {
                field.draw(this);
            }
        }
    }

    @Override public int getWidth() {
        return (int)canvas.getWidth();
    }

    @Override public int getHeight() {
        return (int)canvas.getHeight();
    }

    @Override public double getRelativeScale() {
        return SCALE_RATIOS[scaleRatioIndex];
    }

    public void zoomIn() {
        if (scaleRatioIndex < SCALE_RATIOS.length-1) {
            scaleRatioIndex++;
            scale = SCALE_RATIOS[scaleRatioIndex] * DEFAULT_SCALE;
            scaleSource = ScaleSource.PRESET;
        }
    }

    public void zoomOut() {
        if (scaleRatioIndex > 0) {
            scaleRatioIndex--;
            scale = SCALE_RATIOS[scaleRatioIndex] * DEFAULT_SCALE;
            scaleSource = ScaleSource.PRESET;
        }
    }

    public void zoomDefault() {
        scaleRatioIndex = DEFAULT_SCALE_RATIO_INDEX;
        scale = SCALE_RATIOS[scaleRatioIndex] * DEFAULT_SCALE;
        scaleSource = ScaleSource.PRESET;
    }

    public void setManualScale(double newScale) {
        this.scale = newScale;
        this.scaleSource = ScaleSource.MANUAL;
    }

    public double getScale() {
        return scale;
    }

    public ScaleSource getScaleSource() {
        return scaleSource;
    }

    public double getFieldWidth() {
        if (editableField != null) {
            Object w = editableField.getProperty(EditableField.WIDTH_PROPERTY);
            if (w != null) return Double.parseDouble(w.toString());
        } else if (field != null) {
            return field.getWidth();
        }
        return 20.0; // default field width
    }

    public double getFieldHeight() {
        if (editableField != null) {
            Object h = editableField.getProperty(EditableField.HEIGHT_PROPERTY);
            if (h != null) return Double.parseDouble(h.toString());
        } else if (field != null) {
            return field.getHeight();
        }
        return 30.0; // default field height
    }

    public double getCanvasWidthForScale(double scale) {
        return scale * (getFieldWidth() + 2 * FIELD_MARGIN);
    }

    public double getCanvasHeightForScale(double scale) {
        return scale * (getFieldHeight() + 2 * FIELD_MARGIN);
    }

    public double getScaleForCanvasHeight(double canvasHeight) {
        return canvasHeight / (getFieldHeight() + 2 * FIELD_MARGIN);
    }

    public double getFieldAspectRatio() {
        double w = getFieldWidth() + 2 * FIELD_MARGIN;
        double h = getFieldHeight() + 2 * FIELD_MARGIN;
        return w / h;
    }

    private Point worldPointFromEvent(MouseEvent event) {
        return Point.fromXY(pixelToWorldX(event.getX()), pixelToWorldY(event.getY()));
    }

    private Point worldOffsetFromPixels(int dx, int dy) {
        Point worldZero = Point.fromXY(pixelToWorldX(0), pixelToWorldY(0));
        Point worldTranslated = Point.fromXY(pixelToWorldX(dx), pixelToWorldY(dy));
        return worldTranslated.subtract(worldZero);
    }

    private boolean isElementInClickRange(EditableFieldElement elem, Point point) {
        double distance = 10.0 / this.scale;
        return elem.isPointWithinDistance(point, distance);
    }

    private EditableFieldElement findClickTarget(Point worldPoint) {
        // Give priority to already-selected elements.
        for (EditableFieldElement elem : editableField.getSelectedElements()) {
            if (isElementInClickRange(elem, worldPoint)) {
                return elem;
            }
        }
        for (EditableFieldElement elem : editableField.getElements()) {
            if (isElementInClickRange(elem, worldPoint)) {
                return elem;
            }
        }
        return null;
    }

    private void selectFirstElementAtPoint(Point point) {
        List<EditableFieldElement> selected = new ArrayList<>();
        if (editableField == null) return;
        EditableFieldElement clickedElement = findClickTarget(point);
        if (clickedElement != null) {
            selected.add(clickedElement);
            clickedElement.startDrag(point);
        }
        this.editableField.setSelectedElements(selected);
        this.dragStartPoint = (selected.isEmpty()) ? null : point;
        this.lastDragPoint = null;
        draw();
    }

    private void selectNextElementAtPoint(EditableFieldElement afterElem, Point point) {
        List<EditableFieldElement> available = editableField.getElements().stream()
                .filter(elem -> isElementInClickRange(elem, point))
                .collect(Collectors.toList());
        int index = available.indexOf(afterElem);
        if (index == -1 || available.size() <= 1) {
            return;
        }
        this.editableField.setSelectedElements(
                Collections.singleton(available.get((index + 1) % available.size())));
        draw();
    }

    public void handleEditorMouseDown(MouseEvent event) {
        switch (event.getButton()) {
            case PRIMARY:
                Point worldPoint = worldPointFromEvent(event);
                selectFirstElementAtPoint(worldPoint);
                break;
            case SECONDARY:
                if (this.dragStartPoint != null && editableField.getSelectedElements().size() == 1) {
                    EditableFieldElement sel = (EditableFieldElement) editableField.getSelectedElements().toArray()[0];
                    selectNextElementAtPoint(sel, this.dragStartPoint);
                }
                break;
            default:
                break;
        }
    }

    public void handleEditorMouseDrag(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        if (editableField != null && editableField.hasSelection() && dragStartPoint!=null) {
            Point worldPoint = worldPointFromEvent(event);
            Point totalDragOffset = worldPoint.subtract(dragStartPoint);
            Point previousDragOffset = worldPoint.subtract((lastDragPoint!=null) ? lastDragPoint : dragStartPoint);
            for (EditableFieldElement elem : editableField.getSelectedElements()) {
                elem.handleDrag(dragStartPoint, totalDragOffset, previousDragOffset);
            }
            lastDragPoint = worldPoint;
            draw();
        }
    }

    public void handleEditorMouseUp(MouseEvent event) {
        if (editableField != null && editableField.hasSelection() && dragStartPoint!=null) {
            undoStack.pushSnapshot();
        }
    }

    public void moveSelectionByPixels(int dx, int dy) {
        if (editableField.hasSelection()) {
            Point offset = worldOffsetFromPixels(dx, dy);
            for (EditableFieldElement elem : editableField.getSelectedElements()) {
                elem.translate(offset);
            }
            draw();
        }
    }
}
