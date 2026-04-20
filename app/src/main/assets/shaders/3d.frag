precision mediump float;
uniform vec4 uColor;
uniform vec3 uLightDir;
varying vec3 vNormal;

void main() {
    vec3 norm = normalize(vNormal);
    float ambient = 0.35;
    float diffuse = max(dot(norm, -uLightDir), 0.0) * 0.65;
    float light = ambient + diffuse;
    gl_FragColor = vec4(uColor.rgb * light, uColor.a);
}
