#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

// Vertex stage shared by every hand look. It follows the vanilla entity stage step for step — same
// attributes, same matrix chain, same lightmap and overlay fetches — because the hand layers are
// rebuilt from entity layers and the renderer binds exactly the samplers those layers declare.
// What changes per look is only the fragment stage.

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

out vec4 vertexColor;
out vec4 vertexNormal;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexColor = Color;
    // View space, not model space: the rim is a silhouette test, and the arm pose rotates a
    // model-space normal so it would light arbitrary faces instead of the outline.
    vertexNormal = vec4(mat3(ModelViewMat) * Normal, 0.0);
    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
    overlayColor = texelFetch(Sampler1, UV1, 0);
    texCoord0 = UV0;
}
