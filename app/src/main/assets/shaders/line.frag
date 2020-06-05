#version 100
precision highp float;

uniform vec2 start;
uniform vec2 end;
uniform float lineWidth;
uniform vec4 color;

void main() {
    gl_FragColor = color;
}
