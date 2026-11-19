#version 330

#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <vaultline:sky.glsl>

// The direction is built once per vertex instead of once per pixel. The clip-space corners are the
// quad's own position, so the inverse of the view-projection turns them into a far-plane ray that the
// rasteriser interpolates correctly; the fragment stage only normalises it. Doing the inverse in the
// fragment stage, as this used to, ran a mat4 inversion for every pixel of the sky.

in vec3 Position;
in vec4 Color;

out vec3 skyDir;
out vec4 tint;

void main() {
    skyDir = sky_view_direction(Position.xy);
    tint = Color;

    gl_Position = vec4(Position.xy, 1.0, 1.0);
}
