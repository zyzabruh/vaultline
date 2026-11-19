package com.pro0101.vaultline.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import meteordevelopment.meteorclient.renderer.FixedUniformStorage;
import meteordevelopment.meteorclient.renderer.FullScreenRenderer;
import meteordevelopment.meteorclient.renderer.MeteorVertexFormats;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.OptionalInt;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * The copied client's GUI effects: the blur behind every panel and the bloom around them.
 *
 * <p>Both run as two fullscreen passes over the frame, after the world is drawn and before the GUI is
 * layered on top, which is the only moment the frame is both complete and still writable:
 * <ol>
 *   <li>a horizontal gaussian inside the panels, written to a scratch target — outside every panel
 *       the pixel is copied through, so the world around the GUI stays sharp;</li>
 *   <li>the vertical gaussian inside the panels, plus the bloom falloff around their edges, written
 *       straight back over the frame.</li>
 * </ol>
 *
 * <p>Which pixels belong to a panel is a list of screen rectangles handed to the shaders as uniforms.
 * That replaces the mask framebuffer the other client captures, and saves both the extra target and
 * the draw calls that would fill it. Eight panels are covered, which is more categories than the
 * screen shows at once.
 *
 * <p>Every failure — a shader that will not compile, a driver that rejects the pipeline — disables the
 * effect for the session and leaves the GUI plain rather than breaking the frame.
 */
public final class GuiFx {
    private static final Logger LOG = LoggerFactory.getLogger("Vaultline GUI effects");

    private static final Identifier VERTEX_SHADER = Identifier.of("vaultline", "shaders/gui_blur.vert");
    private static final Identifier BLUR_SHADER = Identifier.of("vaultline", "shaders/gui_blur.frag");
    private static final Identifier COMPOSITE_SHADER = Identifier.of("vaultline", "shaders/gui_composite.frag");

    /** Number of panel rectangles the shaders accept: the categories, the game and the presets. */
    private static final int MAX_RECTS = 12;

    private static final int UNIFORM_SIZE = new Std140SizeCalculator()
        .putVec4().putVec4().putVec4().putVec4()
        .putVec4().putVec4().putVec4().putVec4()
        .putVec4()
        .putVec2()
        .putFloat()
        .putFloat()
        .putFloat()
        .get();

    private static final FixedUniformStorage<Data> STORAGE =
        new FixedUniformStorage<>("Vaultline - Gui Effects", UNIFORM_SIZE, 1);

    private static RenderPipeline blurPipeline;
    private static RenderPipeline compositePipeline;
    private static GpuTexture scratch;
    private static GpuTextureView scratchView;
    private static int scratchWidth;
    private static int scratchHeight;
    private static boolean compiled;
    private static boolean failed;
    private static boolean loggedFailure;
    /** Set once the first frame has been drawn with the effects, so the log says whether they work. */
    private static boolean reported;

    private GuiFx() {
    }

    /**
     * Runs the two passes for one frame.
     *
     * @param rects panel rectangles in screen UVs, four floats each ({@code x0, y0, x1, y1}), reused
     *     between frames so the pass never allocates
     * @param count how many rectangles of {@code rects} are in use
     * @param accent bloom colour, packed as an ARGB integer
     * @param radius blur radius in pixels
     * @param bloom bloom strength, zero to skip the halo
     */
    public static void render(float[] rects, int count, int accent, float radius, float bloom) {
        if (failed || count <= 0) return;

        try {
            Framebuffer main = mc.getFramebuffer();
            if (main == null) return;

            ensurePipelines();
            if (failed || blurPipeline == null || compositePipeline == null) return;

            if (!ensureScratch(main.textureWidth, main.textureHeight)) return;

            int written = Math.min(count, MAX_RECTS);

            STORAGE.clear();
            GpuBufferSlice data = STORAGE.write(new Data(rects, written, main.textureWidth, main.textureHeight, radius,
                accent, bloom));

            GpuTextureView mainView = main.getColorAttachmentView();
            if (mainView == null) return;

            var sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

            // Pass one: the frame into the scratch target, blurred along x inside the panels.
            draw(scratchView, blurPipeline, data, mainView, sampler);

            // Pass two: the blurred frame back over the screen, blurred along y and bloomed.
            draw(mainView, compositePipeline, data, scratchView, sampler);

            if (!reported) {
                reported = true;
                LOG.info("GUI effects are running: {} panels blurred, radius {}, bloom {}", written, radius, bloom);
            }
        } catch (RuntimeException e) {
            failed = true;

            if (!loggedFailure) {
                LOG.warn("Could not run the GUI effects, leaving the screen without them", e);
                loggedFailure = true;
            }

            release();
        }
    }

    /**
     * Draws one fullscreen pass over {@code target}, reading {@code source}.
     *
     * <p>Meteor's {@code MeshRenderer} is deliberately not used: it is a single shared instance that
     * stays marked as taken if a draw throws part way through, which would break every other user of it
     * for the rest of the session. Opening the pass here leaves nothing of Meteor's in a bad state, and
     * the pass is closed on the way out whatever happens.
     */
    private static void draw(GpuTextureView target, RenderPipeline pipeline, GpuBufferSlice data,
                             GpuTextureView source, GpuSampler sampler) {
        if (FullScreenRenderer.vbo == null || FullScreenRenderer.ibo == null) return;

        RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
            .createRenderPass(() -> "Vaultline - Gui effects", target, OptionalInt.empty());

        try {
            pass.setPipeline(pipeline);
            pass.setUniform("GuiData", data);
            pass.bindTexture("u_Texture", source, sampler);
            pass.setVertexBuffer(0, FullScreenRenderer.vbo);
            pass.setIndexBuffer(FullScreenRenderer.ibo, VertexFormat.IndexType.INT);
            pass.drawIndexed(0, 0, (int) (FullScreenRenderer.ibo.size() / Integer.BYTES), 1);
        } finally {
            pass.close();
        }
    }

    /** Releases the scratch target, for a window resize or a resource reload. */
    public static void release() {
        if (scratchView != null) {
            scratchView.close();
            scratchView = null;
        }

        if (scratch != null) {
            scratch.close();
            scratch = null;
        }

        scratchWidth = -1;
        scratchHeight = -1;
    }

    private static void ensurePipelines() {
        if (compiled) return;

        blurPipeline = build("pipeline/gui_blur", BLUR_SHADER);
        compositePipeline = build("pipeline/gui_composite", COMPOSITE_SHADER);

        if (blurPipeline == null || compositePipeline == null) {
            failed = true;
            return;
        }

        // The game only draws a pipeline it has compiled: an uncompiled one makes opening the pass
        // fail, which is why both effects could vanish without a trace on screen.
        precompile(blurPipeline);
        precompile(compositePipeline);
        compiled = true;
    }

    private static void precompile(RenderPipeline pipeline) {
        RenderSystem.getDevice().precompilePipeline(pipeline, GuiFx::shaderSource);
    }

    /** The shaders travel with the mod, so they are read from its own resources, not the packs. */
    private static String shaderSource(Identifier id, ShaderType type) {
        String path = "/assets/" + id.getNamespace() + "/" + id.getPath();

        try (InputStream in = GuiFx.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Missing shader " + path);

            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static RenderPipeline build(String location, Identifier fragment) {
        return RenderPipeline.builder()
            .withLocation(Identifier.of("vaultline", location))
            .withVertexFormat(MeteorVertexFormats.POS2, VertexFormat.DrawMode.TRIANGLES)
            .withVertexShader(VERTEX_SHADER)
            .withFragmentShader(fragment)
            .withSampler("u_Texture")
            .withUniform("GuiData", net.minecraft.client.gl.UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build();
    }

    private static boolean ensureScratch(int width, int height) {
        if (width <= 0 || height <= 0) return false;
        if (scratchView != null && scratchWidth == width && scratchHeight == height) return true;

        release();

        scratch = RenderSystem.getDevice().createTexture(
            "Vaultline - Gui blur", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC
                | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
            TextureFormat.RGBA8, width, height, 1, 1
        );
        scratchView = RenderSystem.getDevice().createTextureView(scratch);

        scratchWidth = width;
        scratchHeight = height;

        return true;
    }

    /** The std140 block the two shaders read. */
    private record Data(float[] rects, int count, int width, int height, float radius, int accent, float bloom)
        implements net.minecraft.client.gl.DynamicUniformStorage.Uploadable {

        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder builder = Std140Builder.intoBuffer(buffer);

            for (int i = 0; i < MAX_RECTS; i++) {
                if (i < count) {
                    int offset = i * 4;

                    builder.putVec4(rects[offset], rects[offset + 1], rects[offset + 2], rects[offset + 3]);
                } else {
                    builder.putVec4(0, 0, 0, 0);
                }
            }

            float alpha = ((accent >>> 24) & 0xFF) / 255f;
            float red = ((accent >> 16) & 0xFF) / 255f;
            float green = ((accent >> 8) & 0xFF) / 255f;
            float blue = (accent & 0xFF) / 255f;

            builder.putVec4(red, green, blue, Math.max(alpha, 0.35f));
            builder.putVec2(width, height);
            builder.putFloat(radius);
            builder.putFloat(bloom);
            builder.putFloat(count);
        }
    }
}
