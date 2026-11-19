package com.pro0101.vaultline.render;

import com.pro0101.vaultline.modules.render.Skybox;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sky shaders and the accent math behind them.
 *
 * <p>Two contracts are pinned here. Every mode still resolves to a shader file, and every fragment
 * stage takes the direction the vertex stage computed, uses the tint as its accent instead of
 * filtering the whole image, and never inverts a matrix per pixel — that last one was the hot spot
 * the rewrite removed, and it is exactly the kind of change that would come back silently.
 *
 * <p>The second group checks the numbers the module folds into the accent: night follows the sun,
 * rain dims it, and the accent is clamped so a bright tint cannot push the vertex colour out of range.
 */
class SkyboxTest {
    private static final Pattern IMPORT = Pattern.compile("#moj_import\\s+<vaultline:(\\w+)\\.glsl>");

    /**
     * Every mode has a fragment shader, and every one of them exists as a resource.
     */
    @Test
    void everyModeHasItsShader() throws IOException {
        for (Skybox.Mode mode : Skybox.Mode.values()) {
            String name = "skybox_" + mode.name().toLowerCase(java.util.Locale.ROOT);

            String source = resource("/assets/vaultline/shaders/core/" + name + ".fsh");

            assertFalse(source.isBlank(), name + " is empty");
            assertTrue(source.contains("#version 330"), name + " has no version directive");
            assertTrue(source.contains("void main()"), name + " has no main");
            assertTrue(balanced(source, '{', '}'), name + " has unbalanced braces");
            assertTrue(balanced(source, '(', ')'), name + " has unbalanced parentheses");
            assertFalse(source.contains("in vec2 screen"), name + " still takes the old screen varying");
        }
    }

    /**
     * The direction is computed in the vertex stage and only normalised per fragment.
     */
    @Test
    void directionIsComputedPerVertex() throws IOException {
        String vertex = resource("/assets/vaultline/shaders/core/skybox.vsh");

        assertTrue(vertex.contains("out vec3 skyDir"), "the vertex stage does not pass the direction");
        assertTrue(vertex.contains("out vec4 tint"), "the vertex stage does not pass the tint");
        assertTrue(vertex.contains("sky_view_direction(Position.xy)"), "the direction is not built from the quad");

        for (Skybox.Mode mode : Skybox.Mode.values()) {
            String name = "skybox_" + mode.name().toLowerCase(java.util.Locale.ROOT);
            String fragment = resource("/assets/vaultline/shaders/core/" + name + ".fsh");

            assertTrue(fragment.contains("in vec3 skyDir"), name + " does not take the direction varying");
            assertTrue(fragment.contains("normalize(skyDir)"), name + " does not normalise the direction");
            assertFalse(fragment.contains("sky_view_direction("),
                name + " recomputes the direction per pixel");
            assertFalse(fragment.contains("inverse("),
                name + " inverts a matrix per pixel, which is the cost the rewrite removed");
            assertTrue(fragment.contains("out vec4 fragColor"), name + " has no colour output");
        }
    }

    /**
     * Every stage imports the includes its uniforms come from.
     */
    @Test
    void everyStageImportsWhatItReads() throws IOException {
        for (Skybox.Mode mode : Skybox.Mode.values()) {
            String name = "skybox_" + mode.name().toLowerCase(java.util.Locale.ROOT);
            String fragment = resource("/assets/vaultline/shaders/core/" + name + ".fsh");
            Set<String> imports = new LinkedHashSet<>();
            Matcher matcher = IMPORT.matcher(fragment);

            while (matcher.find()) imports.add(matcher.group(1));

            assertTrue(fragment.contains("#moj_import <minecraft:projection.glsl>"), name + " has no projection");
            assertTrue(fragment.contains("#moj_import <minecraft:dynamictransforms.glsl>"), name + " has no transforms");
            assertTrue(fragment.contains("#moj_import <minecraft:globals.glsl>"), name + " has no globals");
            assertTrue(imports.contains("sky"), name + " does not include the shared sky helpers");
        }
    }

    /**
     * The tint colours the phenomenon instead of filtering the whole sky.
     */
    @Test
    void tintIsTheAccent() throws IOException {
        for (Skybox.Mode mode : Skybox.Mode.values()) {
            String name = "skybox_" + mode.name().toLowerCase(java.util.Locale.ROOT);
            String fragment = resource("/assets/vaultline/shaders/core/" + name + ".fsh");

            assertTrue(fragment.contains("tint.rgb"), name + " ignores the tint entirely");
        }
    }

    /**
     * The shared helpers the modes rely on are where they are expected.
     */
    @Test
    void sharedHelpersExist() throws IOException {
        String include = resource("/assets/vaultline/shaders/include/sky.glsl");

        assertTrue(include.contains("vec3 sky_view_direction(vec2 ndc)"), "no direction helper");
        assertTrue(include.contains("float sky_stars(vec3 dir, float scale, float threshold)"), "no star helper");
        assertTrue(include.contains("float sky_twinkle(vec3 dir, float time)"), "no twinkle helper");
        assertTrue(include.contains("float sky_dither(vec2 position)"), "no dither helper");
    }

    /**
     * Night follows the sun: noon is day, midnight is night, and the edges cross over.
     */
    @Test
    void nightFactorFollowsTheSun() {
        assertEquals(0.0F, SkyboxRenderer.nightFactor(6000), 0.001F, "noon should be full day");
        assertEquals(1.0F, SkyboxRenderer.nightFactor(18000), 0.001F, "midnight should be full night");
        assertEquals(SkyboxRenderer.nightFactor(0), SkyboxRenderer.nightFactor(12000), 0.001F,
            "dawn and dusk should be symmetric around the day");
        assertTrue(SkyboxRenderer.nightFactor(0) > 0.0F && SkyboxRenderer.nightFactor(0) < 1.0F,
            "dawn should be halfway between day and night");

        for (long time = 0; time < 24000; time += 500) {
            float night = SkyboxRenderer.nightFactor(time);

            assertTrue(night >= 0.0F && night <= 1.0F, "night factor out of range at " + time);
        }
    }

    /**
     * Rain dims the accent without ever turning it off.
     */
    @Test
    void rainDimsTheAccent() {
        assertEquals(1.0F, SkyboxRenderer.rainFactor(0.0F), 0.001F);
        assertEquals(0.45F, SkyboxRenderer.rainFactor(1.0F), 0.001F);

        float light = SkyboxRenderer.rainFactor(0.2F);
        float heavy = SkyboxRenderer.rainFactor(0.8F);

        assertTrue(heavy < light, "heavier rain should dim more");
        assertTrue(heavy > 0.0F, "rain should never black out the accent");
    }

    /**
     * The accent is the tint scaled by the fold, clamped to what a vertex colour can carry.
     */
    @Test
    void accentScalesAndClamps() {
        SkyboxRenderer.Accent accent = SkyboxRenderer.accent(0.5F, 0.25F, 1.0F, 1.6F);

        assertEquals(0.8F, accent.red(), 0.001F);
        assertEquals(0.4F, accent.green(), 0.001F);
        assertEquals(1.0F, accent.blue(), 0.001F, "channels must clamp at one");

        SkyboxRenderer.Accent dimmed = SkyboxRenderer.accent(0.5F, 0.5F, 0.5F, 0.0F);

        assertEquals(0.0F, dimmed.red(), 0.001F);
        assertEquals(0.0F, dimmed.green(), 0.001F);
        assertEquals(0.0F, dimmed.blue(), 0.001F);

        SkyboxRenderer.Accent negative = SkyboxRenderer.accent(-1.0F, 0.5F, 0.5F, 1.0F);

        assertEquals(0.0F, negative.red(), 0.001F, "channels must clamp at zero");
    }

    private static boolean balanced(String text, char open, char close) {
        int depth = 0;

        for (char character : text.toCharArray()) {
            if (character == open) depth++;
            if (character == close) depth--;
            if (depth < 0) return false;
        }

        return depth == 0;
    }

    /**
     * Reads a resource from the classpath.
     *
     * @throws IOException when the resource is missing
     */
    private static String resource(String path) throws IOException {
        try (InputStream stream = SkyboxTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "missing resource " + path);

            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
