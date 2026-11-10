package com.dozingcatsoftware.vectorpinball.model;

public abstract class Shape implements IDrawable {
    int layer;
    int color;
    Integer inactiveLayerColor;

    static float f32(double d) {
        return (float) d;
    }

    @Override public int getLayer() {
        return layer;
    }

    int colorToDraw(Field field) {
        if (inactiveLayerColor == null) {
            return color;
        }
        return (field.hasBallAtLayer(this.getLayer())) ? color : inactiveLayerColor;
    }

    public enum FillType {OUTLINE, SOLID}

    public static class Line extends Shape {
        private double x1, y1, x2, y2;

        private Line() {}

        public static Line create(
                double x1, double y1, double x2, double y2,
                int layer, int color, Integer inactiveLayerColor) {
            Line self = new Line();
            self.layer = layer;
            self.color = color;
            self.inactiveLayerColor = inactiveLayerColor;
            self.x1 = x1;
            self.y1 = y1;
            self.x2 = x2;
            self.y2 = y2;
            return self;
        }

        @Override public void draw(Field field, IFieldRenderer renderer) {
            renderer.drawLine(f32(x1), f32(y1), f32(x2), f32(y2), colorToDraw(field));
        }
    }

    public static class Arc extends Shape {
        private double cx, cy, xRadius, yRadius;
        private double startAngle, endAngle;  // radians
        private float[] xEndpoints, yEndpoints;

        private Arc() {}

        public static Arc create(
                double cx, double cy, double xRadius, double yRadius,
                double startAngleDegrees, double endAngleDegrees,
                int layer, int color, Integer inactiveLayerColor) {
            Arc self = new Arc();
            self.layer = layer;
            self.color = color;
            self.inactiveLayerColor = inactiveLayerColor;
            self.cx = cx;
            self.cy = cy;
            self.xRadius = xRadius;
            self.yRadius = yRadius;
            self.startAngle = Math.toRadians(startAngleDegrees);
            self.endAngle = Math.toRadians(endAngleDegrees);
            // Precompute a polyline approximation for renderers that can't draw arcs directly.
            int numSegments = Math.max(4,
                    (int) Math.ceil(16 * Math.abs(self.endAngle - self.startAngle) / (2 * Math.PI)));
            self.xEndpoints = new float[numSegments + 1];
            self.yEndpoints = new float[numSegments + 1];
            for (int i = 0; i <= numSegments; i++) {
                double angle = self.startAngle + i * (self.endAngle - self.startAngle) / numSegments;
                self.xEndpoints[i] = f32(cx + xRadius * Math.cos(angle));
                self.yEndpoints[i] = f32(cy + yRadius * Math.sin(angle));
            }
            return self;
        }

        @Override public void draw(Field field, IFieldRenderer renderer) {
            int color = colorToDraw(field);
            if (renderer.canDrawArc()) {
                renderer.drawArc(f32(cx), f32(cy), f32(xRadius), f32(yRadius),
                        f32(startAngle), f32(endAngle), color);
            }
            else {
                renderer.drawLinePath(xEndpoints, yEndpoints, color);
            }
        }
    }

    public static class Circle extends Shape {
        private FillType fill;
        private double cx, cy, radius;

        private Circle() {}

        public static Circle create(
                double cx, double cy, double radius, FillType fill,
                int layer, int color, Integer inactiveLayerColor) {
            Circle self = new Circle();
            self.layer = layer;
            self.color = color;
            self.inactiveLayerColor = inactiveLayerColor;
            self.fill = fill;
            self.cx = cx;
            self.cy = cy;
            self.radius = radius;
            return self;
        }

        @Override public void draw(Field field, IFieldRenderer renderer) {
            if (this.fill == FillType.OUTLINE) {
                renderer.frameCircle(f32(cx), f32(cy), f32(radius), colorToDraw(field));
            }
            else {
                renderer.fillCircle(f32(cx), f32(cy), f32(radius), colorToDraw(field));
            }
        }
    }
}
