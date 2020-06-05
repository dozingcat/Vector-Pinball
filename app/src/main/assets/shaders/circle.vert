#version 100
precision highp float;

uniform mat4 uMVPMatrix;
attribute vec4 position;

attribute vec4 inColor;
varying vec4 color;

attribute float inRadiusSquared;
varying float radiusSquared;

attribute float inInnerRadiusSquared;
varying float innerRadiusSquared;

attribute vec2 inCenter;
varying vec2 center;

void main() {
    gl_Position = uMVPMatrix * position;
    color = inColor;
    radiusSquared = inRadiusSquared;
    innerRadiusSquared = inInnerRadiusSquared;
    center = inCenter;
}
