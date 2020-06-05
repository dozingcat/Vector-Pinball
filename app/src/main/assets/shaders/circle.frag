#version 100
precision highp float;

uniform vec2 center;
uniform float radiusSquared;
uniform float innerRadiusSquared;
uniform vec4 color;

void main() {
    // gl_FragCoord is in pixel space, y is up.
    gl_FragColor = color;
    vec2 diff = center - gl_FragCoord.xy;
    float distSquared = dot(diff, diff);
    if (distSquared > radiusSquared || distSquared < innerRadiusSquared) {
       gl_FragColor.a = 0.0;
    }
}
