#version 330 core

precision lowp float;

// Second half of the panel blur, and the bloom in the same pass: the vertical gaussian finishes the
// blur inside the panels, then a falloff from the panel edges adds the halo outside them. One pass
// keeps the effect to two draws, and both write over the frame instead of into a target the GUI then
// has to composite itself.

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

// Distance in pixels from a point to the nearest panel edge, zero inside a panel.
float edgeDistance(vec2 point) {
    float nearest = 1e6;
    vec2 pixels = point * u_Screen;

    for (int i = 0; i < 12; i++) {
        if (float(i) >= u_Count) break;

        vec4 rect = u_Rects[i];
        vec2 low = rect.xy * u_Screen;
        vec2 high = rect.zw * u_Screen;
        vec2 outside = max(max(low - pixels, pixels - high), vec2(0.0));

        nearest = min(nearest, length(outside));
    }

    return nearest;
}

void main() {
    vec2 texel = 1.0 / max(u_Screen, vec2(1.0));
    vec4 base = texture(u_Texture, uv);
    vec3 result = base.rgb;

    if (maskAt(uv) >= 0.5) {
        float radius = clamp(u_Radius, 1.0, 24.0);
        float sigma = 1.0 + radius / 1.5;

        vec4 sum = vec4(0.0);
        float total = 0.0;

        for (int i = -24; i <= 24; i++) {
            float offset = float(i);
            if (abs(offset) > radius) continue;

            float weight = exp(-(offset * offset) / (sigma * sigma));
            sum += texture(u_Texture, uv + vec2(0.0, texel.y * offset)) * weight;
            total += weight;
        }

        result = (sum / max(total, 0.0001)).rgb;
    }

    // The halo sits outside the panels and fades with the distance to the nearest edge.
    if (u_Bloom > 0.0) {
        float distance = edgeDistance(uv);
        float reach = u_Radius * 1.5 + 6.0;
        float glow = exp(-distance / reach) * u_Bloom * u_Accent.a;

        if (distance > 0.0) result += u_Accent.rgb * glow;
    }

    color = vec4(result, 1.0);
}
