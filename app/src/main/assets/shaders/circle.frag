#version 100
precision mediump float;

uniform float centerX;
uniform float centerY;
uniform float radiusSquared;
uniform vec4 color;
uniform bool filled;
uniform float lineWidth;

void main() {
    // gl_FragCoord is in pixel space, y is up.
    gl_FragColor = color;
    float diffX = gl_FragCoord.x - centerX;
    float diffY = gl_FragCoord.y - centerY;
    float distSq = diffX * diffX + diffY * diffY;
    if (distSq > radiusSquared) {
       gl_FragColor.a = 0.0;
    }
    // TODO: use lineWidth
    if (!filled && distSq < 0.8 * radiusSquared) {
        gl_FragColor.a = 0.0;
    }
}
