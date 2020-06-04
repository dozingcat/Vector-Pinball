#version 100
precision mediump float;

uniform vec2 center;
uniform float radius;
uniform vec4 color;
uniform bool filled;
uniform float lineWidth;

void main() {
    // gl_FragCoord is in pixel space, y is up.
    gl_FragColor = color;
    float dist = distance(gl_FragCoord.xy, center);
    if (dist > radius || (!filled && dist < radius - lineWidth)) {
       gl_FragColor.a = 0.0;
    }
}
