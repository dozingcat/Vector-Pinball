#version 100
precision highp float;

varying vec2 center;
varying float radiusSquared;
varying float innerRadiusSquared;
varying vec4 color;

void main() {
    // gl_FragCoord is in pixel space, y is up.
    gl_FragColor = color;

    // The pixel should be transparent if it's outside of the outer radius, and have full alpha if
    // it's inside the inner radius. If it's between, the alpha should fade to zero.
    vec2 diff = center - gl_FragCoord.xy;
    float distSquared = dot(diff, diff);
    if (distSquared > radiusSquared) {
        gl_FragColor.a = 0.0;
    }
    else if (distSquared > innerRadiusSquared) {
        // Scale alpha from 1 at innerRadiusSquared to 0 at radiusSquared.
        float af = 1.0 - (distSquared - innerRadiusSquared) / (radiusSquared - innerRadiusSquared);
        gl_FragColor.a *= af;
    }

}
