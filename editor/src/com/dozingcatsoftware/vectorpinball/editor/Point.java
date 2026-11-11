package com.dozingcatsoftware.vectorpinball.editor;

import java.util.List;

/**
 * An immutable 2D point. Includes static methods for computing distances between points and lines.
 */
public class Point {
  public final double x, y;

  private Point(double x, double y) {
      this.x = x;
      this.y = y;
  }

  public static Point fromXY(double x, double y) {
      return new Point(x, y);
  }

  public static Point fromList(List<Number> xyList) {
      return fromXY(xyList.get(0).doubleValue(), xyList.get(1).doubleValue());
  }

  public Point add(Point other) {
      return fromXY(x+other.x, y+other.y);
  }

  public Point subtract(Point other) {
      return fromXY(x-other.x, y-other.y);
  }

  public static double squaredDistanceBetween(double x1, double y1, double x2, double y2) {
      double xdiff = x1 - x2;
      double ydiff = y1 - y2;
      return xdiff*xdiff + ydiff*ydiff;
  }

  public static double distanceBetween(double x1, double y1, double x2, double y2) {
      return Math.sqrt(squaredDistanceBetween(x1, y1, x2, y2));
  }

  public static double distanceFromPointToLine(
          double x0, double y0, double x1, double y1, double x2, double y2) {
      // http://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line
      double segmentLength = distanceBetween(x1, y1, x2, y2);
      if (segmentLength==0) return distanceBetween(x0, y0, x1, y1);
      return Math.abs((y2-y1)*x0 - (x2-x1)*y0 + x2*y1 - y2*x1) / segmentLength;
  }

  public static double distanceFromPointToLineSegment(
          double x0, double y0, double x1, double y1, double x2, double y2) {
      // If dot product of (p2-p1) and (p0-p1) is negative, we extend past p1.
      if ((x2-x1) * (x0-x1) + (y2-y1)*(y0-y1) < 0) {
          return distanceBetween(x0, y0, x1, y1);
      }
      // If dot product of (p1-p2) and (this-p2) is negative, we extend past p2.
      if ((x1-x2) * (x0-x2) + (y1-y2)*(y0-y2) < 0) {
          return distanceBetween(x0, y0, x2, y2);
      }
      return distanceFromPointToLine(x0, y0, x1, y1, x2, y2);
  }

  public double distanceTo(double x, double y) {
      return distanceBetween(this.x, this.y, x, y);
  }

  public double distanceTo(Point other) {
      return distanceBetween(this.x, this.y, other.x, other.y);
  }

  public double squaredDistanceTo(double x, double y) {
      return squaredDistanceBetween(this.x, this.y, x, y);
  }

  public double squaredDistanceTo(Point other) {
      return squaredDistanceBetween(this.x, this.y, other.x, other.y);
  }

  public double distanceToLine(double x1, double y1, double x2, double y2) {
      return distanceFromPointToLine(this.x, this.y, x1, y1, x2, y2);
  }

  public double distanceToLine(Point p1, Point p2) {
      return distanceFromPointToLine(this.x, this.y, p1.x, p1.y, p2.x, p2.y);
  }

  public double distanceToLineSegment(double x1, double y1, double x2, double y2) {
      return distanceFromPointToLineSegment(this.x, this.y, x1, y1, x2, y2);
  }

  public double distanceToLineSegment(Point p1, Point p2) {
      return distanceFromPointToLineSegment(this.x, this.y, p1.x, p1.y, p2.x, p2.y);
  }

  @Override public String toString() {
      return "Point(" + x + ", " + y + ")";
  }
}
