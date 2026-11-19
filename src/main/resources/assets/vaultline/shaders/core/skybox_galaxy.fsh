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
    float height = clamp(dir.y, -1.0, 1.0);

    float speed = max(tint.a, 0.0);
    float spin = GameTime * 0.012 * speed;

    // Rotate the direction instead of shifting a longitude: a shifted longitude wraps at the back
    // of the sky and leaves a straight seam there.
    float c = cos(spin);
    float s = sin(spin);
    vec3 spun = vec3(dir.x * c - dir.z * s, dir.y, dir.x * s + dir.z * c);
    float cloud = sky_bands3(spun * 3.0);

    // The galactic band is a slab of noise crossing the sky at an angle, with a dust lane in it.
    float slab = dot(dir, normalize(vec3(0.42, 0.38, -0.82)));
    float band = exp(-pow(slab / 0.24, 2.0)) * (0.35 + 0.65 * cloud);
    float lane = exp(-pow(slab / 0.05, 2.0)) * 0.5;

    vec3 base = mix(vec3(0.01, 0.012, 0.03), vec3(0.03, 0.04, 0.09), smoothstep(-0.3, 0.8, height));
    // The tint colours the galactic haze; the stars stay white so the field still reads as stars.
    vec3 haze = mix(tint.rgb, vec3(0.75, 0.72, 1.0), cloud * 0.45) * band;
    haze *= 1.0 - lane;

    float twinkle = sky_twinkle(dir, GameTime);
    float star = sky_stars(dir, 90.0, 0.972) + sky_stars(dir, 160.0, 0.985) * 0.7;
    star *= twinkle;

    vec3 sky = base + haze + vec3(0.9, 0.93, 1.0) * star;

    fragColor = vec4(sky + sky_dither(gl_FragCoord.xy), 1.0);
}
