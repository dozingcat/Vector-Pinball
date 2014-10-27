package com.dozingcatsoftware.bouncy;

import java.util.List;

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

  public static Point fromList(List<Number> xyList) {
      return fromXY(xyList.get(0).floatValue(), xyList.get(1).floatValue());
  }
}
