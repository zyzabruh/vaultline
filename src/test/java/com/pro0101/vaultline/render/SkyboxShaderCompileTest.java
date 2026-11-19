package com.pro0101.vaultline.render;

import com.pro0101.vaultline.modules.render.Skybox;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.client.render.GameRenderer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Compiles and links every skybox stage with a real GLSL compiler, outside the game.
 *
 * <p>The shaders are written for the game's loader, so a compiler cannot read them as they are: the
 * imports have to be expanded first, and the includes carry a {@code #version} line of their own that
 * only the first file may have. {@link #preprocess(String)} does what the loader does — resolve
 * {@code <namespace:path>} to {@code assets/<namespace>/shaders/include/<path>}, recursively, dropping
 * the nested version directives — and then {@code glslang} compiles each stage for OpenGL and links the
 * vertex stage with each fragment stage, which is where varying types are reconciled.
 *
 * <p>This is the check the unit tests could not make: types, swizzles, undeclared functions, version
 * mismatches and identifiers that only exist inside an include are all decided here. The tool lives in
 * {@code .tools/glslang} and is installed by {@code tools/install-glslang.sh}; when it is missing the
 * test is skipped rather than silently passing, and the skip message says how to get it.
 */
class SkyboxShaderCompileTest {
    private static final Pattern IMPORT = Pattern.compile("#moj_import\\s+<([\\w./-]+):([\\w./-]+)>");
    private static final Pattern VERSION = Pattern.compile("(?m)^\\s*#version.*$");

    private static final Path TOOL = Path.of(".tools", "glslang", "glslang.exe");
    private static final int TIMEOUT_SECONDS = 60;

    private static Path work;
    private static ZipFile minecraft;

    /**
     * Creates the scratch directory and opens the Minecraft jar the includes live in.
     *
     */
    @BeforeAll
    static void prepare() throws IOException {
        work = Files.createTempDirectory("vaultline-glsl");
        minecraft = minecraftJar();
    }

    /**
     * @throws Exception when the compiler cannot be run
     */
    @Test
    void everyStageCompiles() throws Exception {
        Path tool = tool();
        assumeTrue(tool != null, "glslang is not installed; run tools/install-glslang.sh to enable this check");

        List<String> failures = new ArrayList<>();

        stageFile(tool, "skybox.vsh", "vert", failures);

        for (Skybox.Mode mode : Skybox.Mode.values()) {
            stageFile(tool, "skybox_" + mode.name().toLowerCase(Locale.ROOT) + ".fsh", "frag", failures);
        }

        assertTrue(failures.isEmpty(), "GLSL compilation failed:\n" + String.join("\n\n", failures));
    }

    /**
     * The fragment stages only take varyings the vertex stage produces, with the same types.
     *
     * <p>glslang's {@code -l} does not reconcile varyings for plain GLSL — it accepts an input that no
     * stage outputs — so the interface is checked here, the way the driver does it at link time: by
     * name, with the type required to match. Without this a renamed or retyped varying would compile
     * stage by stage and only fail inside the game.
     *
     * @throws IOException when a shader cannot be read
     */
    @Test
    void fragmentInputsMatchTheVertexStage() throws IOException {
        Map<String, String> outputs = varyings(read("/assets/vaultline/shaders/core/skybox.vsh"), "out");

        assertTrue(outputs.containsKey("skyDir"), "the vertex stage does not output skyDir");
        assertTrue(outputs.containsKey("tint"), "the vertex stage does not output tint");

        for (Skybox.Mode mode : Skybox.Mode.values()) {
            String name = "skybox_" + mode.name().toLowerCase(Locale.ROOT) + ".fsh";
            Map<String, String> inputs = varyings(read("/assets/vaultline/shaders/core/" + name), "in");

            for (Map.Entry<String, String> input : inputs.entrySet()) {
                String produced = outputs.get(input.getKey());

                assertNotNull(produced, name + " takes " + input.getKey() + ", which the vertex stage never outputs");
                assertEquals(input.getValue(), produced,
                    name + " takes " + input.getKey() + " as " + input.getValue()
                        + " but the vertex stage outputs " + produced);
            }
        }
    }

    /**
     * Collects one stage's interface declarations.
     *
     * @param source shader source
     * @param qualifier {@code in} or {@code out}
     * @return type by variable name
     */
    private static Map<String, String> varyings(String source, String qualifier) {
        Pattern pattern = Pattern.compile(
            "(?m)^\\s*(?:layout\\s*\\([^)]*\\)\\s*)?(?:flat|smooth|noperspective)?\\s*"
                + qualifier + "\\s+(\\w+)\\s+(\\w+)\\s*;"
        );
        Matcher matcher = pattern.matcher(source);
        Map<String, String> declarations = new LinkedHashMap<>();

        while (matcher.find()) declarations.put(matcher.group(2), matcher.group(1));

        return declarations;
    }

    /**
     * Writes one stage out preprocessed and compiles it.
     *
     * @param tool compiler
     * @param file shader file name under the skybox core directory
     * @param stage {@code vert} or {@code frag}
     * @param failures list the failure is added to
     * @return the staged file, or null when it did not compile
     * @throws IOException when the shader cannot be read or written
     * @throws InterruptedException when the compiler is interrupted
     */
    private static Path stageFile(Path tool, String file, String stage, List<String> failures)
        throws IOException, InterruptedException {
        String stagedName = file.substring(0, file.lastIndexOf('.')) + (stage.equals("vert") ? ".vert" : ".frag");
        Path staged = work.resolve(stagedName);

        Files.writeString(staged, preprocess(read("/assets/vaultline/shaders/core/" + file)), StandardCharsets.UTF_8);

        Run run = run(tool, List.of("-S", stage, staged.toString()));

        if (run.failed()) {
            failures.add(file + " (" + stage + "):\n" + run.output());
            return null;
        }

        return staged;
    }

    /**
     * @throws IOException when the process cannot be started
     */
    private static Run run(Path tool, List<String> arguments) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(tool.toString());
        command.addAll(arguments);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        Process process = builder.start();

        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            return new Run(1, "compiler timed out");
        }

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        return new Run(process.exitValue(), output);
    }

    /**
     * Expands the imports of a shader the way the game's loader does.
     *
     * <p>The root file's version directive is kept and every include's is dropped: a hashed include
     * carrying {@code #version} would be a redefinition in the middle of the translation unit.
     *
     * @param source shader source
     * @return the standalone GLSL
     * @throws IOException when an import cannot be resolved
     */
    private static String preprocess(String source) throws IOException {
        return "#version 330\n" + expand(source, 0);
    }

    /**
     * Replaces every import with the include's text, recursively.
     *
     * @throws IOException when an import cannot be read
     */
    private static String expand(String source, int depth) throws IOException {
        assertTrue(depth < 8, "shader imports are nested too deeply");

        StringBuilder out = new StringBuilder();
        Matcher matcher = IMPORT.matcher(source);
        int cursor = 0;

        while (matcher.find()) {
            out.append(VERSION.matcher(source.substring(cursor, matcher.start())).replaceAll(""));
            out.append(expand(readInclude(matcher.group(1), matcher.group(2)), depth + 1));
            cursor = matcher.end();
        }

        out.append(VERSION.matcher(source.substring(cursor)).replaceAll(""));

        return out.toString();
    }

    /**
     * Reads one include, from the Minecraft jar or from the addon's own resources.
     *
     *
     * @throws IOException when the include does not exist
     */
    private static String readInclude(String namespace, String path) throws IOException {
        String resolved = path.contains("/") ? path : "include/" + path;

        if (namespace.equals("minecraft")) {
            ZipEntry entry = minecraft.getEntry("assets/minecraft/shaders/" + resolved);

            assertNotNull(entry, "missing Minecraft include " + resolved);

            try (InputStream stream = minecraft.getInputStream(entry)) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        return read("/assets/" + namespace + "/shaders/" + resolved);
    }

    private static String read(String path) throws IOException {
        try (InputStream stream = SkyboxShaderCompileTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "missing resource " + path);

            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * @return the executable, or null when it is not installed
     */
    private static Path tool() {
        if (Files.isExecutable(TOOL)) return TOOL.toAbsolutePath();

        Path fallback = Path.of("glslangValidator");

        return Files.isExecutable(fallback) ? fallback : null;
    }

    /**
     * Opens the Minecraft jar the game classes come from, so the shader includes can be read from it.
     *
     *
     * @throws IOException when the jar cannot be opened
     */
    private static ZipFile minecraftJar() throws IOException {
        try {
            File jar = new File(GameRenderer.class.getProtectionDomain().getCodeSource().getLocation().toURI());

            assertFalse(jar.isDirectory(), "Minecraft classes are not in a jar, so the includes cannot be read");

            return new ZipFile(jar);
        } catch (URISyntaxException exception) {
            throw new IOException("Minecraft jar location is not a file", exception);
        }
    }

    private record Run(int exit, String output) {
        /**
         * @return true on a non-zero exit or a reported error
         */
        boolean failed() {
            return exit != 0 || output.contains("ERROR:");
        }
    }
}
