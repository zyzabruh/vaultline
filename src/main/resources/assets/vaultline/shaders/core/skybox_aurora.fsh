#version 330

#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <vaultline:sky.glsl>

in vec3 skyDir;
in vec4 tint;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(41.7, 289.3))) * 18643.21);
}

float noise(vec2 p) {
    vec2 cell = floor(p);
    vec2 frac = fract(p);
    // "smooth" is a reserved GLSL qualifier and cannot name a variable.
    vec2 ease = frac * frac * (3.0 - 2.0 * frac);

    float a = hash(cell);
    float b = hash(cell + vec2(1.0, 0.0));
    float c = hash(cell + vec2(0.0, 1.0));
    float d = hash(cell + vec2(1.0, 1.0));

    return mix(mix(a, b, ease.x), mix(c, d, ease.x), ease.y);
}

float bands(vec2 p) {
    float total = 0.0;
    float amplitude = 0.5;

    for (int i = 0; i < 4; i++) {
        total += amplitude * noise(p);
        p *= 2.07;
        amplitude *= 0.5;
    }

    return total;
}

void main() {
    vec3 dir = normalize(skyDir);
    float height = clamp(dir.y * 0.5 + 0.5, 0.0, 1.0);

    vec2 uv = vec2(dir.x, dir.z) / max(abs(dir.y) + 0.35, 0.01);
    float drift = GameTime * 0.03 * max(tint.a, 0.0);
    float ribbon = bands(uv * 1.4 + vec2(drift, drift * 0.5));
    ribbon = pow(clamp(ribbon * 1.6 - 0.35, 0.0, 1.0), 1.8);

    vec3 sky = mix(vec3(0.02, 0.03, 0.06), vec3(0.03, 0.09, 0.2), smoothstep(0.35, 1.0, height));

    // The tint is the curtain colour, not a filter over the whole sky: recolouring the module changes
    // the aurora and leaves the night behind it alone.
    vec3 glow = tint.rgb * ribbon;

    // Stars only above the horizon, and dimmed where the curtains are, so they read as depth.
    float stars = sky_stars(dir, 150.0, 0.985) * sky_twinkle(dir, GameTime) * smoothstep(0.05, 0.4, dir.y);
    stars *= 1.0 - clamp(ribbon, 0.0, 1.0) * 0.8;

    fragColor = vec4(sky + glow + vec3(0.85, 0.9, 1.0) * stars * 0.8 + sky_dither(gl_FragCoord.xy), 1.0);
}
