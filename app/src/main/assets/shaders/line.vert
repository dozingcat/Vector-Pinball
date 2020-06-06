#version 100
precision highp float;

uniform mat4 uMVPMatrix;
attribute vec4 position;

attribute vec4 inColor;
varying vec4 color;

void main() {
    gl_Position = uMVPMatrix * position;
    color = inColor;
}
