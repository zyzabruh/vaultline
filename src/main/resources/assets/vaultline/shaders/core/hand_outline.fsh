#version 330

// Hand outline: only the rim survives. The rim is the part of the arm whose normal is perpendicular
// to the view direction, so a fresnel term on the view-space vertex normal traces the silhouette
// without a second, scaled draw. When a layer arrives without normals — some item and map layers do —
// a flat glow is drawn instead of an empty screen.

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
    float glow = normalLength < 0.01 ? 0.85 : smoothstep(0.55, 1.0, 1.0 - abs(normal.z / normalLength));

    if (glow < 0.08) discard;

    vec3 rim = TINT * glow * lightMapColor.rgb;
    rim = mix(overlayColor.rgb * glow, rim, overlayColor.a);

    fragColor = vec4(rim, glow * texel.a * vertexColor.a);
}
