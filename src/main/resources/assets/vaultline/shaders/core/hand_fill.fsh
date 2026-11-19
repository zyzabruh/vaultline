#version 330

// Hand fill: the arm keeps the shape and the lighting the game drew and takes the accent tint as its
// colour. The normal is not even declared here, so turning the outline off removes the rim from the
// program itself rather than multiplying it by zero.

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;

out vec4 fragColor;

const vec3 TINT = vec3(0.62, 0.86, 1.0);

void main() {
    vec4 texel = texture(Sampler0, texCoord0);

    if (texel.a < 0.05) discard;

    vec3 tinted = mix(texel.rgb, TINT, 0.85) * vertexColor.rgb;
    tinted = mix(overlayColor.rgb, tinted, overlayColor.a);
    tinted *= lightMapColor.rgb;

    fragColor = vec4(tinted, texel.a * vertexColor.a);
}
