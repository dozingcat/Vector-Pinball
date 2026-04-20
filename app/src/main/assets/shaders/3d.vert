uniform mat4 uMVPMatrix;
uniform mat4 uModelMatrix;
attribute vec4 aPosition;
attribute vec3 aNormal;
varying vec3 vNormal;

void main() {
    gl_Position = uMVPMatrix * aPosition;
    vNormal = mat3(uModelMatrix) * aNormal;
}
