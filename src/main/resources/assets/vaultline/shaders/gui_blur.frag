#version 330 core

precision lowp float;

// First half of the panel blur: a horizontal gaussian inside the panels only. Pixels outside every
// panel are copied through untouched, so the world around the GUI stays sharp and the pass costs one
// texture read there instead of a whole kernel.
//
// Which pixels belong to a panel comes as a list of rectangles in screen UVs: the copied client drew
// its panels into a mask framebuffer and read that back, but a uniform list needs no extra target
// and no draw call of its own.

in vec2 uv;
out vec4 color;

uniform sampler2D u_Texture;

layout (std140) uniform GuiData {
    vec4 u_Rects[12];
    vec4 u_Accent;
    vec2 u_Screen;
    float u_Radius;
    float u_Bloom;
    float u_Count;
};

float maskAt(vec2 point) {
    float mask = 0.0;

    for (int i = 0; i < 12; i++) {
        if (float(i) >= u_Count) break;

        vec4 rect = u_Rects[i];
        vec2 inside = step(rect.xy, point) * step(point, rect.zw);
        mask = max(mask, inside.x * inside.y);
    }

    return mask;
}

void main() {
    if (maskAt(uv) < 0.5) {
        color = texture(u_Texture, uv);
        return;
    }

    vec2 texel = 1.0 / max(u_Screen, vec2(1.0));
    float radius = clamp(u_Radius, 1.0, 24.0);
    float sigma = 1.0 + radius / 1.5;

    vec4 sum = vec4(0.0);
    float total = 0.0;

    for (int i = -24; i <= 24; i++) {
        float offset = float(i);
        if (abs(offset) > radius) continue;

        float weight = exp(-(offset * offset) / (sigma * sigma));
        sum += texture(u_Texture, uv + vec2(texel.x * offset, 0.0)) * weight;
        total += weight;
    }

    color = sum / max(total, 0.0001);
}
