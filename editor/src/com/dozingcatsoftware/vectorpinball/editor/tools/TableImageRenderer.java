package com.dozingcatsoftware.vectorpinball.editor.tools;

import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asDouble;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

import javax.imageio.ImageIO;

import com.dozingcatsoftware.vectorpinball.editor.IEditableFieldRenderer;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableField;
import com.dozingcatsoftware.vectorpinball.groovy.GroovyFieldDelegateBuilder;
import com.dozingcatsoftware.vectorpinball.model.AudioPlayer;
import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.model.Field;
import com.dozingcatsoftware.vectorpinball.util.JSONUtils;

/**
 * Command line tool that renders a table JSON file to a PNG image. By default it renders the
 * editor view using the same drawForEditor() code as the editor UI. With "--game" it instead
 * starts a game (running the table's script, so script-driven display state is applied) and
 * renders the game view. Usage:
 *
 *   TableImageRenderer input.json output.png [imageHeightInPixels] [--game]
 */
public class TableImageRenderer {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println(
                    "Usage: TableImageRenderer input.json output.png [imageHeightInPixels] [--game]");
            System.exit(1);
        }
        String json = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        Map<String, Object> fieldMap = JSONUtils.mapFromJSONString(json);
        boolean gameMode = Arrays.asList(args).contains("--game");

        double worldWidth = asDouble(fieldMap.get(EditableField.WIDTH_PROPERTY), 20);
        double worldHeight = asDouble(fieldMap.get(EditableField.HEIGHT_PROPERTY), 30);
        int pixelHeight = (args.length > 2 && !args[2].startsWith("--"))
                ? Integer.parseInt(args[2]) : 1200;
        double scale = pixelHeight / worldHeight;
        int pixelWidth = (int) Math.ceil(worldWidth * scale);

        BufferedImage image = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(java.awt.Color.BLACK);
        g.fillRect(0, 0, pixelWidth, pixelHeight);
        g.setStroke(new BasicStroke(1.5f));
        ImageRenderer renderer = new ImageRenderer(g, scale, pixelWidth, pixelHeight);

        if (gameMode) {
            Field field = new Field(System::currentTimeMillis, (key, params) -> key,
                    AudioPlayer.NoOpPlayer.getInstance());
            field.resetForLayoutMap(fieldMap, f -> {
                String script = f.getScriptText();
                if (script != null && script.trim().length() > 0) {
                    return GroovyFieldDelegateBuilder.createFromScript(
                            script, TableImageRenderer.class.getClassLoader());
                }
                return Field.createDelegateFromLayoutClass(f);
            });
            field.startGame();
            field.tick(16_666_667L, 4);
            field.draw(renderer);
        }
        else {
            EditableField field = new EditableField();
            field.initFromProperties(fieldMap);
            field.drawForEditor(renderer);
        }
        g.dispose();
        ImageIO.write(image, "png", new File(args[1]));
        System.out.println("Wrote " + pixelWidth + "x" + pixelHeight + " image to " + args[1]);
    }

    /**
     * Renders to a Graphics2D using the same world-to-pixel mapping as FxCanvasRenderer:
     * uniform scale with the y axis flipped so that world y increases upward.
     */
    static class ImageRenderer implements IEditableFieldRenderer {
        private final Graphics2D g;
        private final double scale;
        private final int width;
        private final int height;

        ImageRenderer(Graphics2D g, double scale, int width, int height) {
            this.g = g;
            this.scale = scale;
            this.width = width;
            this.height = height;
        }

        private double px(double x) {
            return scale * x;
        }

        private double py(double y) {
            return height - (scale * y);
        }

        private double pd(double dist) {
            return scale * dist;
        }

        private void setColor(int color) {
            g.setColor(new java.awt.Color(Color.getRed(color), Color.getGreen(color),
                    Color.getBlue(color), Color.getAlpha(color)));
        }

        @Override public void drawLine(double x1, double y1, double x2, double y2, int color) {
            setColor(color);
            g.draw(new Line2D.Double(px(x1), py(y1), px(x2), py(y2)));
        }

        @Override public void drawLine(float x1, float y1, float x2, float y2, int color) {
            drawLine((double) x1, y1, x2, y2, color);
        }

        @Override public void drawLinePath(double[] xEndpoints, double[] yEndpoints, int color) {
            setColor(color);
            Path2D.Double path = new Path2D.Double();
            path.moveTo(px(xEndpoints[0]), py(yEndpoints[0]));
            for (int i = 1; i < xEndpoints.length; i++) {
                path.lineTo(px(xEndpoints[i]), py(yEndpoints[i]));
            }
            g.draw(path);
        }

        @Override public void drawLinePath(float[] xEndpoints, float[] yEndpoints, int color) {
            double[] xs = new double[xEndpoints.length];
            double[] ys = new double[yEndpoints.length];
            for (int i = 0; i < xs.length; i++) {
                xs[i] = xEndpoints[i];
                ys[i] = yEndpoints[i];
            }
            drawLinePath(xs, ys, color);
        }

        @Override public void fillCircle(double cx, double cy, double radius, int color) {
            setColor(color);
            g.fill(new Ellipse2D.Double(px(cx - radius), py(cy + radius), pd(radius * 2), pd(radius * 2)));
        }

        @Override public void fillCircle(float cx, float cy, float radius, int color) {
            fillCircle((double) cx, cy, radius, color);
        }

        @Override public void frameCircle(double cx, double cy, double radius, int color) {
            setColor(color);
            g.draw(new Ellipse2D.Double(px(cx - radius), py(cy + radius), pd(radius * 2), pd(radius * 2)));
        }

        @Override public void frameCircle(float cx, float cy, float radius, int color) {
            frameCircle((double) cx, cy, radius, color);
        }

        @Override public boolean canDrawArc() {
            return true;
        }

        @Override public void drawArc(double cx, double cy, double xRadius, double yRadius,
                double startAngle, double endAngle, int color) {
            setColor(color);
            // AWT and JavaFX both measure arc angles counterclockwise from "east" in a y-up sense,
            // so this matches FxCanvasRenderer.drawArc.
            g.draw(new Arc2D.Double(px(cx - xRadius), py(cy + yRadius), pd(xRadius * 2), pd(yRadius * 2),
                    Math.toDegrees(startAngle), Math.toDegrees(endAngle - startAngle), Arc2D.OPEN));
        }

        @Override public void drawArc(float cx, float cy, float xRadius, float yRadius,
                float startAngle, float endAngle, int color) {
            drawArc((double) cx, cy, xRadius, yRadius, startAngle, endAngle, color);
        }

        @Override public void fillPolygon(double[] xPoints, double[] yPoints, int color) {
            setColor(color);
            Path2D.Double path = new Path2D.Double();
            path.moveTo(px(xPoints[0]), py(yPoints[0]));
            for (int i = 1; i < xPoints.length; i++) {
                path.lineTo(px(xPoints[i]), py(yPoints[i]));
            }
            path.closePath();
            g.fill(path);
        }

        @Override public double getRelativeScale() {
            return 1.0;
        }

        @Override public void doDraw() {}

        @Override public int getWidth() {
            return width;
        }

        @Override public int getHeight() {
            return height;
        }
    }
}
