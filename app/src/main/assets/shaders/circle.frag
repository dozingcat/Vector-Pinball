#version 100
precision highp float;

varying vec2 center;
varying float radiusSquared;
varying float innerRadiusSquared;
varying vec4 color;

void main() {
    // gl_FragCoord is in pixel space, y is up.
    gl_FragColor = color;
    vec2 diff = center - gl_FragCoord.xy;
    float distSquared = dot(diff, diff);
    if (distSquared > radiusSquared || distSquared < innerRadiusSquared) {
       gl_FragColor.a = 0.0;
    }
}
