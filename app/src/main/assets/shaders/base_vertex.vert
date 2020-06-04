#version 100
precision highp float;

uniform mat4 uMVPMatrix;
attribute vec4 position;

void main() {
    gl_Position = uMVPMatrix * position;
}
