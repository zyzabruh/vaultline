#version 330

#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <vaultline:sky.glsl>

in vec3 skyDir;
in vec4 tint;

out vec4 fragColor;

float sparks3(vec3 dir, float scale, float rise, float threshold) {
    vec3 p = dir * scale;
    // Scrolling the field through time lifts the embers into the sky.
    p.y += GameTime * rise;

    vec3 cell = floor(p);
    vec3 offset = vec3(sky_hash13(cell + 3.3), sky_hash13(cell + 11.9), sky_hash13(cell + 27.0));
    float seed = sky_hash13(cell + 27.0);
    float distance_to_spark = length(fract(p) - offset);

    return step(threshold, seed) * pow(clamp(1.0 - distance_to_spark * 2.2, 0.0, 1.0), 5.0);
}

void main() {
    vec3 dir = normalize(skyDir);
    float speed = max(tint.a, 0.0);

    // Heat pools at the horizon and the sky above it turns to cooled basalt. The gradient keeps
    // running below the horizon so the two halves meet without a line.
    vec3 base = mix(vec3(0.16, 0.03, 0.01), vec3(0.03, 0.01, 0.02), smoothstep(-0.4, 0.55, dir.y));
    // smoothstep instead of max() keeps the glow's slope continuous at the horizon, so the heat
    // does not leave a faint band where the clamp used to kink.
    float glow = exp(-smoothstep(-0.05, 1.0, dir.y) * 5.0) * 0.6;
    vec3 sky = base + tint.rgb * glow;

    float ember = sparks3(dir, 26.0, 0.35 * speed, 0.955);
    ember += sparks3(dir, 55.0, 0.15 * speed, 0.93) * 0.6;
    // Sparks stay hotter than the sky they rise from: the tint sets their base, white-hot is added.
    sky += mix(tint.rgb, vec3(1.0, 0.85, 0.4), 0.5) * ember;

    sky += tint.rgb * sky_bands3(dir * 3.0 + vec3(0.0, GameTime * 0.02 * speed, 0.0)) * 0.25;

    fragColor = vec4(sky + sky_dither(gl_FragCoord.xy), 1.0);
}
