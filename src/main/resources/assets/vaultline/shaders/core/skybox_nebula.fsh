#version 330

#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <vaultline:sky.glsl>

in vec3 skyDir;
in vec4 tint;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
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

float clouds(vec2 p) {
    float total = 0.0;
    float amplitude = 0.5;

    for (int i = 0; i < 5; i++) {
        total += amplitude * noise(p);
        p *= 2.11;
        amplitude *= 0.5;
    }

    return total;
}

void main() {
    vec3 dir = normalize(skyDir);
    float height = clamp(dir.y * 0.5 + 0.5, 0.0, 1.0);

    vec2 plane = dir.xz / max(abs(dir.y) + 0.25, 0.001);
    float time = GameTime * 0.02 * max(tint.a, 0.0);

    float low = clouds(plane * 1.7 + vec2(time, 0.0));
    float high = clouds(plane * 3.4 - vec2(0.0, time * 0.8));

    vec3 base = mix(vec3(0.03, 0.0, 0.06), vec3(0.05, 0.02, 0.12), height);
    // The tint drives the gas, a second hue is derived from it by pushing it toward a warm highlight,
    // so one colour setting still yields a two-colour nebula instead of a flat wash.
    vec3 violet = tint.rgb * pow(low, 1.7);
    vec3 teal = mix(tint.rgb, vec3(0.65, 0.55, 1.0), 0.45) * pow(high, 2.4);

    // Stars are a sparse threshold on the same hash, so they never move with the clouds.
    float stars = sky_stars(dir, 180.0, 0.985) * sky_twinkle(dir, GameTime) * smoothstep(0.05, 0.4, dir.y);

    fragColor = vec4(base + violet + teal + vec3(0.85, 0.9, 1.0) * stars * 0.7
        + sky_dither(gl_FragCoord.xy), 1.0);
}
