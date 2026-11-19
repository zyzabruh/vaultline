// Helpers shared by the skybox shaders. Textual include: the ProjMat and ModelViewMat uniforms come
// from projection.glsl and dynamictransforms.glsl, which every skybox imports first.

vec3 sky_view_direction(vec2 ndc) {
    mat4 inv = inverse(ProjMat * ModelViewMat);
    vec4 near = inv * vec4(ndc, -1.0, 1.0);
    vec4 far = inv * vec4(ndc, 1.0, 1.0);

    return normalize(far.xyz / far.w - near.xyz / near.w);
}

float sky_hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);

    return fract((p.x + p.y) * p.z);
}

// Value noise sampled in three dimensions. A direction is continuous everywhere on the sphere, so
// unlike a longitude/latitude pair it has no seam at the back and no pinch at the poles.
float sky_noise3(vec3 p) {
    vec3 cell = floor(p);
    vec3 frac = fract(p);
    vec3 ease = frac * frac * (3.0 - 2.0 * frac);

    float n000 = sky_hash13(cell);
    float n100 = sky_hash13(cell + vec3(1.0, 0.0, 0.0));
    float n010 = sky_hash13(cell + vec3(0.0, 1.0, 0.0));
    float n110 = sky_hash13(cell + vec3(1.0, 1.0, 0.0));
    float n001 = sky_hash13(cell + vec3(0.0, 0.0, 1.0));
    float n101 = sky_hash13(cell + vec3(1.0, 0.0, 1.0));
    float n011 = sky_hash13(cell + vec3(0.0, 1.0, 1.0));
    float n111 = sky_hash13(cell + vec3(1.0, 1.0, 1.0));

    float x00 = mix(n000, n100, ease.x);
    float x10 = mix(n010, n110, ease.x);
    float x01 = mix(n001, n101, ease.x);
    float x11 = mix(n011, n111, ease.x);

    return mix(mix(x00, x10, ease.y), mix(x01, x11, ease.y), ease.z);
}

float sky_bands3(vec3 p) {
    float total = 0.0;
    float amplitude = 0.5;

    for (int i = 0; i < 4; i++) {
        total += amplitude * sky_noise3(p);
        p *= 2.07;
        amplitude *= 0.5;
    }

    return total;
}

// Half of one 8-bit colour step, centred on zero. It breaks the straight bands an 8-bit target draws
// through a smooth gradient without reading as grain.
float sky_dither(vec2 position) {
    return (fract(sin(dot(position, vec2(12.9898, 78.233))) * 43758.5453) - 0.5) / 255.0;
}

// A star field on the sphere: one star per cell, placed at a random point inside it, so the field
// has no longitude seam and no rows pinching at the poles. Threshold picks how many cells light up,
// scale how dense the grid is.
float sky_stars(vec3 dir, float scale, float threshold) {
    vec3 point = dir * scale;
    vec3 cell = floor(point);
    vec3 offset = vec3(sky_hash13(cell + 1.7), sky_hash13(cell + 9.3), sky_hash13(cell + 4.1));
    vec3 centre = normalize(cell + offset);
    float distance_to_star = length(dir - centre) * scale;

    return step(threshold, sky_hash13(cell)) * pow(clamp(1.0 - distance_to_star, 0.0, 1.0), 9.0);
}

// Twinkles a star field without a second hash: the noise is sampled at the direction, so each star
// keeps its own phase as the camera turns.
float sky_twinkle(vec3 dir, float time) {
    return 0.75 + 0.25 * sin(time * 2.0 + sky_noise3(dir * 30.0) * 6.2831853);
}
