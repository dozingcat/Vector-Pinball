package com.dozingcatsoftware.vectorpinball.model;

/**
 * Interface for rendering field elements in 3D. The coordinate system matches the 2D field
 * (X and Y are the table plane), with Z representing height above the table surface.
 * Colors use the same ARGB int format as the 2D renderer.
 */
public interface IField3DRenderer {

    /** Draws a sphere centered at (cx, cy, cz) with the given radius. */
    void drawSphere(float cx, float cy, float cz, float radius, int color);

    /** Draws an axis-aligned box from (x1, y1, z1) to (x2, y2, z2). */
    void drawBox(float x1, float y1, float z1, float x2, float y2, float z2, int color);

    /**
     * Draws a cylinder centered at (cx, cy) on the table plane, extending from zBottom to zTop.
     */
    void drawCylinder(float cx, float cy, float zBottom, float zTop, float radius, int color);

    /**
     * Draws an oriented wall from (x1, y1) to (x2, y2), extruded from zBottom to zTop
     * with the given thickness (half-width perpendicular to the wall direction).
     */
    void drawWallBox(float x1, float y1, float x2, float y2,
                     float zBottom, float zTop, float thickness, int color);

    /**
     * Draws a flat quad at the given Z height. Vertices are specified in order.
     */
    void drawQuad(float x1, float y1, float x2, float y2,
                  float x3, float y3, float x4, float y4, float z, int color);

    /** Called before and after each frame's draw pass. */
    void begin3DFrame();
    void end3DFrame();

    /**
     * Begins an overlay pass: subsequent draw calls are rendered above all field elements
     * with depth testing disabled. Used for score animations and other UI that should always
     * be visible.
     */
    void beginOverlay();
    void endOverlay();
}
