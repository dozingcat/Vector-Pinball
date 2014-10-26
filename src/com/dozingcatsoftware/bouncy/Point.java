package com.dozingcatsoftware.bouncy;

/**
 * An immutable 2D point.
 */
public class Point {
  public final float x, y;

  private Point(float x, float y) {
      this.x = x;
      this.y = y;
  }

  public static Point fromXY(float x, float y) {
      return new Point(x, y);
  }
}
