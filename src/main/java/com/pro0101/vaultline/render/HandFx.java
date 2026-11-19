package com.pro0101.vaultline.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.pro0101.vaultline.VaultlineAddon;
import com.pro0101.vaultline.mixins.RenderLayerAccessor;
import com.pro0101.vaultline.mixins.RenderSetupAccessor;
import com.pro0101.vaultline.mixins.TextureSpecAccessor;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

/**
 * The first-person hands, drawn through Vaultline's own pipelines instead of the vanilla ones.
 *
 * <p>The game still builds the hand geometry — the arm, the item, the map, the swing, the swim and
 * sleep poses all come from vanilla code, so a modified FOV, a second hand, swimming, sleeping and a
 * paused world keep working without a second implementation of any of them. What changes is the
 * render type each hand part is submitted with: the queue decorator in {@link HandCommands} swaps the
 * layer of every hand submission for one of ours, and our layer carries our pipeline and our
 * fragment stage.
 *
 * <p>Every look is a separate {@link Spec} with its own fragment stage, not a uniform toggled at
 * draw time. Both toggles off is not "off": an enabled module falls back to {@link #DEFAULT_LOOK}, so
 * switching the module on can never produce a module that is active and invisible.
 * {@link Look#OFF} is only ever reached by the module being disabled, and it selects no pipeline and
 * no layer, so the vanilla render type reaches the vanilla pipeline untouched.
 *
 * <p>Nothing built here is remembered after a failure ({@link HandCache}), the only live state is the
 * {@link #renderingHands()} flag, and the first hand drawn with a look logs the look and the pipeline
 * with its shaders, so a silent hand pass can be told apart from a configuration mistake in
 * {@code latest.log} without guessing.
 */
public final class HandFx {
    /** Look used when the module is on and neither toggle is: the module must do something visible. */
    public static final Look DEFAULT_LOOK = Look.GLOW;

    /** Default of the hand-fill option: off, and the default look covers it. */
    public static final boolean DEFAULT_HAND_FILL = false;

    /** Default of the hand-outline option: off, and the default look covers it. */
    public static final boolean DEFAULT_HAND_OUTLINE = false;

    private static final Identifier VERTEX_SHADER = Identifier.of("vaultline", "core/hand");

    private static final List<String> UNIFORMS = List.of("DynamicTransforms", "Projection");

    /**
     * Samplers the hand stages read, and what binds each one.
     *
     * <p>Every sampler the GLSL reads is listed here with its feed, and the layer builder satisfies
     * every feed: the texture map for {@code Sampler0}, and the lightmap and overlay flags for the
     * other two. A sampler that is declared but never bound leaves the stage reading an empty texture,
     * which draws nothing and says nothing.
     */
    private static final List<Sampler> SAMPLERS = List.of(
        new Sampler("Sampler0", Feed.TEXTURE),
        new Sampler("Sampler1", Feed.OVERLAY),
        new Sampler("Sampler2", Feed.LIGHTMAP)
    );

    /** Pixels the hand stages write, so the fill and the rim are blended over the arm. */
    private static final BlendFunction BLEND = BlendFunction.TRANSLUCENT;

    /** Vertex layout the hand stages declare: the entity layout vanilla writes hand geometry in. */
    private static final VertexFormat HAND_FORMAT = VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;

    /**
     * Draw mode vanilla writes hand geometry in.
     *
     * <p>Quads, not triangles. A model part emits four corners per face, so a pipeline that declares
     * triangles reads each group of four corners as one triangle and a third of the next face: the
     * hands then come out as spikes shooting across the screen instead of arms.
     */
    private static final VertexFormat.DrawMode HAND_MODE = VertexFormat.DrawMode.QUADS;

    /** What the hands are drawn as. Off is the vanilla path. */
    public enum Look {
        OFF,
        FILL,
        OUTLINE,
        GLOW;

        /**
         * Picks the look the two toggles name, without the module-level default.
         *
         * @return FILL, OUTLINE, GLOW, or OFF when both toggles are off
         */
        public static Look of(boolean fill, boolean outline) {
            if (fill && outline) return GLOW;
            if (fill) return FILL;
            if (outline) return OUTLINE;

            return OFF;
        }

        /**
         * Lower-case name used in identifiers and layer names.
         *
         */
        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * The hand settings as the module reports them.
     *
     * <p>Both toggles off is not a hidden switch: {@link #look()} answers the {@link #DEFAULT_LOOK},
     * so an enabled module always draws something. {@link #explicitLook()} is what the toggles alone
     * say, and is what the config shows.
     *
     * @param handFill tinted fill on the hands
     * @param handOutline rim on the hands
     */
    public record Settings(boolean handFill, boolean handOutline) {
        /** The shipped defaults: both toggles off, and the module's default look applies. */
        public static final Settings DEFAULT = new Settings(DEFAULT_HAND_FILL, DEFAULT_HAND_OUTLINE);

        /**
         * The look the toggles name, with OFF for both off.
         *
         */
        public Look explicitLook() {
            return Look.of(this.handFill, this.handOutline);
        }

        /**
         * The look the renderer uses: the toggles' look, or the default look when they name none.
         *
         * @return the look to draw, never {@link Look#OFF}
         */
        public Look look() {
            Look explicit = explicitLook();

            return explicit == Look.OFF ? DEFAULT_LOOK : explicit;
        }

        /**
         * Whether a toggle selects a look explicitly.
         *
         * @return true when at least one toggle is on
         */
        public boolean anyToggle() {
            return explicitLook() != Look.OFF;
        }

        /**
         * Whether these settings ask for a hand effect, which the default look now always does.
         *
         * @return true when an effect is drawn
         */
        public boolean enabled() {
            return look() != Look.OFF;
        }
    }

    /**
     * Where a sampler's texture comes from, which is the part a pipeline cannot declare by itself.
     */
    public enum Feed {
        /** Bound by the layer's own texture map, under the sampler name. */
        TEXTURE,
        /** Bound by the layer when it asks for the lightmap. */
        LIGHTMAP,
        /** Bound by the layer when it asks for the overlay. */
        OVERLAY
    }

    /**
     * One sampler a hand stage reads, and what has to bind it.
     *
     * @param name sampler name as written in the GLSL and in the pipeline
     * @param feed what binds it at draw time
     */
    public record Sampler(String name, Feed feed) {
    }

    /**
     * Everything one hand pipeline is made of.
     *
     * <p>The record is pure data, so the parts a GPU would otherwise have to prove — which shaders a
     * pipeline loads, which uniforms and samplers it declares, which vertex layout it expects, and
     * which blend/depth/cull state it draws with — can be checked without a game running and without
     * a driver.
     *
     * @param uniforms uniform blocks the stages read
     * @param samplers samplers the stages read, with what binds each one
     * @param vertexFormat vertex layout the geometry is written in
     * @param depthWrite whether the look writes depth
     * @param blend blend state, null for none
     * @param cull whether back faces are culled
     */
    public record Spec(
        Identifier location,
        Identifier vertexShader,
        Identifier fragmentShader,
        List<String> uniforms,
        List<Sampler> samplers,
        VertexFormat vertexFormat,
        VertexFormat.DrawMode mode,
        DepthTestFunction depthTest,
        boolean depthWrite,
        BlendFunction blend,
        boolean cull
    ) {
        /**
         * Copies the spec lists so a shared mutable list cannot change a pipeline after the fact.
         */
        public Spec {
            uniforms = List.copyOf(uniforms);
            samplers = List.copyOf(samplers);
        }

        public List<String> samplerNames() {
            return this.samplers.stream().map(Sampler::name).toList();
        }
    }

    /** One original layer and the look it was retargeted to. */
    private record Key(RenderLayer original, Look look) {}

    /** One geometry layout a hand pipeline can be built for. */
    private record Layout(VertexFormat format, VertexFormat.DrawMode mode) {}

    private static final Map<Look, Spec> SPECS = Map.of(
        Look.FILL, spec(Look.FILL, true),
        Look.OUTLINE, spec(Look.OUTLINE, false),
        Look.GLOW, spec(Look.GLOW, true)
    );

    /**
     * One pipeline cache per geometry layout.
     *
     * <p>A pipeline declares the layout its vertices are written in, so a look cannot have a single
     * pipeline for every layer it replaces: an arm, a held item and a held map are not the same
     * geometry. The layout is part of the key, so each shape gets a pipeline that matches it instead
     * of one layout being imposed on all of them.
     */
    private static final Map<Layout, HandCache<RenderPipeline>> PIPELINES = new HashMap<>();
    private static final Map<Key, RenderLayer> LAYERS = new HashMap<>();

    private static Look active = Look.OFF;
    private static Look announced = Look.OFF;

    private static int serial;

    private HandFx() {
    }

    /**
     * The pipeline description of one look.
     *
     * @return the spec, or null for {@link Look#OFF}, which has no pipeline
     */
    public static Spec spec(Look look) {
        return look == null ? null : SPECS.get(look);
    }

    /**
     * Whether the hand pass is running right now.
     *
     * <p>This is the flag the passes and the effects read: {@link HandCommands} only retargets while
     * it is on, and the entity effects that would otherwise touch every submitted model skip the
     * hands while it is on, so nothing is drawn twice.
     *
     * @return true while the game is drawing first-person hand geometry
     */
    public static boolean renderingHands() {
        return active != Look.OFF;
    }

    /**
     * The look the hand pass runs with, or {@link Look#OFF} outside the pass.
     *
     */
    public static Look activeLook() {
        return active;
    }

    /**
     * Whether a look changes the render type at all.
     *
     * @return true when the look has its own pipeline
     */
    public static boolean retargets(Look look) {
        return look != null && look != Look.OFF;
    }

    /**
     * Whether this submission should be retargeted right now.
     *
     * <p>Both halves matter: the look has to exist and the pass has to be running with that same
     * look. A decorator that outlives its pass, or a pass that changes look halfway through, therefore
     * retargets nothing and leaves the vanilla layer alone.
     *
     * @param look look the submission belongs to
     * @return true when the layer should be replaced
     */
    public static boolean shouldRetarget(Look look) {
        return retargets(look) && active == look;
    }

    /**
     * Whether effects that must not touch the hands have to stand down.
     *
     * @return true while the hand pass is running
     */
    public static boolean ignoresHands() {
        return renderingHands();
    }

    /**
     * Marks the hand pass as running.
     *
     * @param look look to run with
     * @return the look that was active before, to hand back to {@link #exit(Look)}
     */
    public static Look enter(Look look) {
        Look previous = active;
        active = look;

        return previous;
    }

    /**
     * Restores the look a previous {@link #enter(Look)} replaced.
     *
     * @param previous value returned by that call
     */
    public static void exit(Look previous) {
        active = previous;
    }

    /**
     * Clears the flag without a saved value, for the end of the hand pass.
     *
     * <p>The pass is the only writer, so restoring off is always correct, and doing it
     * unconditionally means a cancelled or skipped render cannot leave the flag set for the world.
     */
    public static void reset() {
        active = Look.OFF;
    }

    /**
     * Runs a body with the hand pass flag raised, and clears it whatever the body does.
     *
     * @param look look the pass draws with
     * @param body work to run inside the pass
     */
    public static void handPass(Look look, Runnable body) {
        Look previous = enter(look);

        try {
            body.run();
        } finally {
            exit(previous);
        }
    }

    /**
     * Runs a body with the hand pass flag raised and hands it back its result.
     *
     * @param look look the pass draws with
     * @param body work to run inside the pass
     */
    public static <T> T handPass(Look look, Supplier<T> body) {
        Look previous = enter(look);

        try {
            return body.get();
        } finally {
            exit(previous);
        }
    }

    /**
     * The pipeline of one look.
     *
     * @return the pipeline, or null when the look is {@link Look#OFF} or its build failed
     */
    public static RenderPipeline pipeline(Look look) {
        return pipeline(look, HAND_FORMAT, HAND_MODE);
    }

    /**
     * The pipeline of one look for one geometry layout.
     *
     * @param format vertex layout the geometry is written in
     * @param mode draw mode the geometry is written in
     * @return the pipeline, or null when the look is {@link Look#OFF} or its build failed
     */
    public static RenderPipeline pipeline(Look look, VertexFormat format, VertexFormat.DrawMode mode) {
        if (!retargets(look) || format == null || mode == null) return null;

        return PIPELINES.computeIfAbsent(new Layout(format, mode), layout -> new HandCache<>())
            .resolve(look, missing -> {
                try {
                    return buildPipeline(missing, format, mode);
                } catch (RuntimeException | LinkageError exception) {
                    // Not cached, so the next frame retries; the hands stay vanilla until a build works.
                    VaultlineAddon.LOG.error(failure(missing) + " [pipeline]", exception);

                    return null;
                }
            }, null);
    }

    /**
     * Whether the hand stages can read a geometry layout.
     *
     * <p>The vertex stage declares exactly the attributes of {@link #HAND_FORMAT}. A layer written in
     * another layout — the glint, a held map — has no values for some of them, and drawing it with
     * our pipeline would hand the shader garbage, so those layers keep their vanilla pipeline.
     *
     * @return true when every attribute the stage reads is written by the layout
     */
    public static boolean readable(VertexFormat format) {
        return format != null && format.getElements().equals(HAND_FORMAT.getElements());
    }

    /**
     * The hand layer replacing a vanilla one.
     *
     * <p>The layer keeps the original's textures and every setup flag it has — lightmap, overlay,
     * crumbling, translucency, output target, layering and texture transforms, outline mode and buffer
     * size — so the arm still samples the player skin, still lands in the same target and still takes
     * part in the outline pass. Only the pipeline changes, and it is built for the layout the layer
     * itself declares, so the topology of a face is never imposed from outside. A look of
     * {@link Look#OFF}, a null layer, a layer whose layout the hand stages cannot read, a look whose
     * pipeline is not available, or a layer that failed to build all return the original, which is
     * what keeps the vanilla path bit-for-bit.
     *
     * @param original vanilla layer the game submitted
     * @param look look to draw with
     * @return the layer to submit instead
     */
    public static RenderLayer layer(RenderLayer original, Look look) {
        if (!retargets(look) || original == null) return original;

        Key key = new Key(original, look);
        RenderLayer cached = LAYERS.get(key);

        if (cached != null) return cached;

        try {
            // The layout comes from the layer being replaced, never from the look: the geometry was
            // written before we see it, and a pipeline that disagrees with it misreads every vertex.
            VertexFormat format = original.getVertexFormat();

            if (!readable(format)) return original;

            RenderPipeline pipeline = pipeline(look, format, original.getDrawMode());

            if (pipeline == null) return original;

            RenderSetupAccessor source = (RenderSetupAccessor) (Object) ((RenderLayerAccessor) original).vaultline$renderSetup();
            RenderSetup.Builder setup = RenderSetup.builder(pipeline)
                .outputTarget(source.vaultline$outputTarget())
                .layeringTransform(source.vaultline$layeringTransform())
                .textureTransform(source.vaultline$textureTransform())
                .outlineMode(source.vaultline$outlineMode())
                .expectedBufferSize(source.vaultline$expectedBufferSize());

            // The hand stages read the overlay and the lightmap, so the layer has to ask for both
            // whatever the source layer did: an unbound sampler draws nothing at all.
            setup.useLightmap();
            setup.useOverlay();
            if (source.vaultline$crumbling()) setup.crumbling();
            if (source.vaultline$translucent()) setup.translucent();

            for (Map.Entry<String, ?> entry : source.vaultline$textures().entrySet()) {
                Identifier texture = ((TextureSpecAccessor) entry.getValue()).vaultline$location();

                if (texture != null) setup.texture(entry.getKey(), texture);
            }

            // Every rebuilt layer carries its own name: a skin, an item atlas and a map each become
            // their own layer, and two layers sharing a name would be indistinguishable to anything
            // that keys on it.
            RenderLayer built = RenderLayer.of("vaultline_hand_" + look.id() + "_" + (serial++), setup.build());

            LAYERS.put(key, built);

            announce(look);

            return built;
        } catch (RuntimeException | LinkageError exception) {
            // A broken layer must cost the hand effect, never the game. Nothing is stored, so this
            // frame draws vanilla and the next attempt retries instead of serving a broken layer.
            LAYERS.remove(key);
            VaultlineAddon.LOG.error(failure(look) + " [layer]", exception);

            return original;
        }
    }

    /**
     * Says once, and once per look change, what the hand pass is actually drawing with.
     *
     * <p>A single unambiguous line in {@code latest.log}: if it is missing, the pass never ran; if it
     * names a look and a pipeline, the chain is live and any remaining problem is inside the shader.
     *
     * @param look look the first hand layer was built for
     */
    private static void announce(Look look) {
        if (announced == look) return;

        announced = look;

        VaultlineAddon.LOG.info("{}", announcement(look));
    }

    /**
     * The single line logged the first time a look draws.
     *
     * <p>Written as one string so a test can pin what a reader of {@code latest.log} will see: the
     * look, the pipeline and both shader stages, on one unbroken line.
     *
     * @param look look that started drawing
     */
    static String announcement(Look look) {
        Spec spec = spec(look);

        return "Vaultline hand shader active: look=" + look
            + " pipeline=" + spec.location()
            + " vertex=" + spec.vertexShader()
            + " fragment=" + spec.fragmentShader();
    }

    /**
     * The line logged when a look cannot be built.
     *
     * @return the message, logged with the exception so the stack is in the log too
     */
    static String failure(Look look) {
        return "Vaultline hand shader: look " + look + " could not be built, hands stay vanilla";
    }

    /**
     * Forgets every built layer and pipeline, and the announcement with them.
     *
     * <p>Layers carry texture state from the pack that built them, so a resource reload has to drop
     * them; the pipelines are dropped with them because their shaders come from the same pack.
     */
    public static void clear() {
        PIPELINES.clear();
        LAYERS.clear();
        announced = Look.OFF;
        reset();
    }

    /**
     * Describes one look.
     *
     * @param depthWrite whether the look writes depth
     */
    private static Spec spec(Look look, boolean depthWrite) {
        return new Spec(
            Identifier.of("vaultline", "pipeline/hand_" + look.id()),
            VERTEX_SHADER,
            Identifier.of("vaultline", "core/hand_" + look.id()),
            UNIFORMS,
            SAMPLERS,
            HAND_FORMAT,
            HAND_MODE,
            DepthTestFunction.LEQUAL_DEPTH_TEST,
            depthWrite,
            BLEND,
            false
        );
    }

    /**
     * Builds the pipeline of one look from its spec.
     *
     */
    private static RenderPipeline buildPipeline(Look look, VertexFormat format, VertexFormat.DrawMode mode) {
        Spec spec = spec(look);

        if (spec == null) return null;

        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(spec.location())
            .withVertexShader(spec.vertexShader())
            .withFragmentShader(spec.fragmentShader())
            .withVertexFormat(format, mode)
            .withDepthTestFunction(spec.depthTest())
            .withDepthWrite(spec.depthWrite())
            .withCull(spec.cull());

        if (spec.blend() == null) builder.withoutBlend();
        else builder.withBlend(spec.blend());

        for (String uniform : spec.uniforms()) builder.withUniform(uniform, UniformType.UNIFORM_BUFFER);
        for (Sampler sampler : spec.samplers()) builder.withSampler(sampler.name());

        return builder.build();
    }
}
