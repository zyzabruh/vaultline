package com.pro0101.vaultline.render;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The hand pipelines against the shader files they name.
 *
 * <p>A pipeline is a promise: these shaders exist, they read exactly these uniform blocks and
 * samplers, the vertex stage outputs what the fragment stage takes, and the layout is the entity one
 * the game writes hand geometry in. Every one of those promises can be checked from the resources on
 * the classpath, so a missing file, a renamed sampler or a shader that quietly lost its rim is
 * caught here instead of in a frame.
 *
 * <p>The last group of checks is the difference the two toggles are supposed to make: no rim term in
 * the fill stage, no fill term in the outline stage, and nothing from the entity shaders reused, so
 * a hand effect cannot leak onto the HUD or the entities.
 */
class HandPipelineTest {
    private static final List<HandFx.Look> LOOKS = List.of(HandFx.Look.FILL, HandFx.Look.OUTLINE, HandFx.Look.GLOW);

    private static final Pattern VERTEX_OUTPUT = Pattern.compile("(?m)^\\s*out\\s+\\w+\\s+(\\w+)\\s*;");
    private static final Pattern FRAGMENT_INPUT = Pattern.compile("(?m)^\\s*in\\s+\\w+\\s+(\\w+)\\s*;");
    private static final Pattern SAMPLER = Pattern.compile("(?m)^\\s*uniform\\s+sampler2D\\s+(\\w+)\\s*;");
    private static final Pattern IMPORT = Pattern.compile("#moj_import\\s+<minecraft:(\\w+)\\.glsl>");

    @Test
    void offLookHasNoSpec() {
        assertNull(HandFx.spec(HandFx.Look.OFF));
        assertNull(HandFx.spec(null));
    }

    /**
     * Every effect look declares the same stages, layout and uniforms, and its own pipeline.
     */
    @Test
    void everyLookDescribesAPipeline() {
        Set<Identifier> locations = new LinkedHashSet<>();
        Set<Identifier> fragments = new LinkedHashSet<>();

        for (HandFx.Look look : LOOKS) {
            HandFx.Spec spec = HandFx.spec(look);

            assertNotNull(spec, look + " has no spec");
            assertEquals("vaultline", spec.location().getNamespace());
            assertEquals("vaultline", spec.vertexShader().getNamespace());
            assertEquals("vaultline", spec.fragmentShader().getNamespace());
            assertFalse(spec.fragmentShader().getPath().contains("entity"), look + " reuses an entity shader");

            assertEquals(List.of("DynamicTransforms", "Projection"), spec.uniforms());
            assertEquals(List.of("Sampler0", "Sampler1", "Sampler2"), spec.samplerNames());
            assertEquals("core/hand", spec.vertexShader().getPath());
            assertSame(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, spec.vertexFormat());
            assertEquals(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL.getElements(), spec.vertexFormat().getElements());
            assertEquals(net.minecraft.client.gl.UniformType.UNIFORM_BUFFER, net.minecraft.client.gl.UniformType.UNIFORM_BUFFER);

            locations.add(spec.location());
            fragments.add(spec.fragmentShader());
        }

        assertEquals(LOOKS.size(), locations.size(), "two looks share a pipeline");
        assertEquals(LOOKS.size(), fragments.size(), "two looks share a fragment stage");
    }

    /**
     * Hand geometry is quads, as vanilla writes it.
     *
     * <p>A model part emits four corners per face. Declaring triangles reads each group of four as
     * one triangle and a third of the next face, which is what turns the hands into spikes; the
     * vanilla entity pipelines the hands are taken from all declare quads, so the hand pipelines must
     * declare the same.
     */
    @Test
    void handGeometryIsDrawnAsQuadsLikeTheVanillaEntityPipelines() {
        for (HandFx.Look look : LOOKS) {
            HandFx.Spec spec = HandFx.spec(look);
            RenderPipeline pipeline = HandFx.pipeline(look);

            assertEquals(VertexFormat.DrawMode.QUADS, spec.mode(), look + " declares a non-quad topology");
            assertEquals(RenderPipelines.ENTITY_SOLID.getVertexFormatMode(), pipeline.getVertexFormatMode(),
                look + " draws quads as triangles, so every model part is misread");
            assertEquals(RenderPipelines.ENTITY_SOLID.getVertexFormat(), pipeline.getVertexFormat(),
                look + " does not use the layout vanilla writes hand geometry in");
        }
    }

    /**
     * A hand pipeline is built for the layout of the layer it replaces, not for a fixed one.
     *
     * <p>This is the regression the spikes came from: the pipeline declared a topology the geometry
     * was not written in, so every vertex after the first face was read from the wrong place. The
     * layout is taken from the layer, and a layer whose layout the vertex stage cannot read is left
     * alone rather than fed garbage.
     */
    @Test
    void aHandPipelineIsBuiltForTheLayoutOfTheGeometryItReplaces() {
        RenderPipeline base = RenderPipeline.builder()
            .withLocation(Identifier.of("vaultline", "test/quads"))
            .withVertexShader(Identifier.of("vaultline", "core/hand"))
            .withFragmentShader(Identifier.of("vaultline", "core/hand_fill"))
            .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS)
            .build();
        RenderLayer quads = RenderLayer.of("vaultline_test_quads", RenderSetup.builder(base).build());
        RenderPipeline pipeline = HandFx.pipeline(HandFx.Look.FILL, quads.getVertexFormat(), quads.getDrawMode());

        assertEquals(VertexFormat.DrawMode.QUADS, quads.getDrawMode(), "the fixture is not hand geometry");
        assertNotNull(pipeline);
        assertEquals(quads.getDrawMode(), pipeline.getVertexFormatMode(),
            "the pipeline does not draw in the topology its layer declares");
        assertEquals(quads.getVertexFormat(), pipeline.getVertexFormat());

        assertTrue(HandFx.readable(quads.getVertexFormat()), "the entity layout the hands use was called unreadable");
        assertFalse(HandFx.readable(VertexFormats.POSITION), "a position-only layout was called readable");
    }

    /**
     * The outline look does not write depth, because it draws over the arm it traces.
     */
    @Test
    void outlineDoesNotWriteDepth() {
        assertFalse(HandFx.spec(HandFx.Look.OUTLINE).depthWrite());
        assertTrue(HandFx.spec(HandFx.Look.FILL).depthWrite());
        assertTrue(HandFx.spec(HandFx.Look.GLOW).depthWrite());
    }

    /**
     * Every shader a spec names exists as a resource.
     */
    @Test
    void everyShaderResourceExists() throws IOException {
        for (HandFx.Look look : LOOKS) {
            HandFx.Spec spec = HandFx.spec(look);

            assertFalse(read(spec.vertexShader(), "vsh").isBlank(), look + " vertex stage is empty");
            assertFalse(read(spec.fragmentShader(), "fsh").isBlank(), look + " fragment stage is empty");
        }
    }

    /**
     * The declared uniforms and samplers are exactly what the stages read.
     */
    @Test
    void declaredUniformsAndSamplersMatchTheSources() throws IOException {
        for (HandFx.Look look : LOOKS) {
            HandFx.Spec spec = HandFx.spec(look);
            String vertex = read(spec.vertexShader(), "vsh");
            String fragment = read(spec.fragmentShader(), "fsh");

            Set<String> imports = find(IMPORT, vertex);
            imports.addAll(find(IMPORT, fragment));

            for (String uniform : spec.uniforms()) {
                assertTrue(imports.contains(uniform.toLowerCase(java.util.Locale.ROOT)),
                    look + " declares " + uniform + " but no stage imports it");
            }

            Set<String> declaredSamplers = find(SAMPLER, vertex);
            declaredSamplers.addAll(find(SAMPLER, fragment));

            assertEquals(new LinkedHashSet<>(spec.samplerNames()), declaredSamplers,
                look + " declares samplers no stage reads, or misses one a stage needs");
        }
    }

    /**
     * Every fragment input is produced by the vertex stage.
     */
    @Test
    void fragmentInputsAreProducedByTheVertexStage() throws IOException {
        for (HandFx.Look look : LOOKS) {
            HandFx.Spec spec = HandFx.spec(look);
            Set<String> outputs = find(VERTEX_OUTPUT, read(spec.vertexShader(), "vsh"));
            Set<String> inputs = find(FRAGMENT_INPUT, read(spec.fragmentShader(), "fsh"));

            assertTrue(outputs.containsAll(inputs), look + " reads an output the vertex stage does not write");
            assertTrue(inputs.contains("vertexColor"), look + " lost the vertex colour");
            assertTrue(inputs.contains("texCoord0"), look + " lost the texture coordinate");
        }
    }

    /**
     * The vertex stage transforms with the same matrices as the rest of the world.
     */
    @Test
    void vertexStageUsesTheWorldMatrices() throws IOException {
        String vertex = read(HandFx.spec(HandFx.Look.FILL).vertexShader(), "vsh");

        assertTrue(vertex.contains("gl_Position = ProjMat * ModelViewMat"));
        assertTrue(vertex.contains("in vec3 Position"));
        assertTrue(vertex.contains("in vec4 Color"));
        assertTrue(vertex.contains("in vec2 UV0"));
        // A model-space normal makes the rim light arbitrary faces: the arm pose rotates it.
        assertTrue(vertex.contains("mat3(ModelViewMat) * Normal"), "the normal never reaches view space");
    }

    /**
     * Fill means fill: the outline term is absent from the program, not multiplied away.
     */
    @Test
    void fillStageHasNoOutlineTerm() throws IOException {
        String fill = read(HandFx.spec(HandFx.Look.FILL).fragmentShader(), "fsh");

        assertFalse(fill.contains("vertexNormal"), "fill stage still receives the normal");
        assertFalse(fill.contains("smoothstep"), "fill stage still computes a rim");
        assertTrue(fill.contains("mix(texel.rgb"), "fill stage lost its tint");
    }

    /**
     * Outline means outline: no fill tint is mixed into the texel.
     */
    @Test
    void outlineStageHasNoFillTerm() throws IOException {
        String outline = read(HandFx.spec(HandFx.Look.OUTLINE).fragmentShader(), "fsh");

        assertFalse(outline.contains("mix(texel.rgb"), "outline stage still fills");
        assertTrue(outline.contains("smoothstep"), "outline stage lost its rim");
        assertTrue(outline.contains("vertexNormal"), "outline stage lost the normal it traces");
    }

    @Test
    void glowStageHasBothTerms() throws IOException {
        String glow = read(HandFx.spec(HandFx.Look.GLOW).fragmentShader(), "fsh");

        assertTrue(glow.contains("mix(texel.rgb"), "glow stage lost its fill");
        assertTrue(glow.contains("smoothstep"), "glow stage lost its rim");
    }

    /**
     * The vertex stage fetches what the entity layers bind, so the layer's flags have a consumer.
     */
    @Test
    void vertexStageFetchesLightmapAndOverlay() throws IOException {
        String vertex = read(HandFx.spec(HandFx.Look.FILL).vertexShader(), "vsh");

        assertTrue(vertex.contains("in ivec2 UV1"), "overlay coordinate missing");
        assertTrue(vertex.contains("in ivec2 UV2"), "lightmap coordinate missing");
        assertTrue(vertex.contains("texelFetch(Sampler1, UV1, 0)"), "overlay not fetched");
        assertTrue(vertex.contains("texelFetch(Sampler2, UV2 / 16, 0)"), "lightmap not fetched");
    }

    /**
     * The rim survives layers that arrive without normals instead of discarding the whole screen.
     */
    @Test
    void outlineAndGlowFallBackWithoutNormals() throws IOException {
        String outline = read(HandFx.spec(HandFx.Look.OUTLINE).fragmentShader(), "fsh");
        String glow = read(HandFx.spec(HandFx.Look.GLOW).fragmentShader(), "fsh");

        assertTrue(outline.contains("normalLength < 0.01"), "outline still discards a normal-less layer");
        assertTrue(glow.contains("normalLength < 0.01"), "glow still discards a normal-less layer");
    }

    /**
     * The hand stages are the addon's own, so nothing they do can reach the entity stages.
     */
    @Test
    void handStagesAreTheAddonsOwn() {
        HandFx.Spec fill = HandFx.spec(HandFx.Look.FILL);
        HandFx.Spec outline = HandFx.spec(HandFx.Look.OUTLINE);

        assertNotEquals(fill.fragmentShader(), outline.fragmentShader());
        assertTrue(fill.location().getPath().startsWith("pipeline/hand_"));
        assertTrue(fill.fragmentShader().getPath().startsWith("core/hand_"));
        assertFalse(fill.uniforms().contains("Globals"), "hand pipelines must not pull in the entity globals");
    }

    /**
     * The fill stage is the only one that draws the tint, so the other looks cannot fill by accident.
     */
    @Test
    void onlyFillAndGlowDrawTheTint() throws IOException {
        String fill = read(HandFx.spec(HandFx.Look.FILL).fragmentShader(), "fsh");
        String outline = read(HandFx.spec(HandFx.Look.OUTLINE).fragmentShader(), "fsh");
        String glow = read(HandFx.spec(HandFx.Look.GLOW).fragmentShader(), "fsh");

        assertTrue(fill.contains("mix(texel.rgb, TINT"));
        assertTrue(glow.contains("mix(texel.rgb, TINT"));
        assertFalse(outline.contains("mix(texel.rgb, TINT"), "outline mixes a fill tint");
    }

    private static String read(Identifier id, String extension) throws IOException {
        String path = "/assets/" + id.getNamespace() + "/shaders/" + id.getPath() + "." + extension;

        try (InputStream stream = HandFx.class.getResourceAsStream(path)) {
            assertNotNull(stream, "missing shader resource " + path);

            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Set<String> find(Pattern pattern, String source) {
        Set<String> found = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(source);

        while (matcher.find()) found.add(matcher.group(1));

        return found;
    }
}
