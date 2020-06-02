#version 100
precision mediump float;
void main() {
    gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
    /*
    vec2 fragmentPosition = 2.0*gl_PointCoord - 1.0;
    float distance = length(fragmentPosition);
    // float distanceSqrd = distance * distance;
    float inner = 0.4;
    float outer = 0.6;
    float shadow = 0.2;
    float red = 0.0;
    if (distance > inner && distance < outer) {
        red = 0.7;
    }
    else if (distance < inner && distance > inner - shadow) {
        red = 0.7 * (1.0 - (inner - distance) / shadow);
    }
    else if (distance > outer && distance < outer + shadow) {
        red = 0.7 * (1.0 - (distance - outer) / shadow);
    }
    gl_FragColor = vec4(
    red,
    0.0,
    0.7,
    1.0
    // 0.2/distanceSqrd,
    // 0.1/distanceSqrd,
    // 0.0, 1.0
    );
    */
}
