#version 330

// Hand glow: the fill and the rim in one draw, which is what both toggles on means. The rim is added
// on top of the fill instead of replacing it, so the arm reads as a lit shape inside a halo. A layer
// that arrives without normals still gets the fill, plus a flat halo.

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec4 vertexNormal;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;

out vec4 fragColor;

const vec3 TINT = vec3(0.62, 0.86, 1.0);

void main() {
    vec4 texel = texture(Sampler0, texCoord0);

    if (texel.a < 0.05) discard;

    vec3 normal = vertexNormal.xyz;
    float normalLength = length(normal);
    float rim = normalLength < 0.01 ? 0.35 : smoothstep(0.45, 1.0, 1.0 - abs(normal.z / normalLength));

    vec3 fill = mix(texel.rgb, TINT, 0.7) * vertexColor.rgb;
    fill = mix(overlayColor.rgb, fill, overlayColor.a);
    fill *= lightMapColor.rgb;

    fragColor = vec4(fill + TINT * rim * 0.9, max(texel.a, rim) * vertexColor.a);
}
