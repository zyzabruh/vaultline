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
    float speed = max(tint.a, 0.0);
    float drift = GameTime * 0.05 * speed;

    // Sampling the swell in three dimensions keeps the pattern attached to the compass without
    // the longitude seam that atan() leaves behind the camera.
    vec3 point = vec3(dir.x, dir.y * 0.45, dir.z) * 3.0;
    float swell = sky_bands3(point + vec3(drift, -drift * 0.3, drift * 0.2));
    float chop = sky_bands3(point * 2.4 - vec3(drift * 1.7, drift * 0.6, 0.0));
    float crest = smoothstep(0.55, 0.85, swell * 0.7 + chop * 0.45);

    vec3 deep = vec3(0.01, 0.05, 0.11);
    vec3 mid = vec3(0.03, 0.22, 0.34);
    vec3 foam = vec3(0.62, 0.9, 0.95);

    // The gradient keeps going below the horizon instead of flattening at y = 0, which is what
    // drew a straight line across the water at eye level.
    vec3 sky = mix(deep, mid, smoothstep(-0.35, 0.75, dir.y));
    float surface = smoothstep(-0.1, 0.5, dir.y);
    // Foam keeps its own brightness; the tint gives it the water's colour, which is what makes the
    // setting read as a different ocean rather than a different exposure.
    sky = mix(sky, mix(tint.rgb, foam, 0.45), crest * (1.0 - surface) * 0.55);

    float sun = pow(max(dot(dir, normalize(vec3(0.3, 0.55, 0.78))), 0.0), 90.0);
    sky += tint.rgb * sun * 1.2;

    fragColor = vec4(sky + sky_dither(gl_FragCoord.xy), 1.0);
}
