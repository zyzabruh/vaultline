package com.pro0101.vaultline.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The hand GLSL against the pipeline it is loaded by, so a shader cannot be loaded and inert.
 *
 * <p>Four contracts are checked, each of which used to be invisible until a GPU was in front of it:
 * <ul>
 *   <li>every sampler the GLSL reads is declared by the pipeline with the same name and has a feed,
 *       because a declared-but-unbound sampler reads an empty texture and draws nothing;</li>
 *   <li>every uniform block is imported by the stage that reads it and defined under that exact name
 *       in the Minecraft include it imports, because a renamed block binds nothing;</li>
 *   <li>the vertex inputs are exactly the attribute names of the pipeline's vertex format, with the
 *       right GLSL types, because a mismatched layout feeds zeros or garbage;</li>
 *   <li>blend, depth, cull and the write masks are the state that lets a translucent halo show, and the
 *       fragment stages only discard outside the arm.</li>
 * </ul>
 *
 * <p>On top of the contracts, the sources are linted: a first {@code #version 330}, balanced braces and
 * parentheses, a {@code main}, and no identifier that is neither declared, imported, a keyword nor a
 * GLSL builtin. That last check is what catches a typo in an attribute or sampler name, which is the
 * class of mistake that compiles silently to a no-op. It is not a driver: real compilation is out of
 * reach offline (no {@code glslangValidator} on this machine), and this file says so rather than
 * pretending otherwise.
 */
class HandShaderValidityTest {
    private static final List<HandFx.Look> LOOKS = List.of(HandFx.Look.FILL, HandFx.Look.OUTLINE, HandFx.Look.GLOW);

    private static final Pattern IMPORT = Pattern.compile("#moj_import\\s+<minecraft:(\\w+)\\.glsl>");
    private static final Pattern VERTEX_INPUT = Pattern.compile("(?m)^\\s*in\\s+(\\w+)\\s+(\\w+)\\s*;");
    private static final Pattern TEXTURE_UNIFORM = Pattern.compile("(?m)^\\s*uniform\\s+sampler2D\\s+(\\w+)\\s*;");
    private static final Pattern BLOCK = Pattern.compile("(?m)^\\s*layout\\s*\\(\\s*std140\\s*\\)\\s*uniform\\s+(\\w+)\\s*\\{");

    /** GLSL types each attribute of the entity layout must be declared with. */
    private static final Map<String, String> ATTRIBUTE_TYPES = attributeTypes();

    /**
     * Every sampler read by a hand stage is declared by the pipeline, with the feed that binds it.
     */
    @Test
    void everySamplerReadIsDeclaredAndFed() throws IOException {
        for (HandFx.Look look : LOOKS) {
            HandFx.Spec spec = HandFx.spec(look);
            Set<String> read = new LinkedHashSet<>();

            read.addAll(find(TEXTURE_UNIFORM, source(spec.vertexShader(), "vsh")));
            read.addAll(find(TEXTURE_UNIFORM, source(spec.fragmentShader(), "fsh")));

            assertEquals(new LinkedHashSet<>(spec.samplerNames()), read,
                look + " reads a sampler the pipeline does not declare, or declares one it never reads");

            for (HandFx.Sampler sampler : spec.samplers()) {
                switch (sampler.feed()) {
                    case TEXTURE -> assertEquals("Sampler0", sampler.name(), "the layer texture feeds Sampler0");
                    case OVERLAY -> assertEquals("Sampler1", sampler.name(), "the overlay feeds Sampler1");
                    case LIGHTMAP -> assertEquals("Sampler2", sampler.name(), "the lightmap feeds Sampler2");
                }
            }
        }
    }

    /**
     * Every uniform block is imported by the shader and defined under that name in the Minecraft include.
     */
    @Test
    void everyUniformBlockIsImportedAndDefined() throws IOException {
        try (ZipFile jar = minecraftJar()) {
            for (HandFx.Look look : LOOKS) {
                HandFx.Spec spec = HandFx.spec(look);
                Map<String, String> stageSources = Map.of(
                    "vsh", source(spec.vertexShader(), "vsh"),
                    "fsh", source(spec.fragmentShader(), "fsh")
                );

                for (String uniform : spec.uniforms()) {
                    String include = uniform.toLowerCase(java.util.Locale.ROOT);
                    boolean imported = false;
                    boolean defined = false;

                    for (String stage : stageSources.values()) {
                        if (find(IMPORT, stage).contains(include)) imported = true;
                    }

                    String text = minecraftInclude(jar, include);

                    assertNotNull(text, "Minecraft has no shader include for " + uniform);
                    defined = find(BLOCK, text).contains(uniform);

                    assertTrue(imported, look + " does not import the include that defines " + uniform);
                    assertTrue(defined, "the include " + include + ".glsl does not define a block named " + uniform);
                }
            }
        }
    }

    /**
     * The vertex inputs are exactly the attribute names of the pipeline's format, with matching types.
     */
    @Test
    void vertexInputsMatchTheVertexFormat() throws IOException {
        for (HandFx.Look look : LOOKS) {
            HandFx.Spec spec = HandFx.spec(look);
            Map<String, String> declared = new LinkedHashMap<>();
            Matcher matcher = VERTEX_INPUT.matcher(source(spec.vertexShader(), "vsh"));

            while (matcher.find()) declared.put(matcher.group(2), matcher.group(1));

            assertEquals(new LinkedHashSet<>(spec.vertexFormat().getElementAttributeNames()), declared.keySet(),
                look + " declares vertex inputs the format does not write, or misses one it does");

            declared.forEach((name, type) -> assertEquals(ATTRIBUTE_TYPES.get(name), type,
                name + " is declared as " + type + " but the format writes " + ATTRIBUTE_TYPES.get(name)));

            // The normal is what the rim is computed from: without it the outline has no silhouette.
            assertTrue(declared.containsKey("Normal"), look + " lost the normal attribute");
        }
    }

    /**
     * The built pipeline carries the state that lets a translucent halo show.
     */
    @Test
    void pipelineStateLetsTheHaloDraw() {
        for (HandFx.Look look : LOOKS) {
            HandFx.Spec spec = HandFx.spec(look);
            RenderPipeline pipeline = HandFx.pipeline(look);

            assertNotNull(pipeline, look + " built no pipeline");

            assertSame(spec.vertexFormat(), pipeline.getVertexFormat(), look + " lost the entity vertex layout");
            assertEquals(spec.mode(), pipeline.getVertexFormatMode(),
                look + " lost the draw mode the geometry is written in");
            assertEquals(DepthTestFunction.LEQUAL_DEPTH_TEST, pipeline.getDepthTestFunction(),
                look + " would be rejected by the depth buffer the hands draw against");
            assertTrue(pipeline.getBlendFunction().isPresent(), look + " has no blend, so the halo cannot blend");
            assertFalse(pipeline.isCull(), look + " culls faces, which can remove the arm silhouette");
            assertTrue(pipeline.isWriteColor(), look + " writes no colour");
            assertTrue(pipeline.isWriteAlpha(), look + " writes no alpha");
            assertEquals(spec.depthWrite(), pipeline.isWriteDepth(), look + " depth write differs from its spec");
        }
    }

    /**
     * The stages are structurally valid GLSL, and every identifier is declared or built in.
     */
    @Test
    void everyStageIsStructurallyValid() throws IOException {
        for (HandFx.Look look : LOOKS) {
            HandFx.Spec spec = HandFx.spec(look);

            for (String extension : List.of("vsh", "fsh")) {
                Identifier shader = extension.equals("vsh") ? spec.vertexShader() : spec.fragmentShader();
                String text = source(shader, extension);

                assertTrue(firstLine(text).startsWith("#version 330"),
                    shader + " does not start with #version 330, which the shader loader requires");
                assertTrue(balanced(text, '{', '}'), shader + " has unbalanced braces");
                assertTrue(balanced(text, '(', ')'), shader + " has unbalanced parentheses");
                assertTrue(text.contains("void main()"), shader + " has no main");
                assertEquals(List.of(), unknownIdentifiers(text),
                    shader + " uses identifiers that are neither declared, imported, keywords nor builtins");

                if (extension.equals("fsh")) {
                    assertTrue(text.contains("out vec4 fragColor"), shader + " does not write fragColor");
                }
            }
        }
    }

    /**
     * The fill and glow stages only skip fully transparent texels, so the hand can never vanish.
     */
    @Test
    void fillAndGlowOnlyDiscardTransparentTexels() throws IOException {
        for (HandFx.Look look : List.of(HandFx.Look.FILL, HandFx.Look.GLOW)) {
            String text = source(HandFx.spec(look).fragmentShader(), "fsh");

            assertEquals(1, count(text, "discard"), look + " discards in more than one place");
            assertTrue(text.contains("if (texel.a < 0.05) discard"), look + " discards something other than a clear texel");
        }
    }

    private static Set<String> find(Pattern pattern, String text) {
        Set<String> found = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) found.add(matcher.group(1));

        return found;
    }

    /**
     * Reads a shader stage that ships with the mod.
     *
     * @throws IOException when the resource is missing
     */
    private static String source(Identifier id, String extension) throws IOException {
        String path = "/assets/" + id.getNamespace() + "/shaders/" + id.getPath() + "." + extension;

        try (InputStream stream = HandFx.class.getResourceAsStream(path)) {
            assertNotNull(stream, "missing shader resource " + path);

            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Opens the Minecraft jar the game classes come from, so its shader includes can be read.
     *
     * @throws IOException when the jar cannot be opened
     */
    private static ZipFile minecraftJar() throws IOException {
        try {
            File jar = new File(GameRenderer.class.getProtectionDomain().getCodeSource().getLocation().toURI());

            return new ZipFile(jar);
        } catch (URISyntaxException exception) {
            throw new IOException("Minecraft jar location is not a file", exception);
        }
    }

    private static String minecraftInclude(ZipFile jar, String name) throws IOException {
        ZipEntry entry = jar.getEntry("assets/minecraft/shaders/include/" + name + ".glsl");

        if (entry == null) return null;

        try (InputStream stream = jar.getInputStream(entry)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> attributeTypes() {
        Map<String, String> types = new LinkedHashMap<>();

        types.put("Position", "vec3");
        types.put("Color", "vec4");
        types.put("UV0", "vec2");
        types.put("UV1", "ivec2");
        types.put("UV2", "ivec2");
        types.put("Normal", "vec3");

        return types;
    }

    /**
     * The first line that is neither blank nor a comment.
     *
     * <p>Preprocessor lines are kept on purpose: the version directive is one, and it has to be the
     * first thing the loader sees.
     *
     */
    private static String firstLine(String text) {
        for (String line : text.split("\n")) {
            String trimmed = line.trim();

            if (!trimmed.isEmpty() && !trimmed.startsWith("//")) return trimmed;
        }

        return "";
    }

    /**
     * Whether a pair of delimiters is balanced, ignoring comments.
     *
     */
    private static boolean balanced(String text, char open, char close) {
        String stripped = stripComments(text);
        int depth = 0;

        for (char character : stripped.toCharArray()) {
            if (character == open) depth++;
            if (character == close) depth--;
            if (depth < 0) return false;
        }

        return depth == 0;
    }

    private static int count(String text, String needle) {
        int total = 0;
        int index = text.indexOf(needle);

        while (index >= 0) {
            total++;
            index = text.indexOf(needle, index + needle.length());
        }

        return total;
    }

    /**
     * Finds identifiers that no declaration, import, keyword or builtin accounts for.
     *
     * <p>This is the check that would have caught a renamed attribute or sampler: a typo adds a use of
     * a name nothing declares, and the driver would otherwise answer with zeros or an empty texture.
     *
     */
    private static List<String> unknownIdentifiers(String text) {
        String stripped = stripComments(text).replaceAll("(?m)^\\s*#.*$", "");

        Set<String> declared = new LinkedHashSet<>();
        declared.addAll(find(Pattern.compile("(?m)^\\s*(?:in|out|uniform|const)\\s+(?:\\w+\\s+)*?(\\w+)\\s*(?:;|=|\\[)"), stripped));
        declared.addAll(find(Pattern.compile("(?m)\\b(?:int|float|bool|vec[234]|ivec[234]|mat[234])\\s+(\\w+)\\s*(?:=|;)"), stripped));
        declared.addAll(find(Pattern.compile("(?m)\\b(?:void|int|float|vec[234]|ivec[234]|mat[234])\\s+(\\w+)\\s*\\("), stripped));
        declared.addAll(List.of("ProjMat", "ModelViewMat", "ColorModulator", "ModelOffset", "TextureMat", "main"));

        Set<String> keywords = Set.of(
            "if", "else", "return", "discard", "for", "while", "break", "continue", "in", "out", "uniform",
            "layout", "std140", "const", "void", "float", "int", "bool", "vec2", "vec3", "vec4", "ivec2",
            "ivec3", "ivec4", "mat2", "mat3", "mat4", "sampler2D", "true", "false", "struct"
        );

        Set<String> builtins = Set.of(
            "texture", "texelFetch", "normalize", "length", "smoothstep", "mix", "max", "min", "abs", "dot",
            "clamp", "pow", "sqrt", "sin", "cos", "floor", "fract", "step", "mod", "cross", "exp", "log"
        );

        List<String> unknown = new ArrayList<>();
        Matcher matcher = Pattern.compile("(?<![.\\w])([A-Za-z_]\\w*)(\\s*\\()?").matcher(stripped);

        while (matcher.find()) {
            // A name followed by a parenthesis is a call, not a value read.
            if (matcher.group(2) != null) continue;

            String name = matcher.group(1);

            if (name.startsWith("gl_")) continue;
            if (keywords.contains(name)) continue;
            if (builtins.contains(name)) continue;
            if (declared.contains(name)) continue;

            unknown.add(name);
        }

        return unknown;
    }

    /**
     * Removes line and block comments, so neither braces nor identifiers inside them are counted.
     *
     */
    private static String stripComments(String text) {
        return text.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("(?m)//.*$", " ");
    }
}
