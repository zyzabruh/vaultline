#version 330

#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <vaultline:sky.glsl>

in vec3 skyDir;
in vec4 tint;

out vec4 fragColor;

void main() {
    vec3 dir = normalize(skyDir);

    // Horizon at y = 0 and zenith at y = 1; the gradient is not clamped at the horizon, so the
    // sky keeps changing smoothly below it instead of stopping on a straight line.
    float up = dir.y;
    float horizon = smoothstep(0.0, 0.22, up);

    vec3 low = vec3(0.95, 0.42, 0.18);
    vec3 mid = vec3(0.45, 0.14, 0.32);
    vec3 high = vec3(0.05, 0.05, 0.18);

    vec3 sky = mix(mix(low, mid, horizon), high, smoothstep(0.12, 0.85, up));

    // The sun sits on the +X horizon and only its halo is drawn; the vanilla disc is cancelled. The
    // tint is the halo colour, which is what turns the same gradient into a cold dusk or a hot one.
    float sun = pow(max(dot(dir, normalize(vec3(1.0, 0.12, 0.0))), 0.0), 24.0);
    sky += tint.rgb * sun * 1.1;

    // The lower half darkens over a wide band rather than switching at y = 0.
    float down = 1.0 - smoothstep(-0.55, 0.0, up);
    sky = mix(sky, vec3(0.02, 0.02, 0.04), down);

    fragColor = vec4(sky + sky_dither(gl_FragCoord.xy), 1.0);
}
