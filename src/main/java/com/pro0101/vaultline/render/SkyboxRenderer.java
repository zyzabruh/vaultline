package com.pro0101.vaultline.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.pro0101.vaultline.VaultlineAddon;
import com.pro0101.vaultline.modules.render.Skybox;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

/**
 * Draws the skybox as one fullscreen quad in the sky pass.
 *
 * <p>Each mode owns a render pipeline and a layer built from it. The vertex shader passes the
 * clip-space corners through untouched and the fragment shader turns them back into a view
 * direction with the inverse of the projection and model-view matrices, so the sky follows the
 * camera without any geometry of its own.
 *
 * <p>The procedural modes are built once, at class load. The image mode cannot be: its texture only
 * exists once the user's file has been read, so its layer is built on the first frame that draws it.
 * A pipeline that cannot be created leaves the vanilla sky in place rather than taking the addon
 * down, and a missing image falls back to the Sunset layer so the screen never goes black.
 */
public final class SkyboxRenderer {
    private static final Map<Skybox.Mode, RenderLayer> LAYERS = new EnumMap<>(Skybox.Mode.class);

    private static RenderLayer imageLayer;
    private static Identifier imageTexture;

    private static boolean available;

    static {
        boolean built = true;

        for (Skybox.Mode mode : Skybox.Mode.values()) {
            // The image mode binds a texture that does not exist yet, so it is built on demand.
            if (mode == Skybox.Mode.Image) continue;

            RenderLayer layer = build(mode);

            if (layer == null) {
                built = false;
                continue;
            }

            LAYERS.put(mode, layer);
        }

        available = built;

        if (!built) {
            VaultlineAddon.LOG.error(
                "Skybox shaders are unavailable, the vanilla sky stays in place."
            );
        }
    }

    /**
     * The accent colour the shaders take their phenomenon from.
     *
     * <p>The module's tint is an accent, not a filter: it colours the aurora curtains, the lightning,
     * the nebula gas and the sunset halo, while the sky behind them keeps its own gradient. The
     * scalar folds in the brightness, the time of day and the rain, so one value reaches the shader
     * through the vertex colour it already reads.
     *
     * @param red red channel, 0 to 1
     * @param green green channel, 0 to 1
     * @param blue blue channel, 0 to 1
     */
    public record Accent(float red, float green, float blue) {
        /**
         * Clamps the channels into the range the vertex colour accepts.
         */
        public Accent {
            red = clamp(red);
            green = clamp(green);
            blue = clamp(blue);
        }
    }

    private SkyboxRenderer() {}

    /**
     * Scales a tint into the accent the shaders draw with.
     *
     *
     * @param red tint red, 0 to 1
     * @param green tint green, 0 to 1
     * @param blue tint blue, 0 to 1
     * @param scale brightness, time of day and rain folded into one factor
     */
    public static Accent accent(float red, float green, float blue, float scale) {
        return new Accent(red * scale, green * scale, blue * scale);
    }

    /**
     * How much of the sky is in night, from the world time.
     *
     * <p>The sun height is a cosine of the day fraction, so the value rises through dusk, sits at one
     * all night and falls again at dawn. It is what lets a night sky fade out by day instead of being
     * painted over a blue sky.
     *
     * @param timeOfDay world time in ticks
     * @return 0 in full day, 1 in full night
     */
    public static float nightFactor(long timeOfDay) {
        double fraction = Math.floorMod(timeOfDay, 24000L) / 24000.0;
        double sunHeight = Math.cos((fraction - 0.25) * 2.0 * Math.PI);

        return (float) (1.0 - smoothstep(-0.15, 0.1, sunHeight));
    }

    /**
     * @param rainGradient rain gradient, 0 to 1
     * @return 1 when dry, 0.45 in the heaviest rain
     */
    public static float rainFactor(float rainGradient) {
        return 1.0F - 0.55F * clamp(rainGradient);
    }

    /**
     * @param edge0 lower edge
     * @param edge1 upper edge
     * @return 0 below the lower edge, 1 above the upper one
     */
    private static double smoothstep(double edge0, double edge1, double value) {
        double t = Math.min(1.0, Math.max(0.0, (value - edge0) / (edge1 - edge0)));

        return t * t * (3.0 - 2.0 * t);
    }

    /**
     * @return the clamped value
     */
    private static float clamp(float value) {
        return Math.min(1.0F, Math.max(0.0F, value));
    }

    /**
     * @return true when the skybox can be drawn
     */
    public static boolean available() {
        return available;
    }

    /**
     * @param mode skybox mode to draw
     * @param red tint red, from 0 to 1
     * @param green tint green, from 0 to 1
     * @param blue tint blue, from 0 to 1
     * @param imageName panorama file used by the image mode
     * @param speed animation multiplier, carried to the shader in the tint alpha
     */
    public static void render(Skybox.Mode mode, Accent accent, String imageName, float speed) {
        if (mode == Skybox.Mode.Image) {
            renderImage(accent, imageName, speed);
            return;
        }

        RenderLayer layer = LAYERS.get(mode);

        if (layer == null || !available) return;

        draw(layer, accent, speed);
    }

    /**
     * Draws the user's panorama, or the Sunset sky when there is no usable image.
     *
     * @param red tint red, from 0 to 1
     * @param green tint green, from 0 to 1
     * @param blue tint blue, from 0 to 1
     * @param imageName panorama file to draw
     * @param speed animation multiplier, carried to the shader in the tint alpha
     */
    private static void renderImage(Accent accent, String imageName, float speed) {
        Identifier texture = SkyboxImage.get(imageName);

        if (texture == null) {
            RenderLayer fallback = LAYERS.get(Skybox.Mode.Sunset);

            if (fallback != null) draw(fallback, accent, speed);

            return;
        }

        if (imageLayer == null || !texture.equals(imageTexture)) {
            imageLayer = buildImage(texture);
            imageTexture = texture;
        }

        if (imageLayer != null) draw(imageLayer, accent, speed);
    }

    /**
     * Submits the fullscreen quad to a layer.
     *
     * @param layer layer to draw with
     * @param red tint red, from 0 to 1
     * @param green tint green, from 0 to 1
     * @param blue tint blue, from 0 to 1
     * @param speed animation multiplier, written to the alpha so the shader receives it without a
     *              uniform block of its own
     */
    private static void draw(RenderLayer layer, Accent accent, float speed) {
        BufferBuilder buffer = Tessellator.getInstance().begin(
            VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR
        );

        buffer.vertex(-1.0F, -1.0F, 0.0F).color(accent.red(), accent.green(), accent.blue(), speed);
        buffer.vertex(1.0F, -1.0F, 0.0F).color(accent.red(), accent.green(), accent.blue(), speed);
        buffer.vertex(1.0F, 1.0F, 0.0F).color(accent.red(), accent.green(), accent.blue(), speed);
        buffer.vertex(-1.0F, 1.0F, 0.0F).color(accent.red(), accent.green(), accent.blue(), speed);

        BuiltBuffer built = buffer.end();

        if (built == null) return;

        layer.draw(built);
    }

    /**
     * Builds the pipeline and layer of one procedural mode.
     *
     *
     * @return layer, or null when the pipeline could not be created
     */
    private static RenderLayer build(Skybox.Mode mode) {
        String name = mode.name().toLowerCase(Locale.ROOT);

        try {
            return RenderLayer.of(
                "vaultline_skybox_" + name,
                RenderSetup.builder(pipelineBuilder(name).build()).build()
            );
        } catch (RuntimeException exception) {
            VaultlineAddon.LOG.error("Skybox mode " + name + " failed to build", exception);
            return null;
        }
    }

    /**
     * Builds the layer of the image mode around the user's texture.
     *
     * @param texture panorama to bind as Sampler0
     * @return layer, or null when the pipeline could not be created
     */
    private static RenderLayer buildImage(Identifier texture) {
        try {
            return RenderLayer.of(
                "vaultline_skybox_image",
                RenderSetup.builder(pipelineBuilder("image").withSampler("Sampler0").build())
                    .texture("Sampler0", texture)
                    .build()
            );
        } catch (RuntimeException exception) {
            VaultlineAddon.LOG.error("Skybox image mode failed to build", exception);
            return null;
        }
    }

    /**
     * @param name shader and pipeline suffix
     * @return a builder the caller finishes, so the image mode can declare its sampler
     */
    private static RenderPipeline.Builder pipelineBuilder(String name) {
        return RenderPipeline.builder()
            .withLocation(Identifier.of("vaultline", "pipeline/skybox_" + name))
            .withVertexShader(Identifier.of("vaultline", "core/skybox"))
            .withFragmentShader(Identifier.of("vaultline", "core/skybox_" + name))
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("Globals", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false);
    }
}
