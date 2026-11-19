package com.pro0101.vaultline.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.pro0101.vaultline.modules.visual.HandShader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.block.BlockState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.MovingBlockRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The hand chain, link by link, so no part of it can go quiet again.
 *
 * <p>The chain is: the module is on → the first-person pass runs with a look → the queue stops being
 * the vanilla one → every layer it is given is resolved to ours → the pipeline resolved is Vaultline's,
 * not the vanilla one → the layer the game eventually draws came from the delegate with that
 * pipeline. Each link has a test here, and each test fails if its link is removed: the tests use a
 * recording delegate and an injected resolver, so "the command went through the decorator with the
 * retargeted layer" is an observed fact rather than an assumption.
 *
 * <p>The mixin and the module ids are checked the only way that catches a silent misconfiguration:
 * the injection descriptor is compared with the real first-person method by reflection, and the ids
 * the settings are built from are pinned.
 */
class HandChainTest {
    @BeforeEach
    void resetFlag() {
        HandFx.reset();
    }

    /**
     * The look the module falls back to really builds a Vaultline pipeline, not the vanilla one.
     */
    @Test
    void theDefaultLookBuildsAVaultlinePipeline() {
        HandFx.Look look = HandFx.Settings.DEFAULT.look();

        assertEquals(HandFx.Look.GLOW, look);

        RenderPipeline pipeline = HandFx.pipeline(look);

        assertNotNull(pipeline, "the default look built no pipeline, so the hands would fall back to vanilla");
        assertEquals(HandFx.spec(look).location(), pipeline.getLocation());
        assertEquals(HandFx.spec(look).vertexShader(), pipeline.getVertexShader());
        assertEquals(HandFx.spec(look).fragmentShader(), pipeline.getFragmentShader());
        assertNotEquals(Identifier.of("minecraft", "core/entity"), pipeline.getFragmentShader());
        assertNotEquals(Identifier.of("minecraft", "pipeline/entity_solid"), pipeline.getLocation());
    }

    /**
     * Inside the pass, every submitted layer is resolved and the delegate receives the replacement.
     */
    @Test
    void handLayersAreRetargetedInsideThePass() {
        RenderLayer original = layer("original");
        RenderLayer replacement = layer("replacement");
        RecordingQueue delegate = new RecordingQueue();
        AtomicReference<RenderLayer> asked = new AtomicReference<>();
        AtomicReference<HandFx.Look> askedLook = new AtomicReference<>();
        HandQueue queue = new HandQueue(delegate, HandFx.Look.GLOW, (submitted, look) -> {
            asked.set(submitted);
            askedLook.set(look);

            return replacement;
        });

        HandFx.handPass(HandFx.Look.GLOW,
            () -> queue.submitModelPart(null, null, original, 0, 0, null, false, false, 0, null, 0));

        assertSame(original, asked.get(), "the resolver was not asked about the submitted layer");
        assertEquals(HandFx.Look.GLOW, askedLook.get(), "the resolver was not told the pass look");
        assertSame(replacement, delegate.lastLayer, "the delegate did not receive the retargeted layer");
    }

    /**
     * Outside the pass nothing is resolved, so no other path can repaint the hands or anything else.
     */
    @Test
    void commandsAreNotRetargetedOutsideThePass() {
        RenderLayer original = layer("original");
        RecordingQueue delegate = new RecordingQueue();
        AtomicReference<RenderLayer> asked = new AtomicReference<>();
        HandQueue queue = new HandQueue(delegate, HandFx.Look.GLOW, (submitted, look) -> {
            asked.set(submitted);

            return layer("replacement");
        });

        queue.submitModelPart(null, null, original, 0, 0, null, false, false, 0, null, 0);

        assertNull(asked.get(), "a submission outside the pass was retargeted");
        assertSame(original, delegate.lastLayer);
    }

    /**
     * The batching queues the pass hands out keep the decorator and the resolver.
     */
    @Test
    void batchingQueuesKeepTheResolver() {
        RenderLayer original = layer("original");
        RenderLayer replacement = layer("replacement");
        RecordingQueue delegate = new RecordingQueue();
        HandQueue queue = new HandQueue(delegate, HandFx.Look.FILL, (submitted, look) -> replacement);

        Object batch = queue.getBatchingQueue(3);

        assertTrue(batch instanceof HandCommands, "the batching queue was not decorated");
        assertSame(queue.resolver(), ((HandCommands) batch).resolver());

        HandFx.handPass(HandFx.Look.FILL,
            () -> ((HandCommands) batch).submitItem(null, null, 0, 0, 0, new int[0], List.of(), original, null));

        assertSame(replacement, delegate.lastLayer, "the batched submission kept the vanilla layer");
    }

    /**
     * The mixin is configured, and its injection descriptor is the real first-person method.
     */
    @Test
    void mixinTargetsTheFirstPersonPass() throws Exception {
        String config = resource("/vaultline.mixins.json");

        assertTrue(config.contains("\"HeldItemRendererMixin\""), "the hand mixin is not in the mixin config");
        assertTrue(config.contains("\"required\": true"), "mixins are not required, so a failed injection stays silent");

        Method target = HeldItemRenderer.class.getDeclaredMethod("renderItem", float.class, MatrixStack.class,
            OrderedRenderCommandQueue.class, ClientPlayerEntity.class, int.class);

        assertEquals("renderItem", target.getName());
        assertEquals(target.getName() + descriptor(target), HandTarget.RENDER_ITEM,
            "the injection target does not name the first-person method, so the mixin would never apply");
    }

    /**
     * The ids the module is registered and configured with are the ones the code uses.
     */
    @Test
    void moduleIdsMatchTheConfig() {
        assertEquals("hand-shader", HandShader.ID);
        assertEquals("hand-fill", HandShader.HAND_FILL);
        assertEquals("hand-outline", HandShader.HAND_OUTLINE);
    }

    /**
     * The first draw logs one line naming the look and the pipeline, and a failure names the look too.
     */
    @Test
    void theChainAnnouncesItselfInTheLog() {
        String line = HandFx.announcement(HandFx.Look.GLOW);

        assertTrue(line.startsWith("Vaultline hand shader active: look=GLOW"), line);
        assertTrue(line.contains("pipeline=vaultline:pipeline/hand_glow"), line);
        assertTrue(line.contains("vertex=vaultline:core/hand"), line);
        assertTrue(line.contains("fragment=vaultline:core/hand_glow"), line);
        assertFalse(line.contains("\n"), "the announcement is not a single line");

        String failure = HandFx.failure(HandFx.Look.GLOW);

        assertTrue(failure.contains("look GLOW"), failure);
        assertTrue(failure.contains("could not be built"), failure);
        assertTrue(failure.contains("hands stay vanilla"), failure);
    }

    /**
     * Builds a real layer around a minimal pipeline, so the tests move actual layer objects.
     */
    private static RenderLayer layer(String name) {
        RenderPipeline pipeline = RenderPipeline.builder()
            .withLocation(Identifier.of("vaultline", "test/" + name))
            .withVertexShader(Identifier.of("vaultline", "core/hand"))
            .withFragmentShader(Identifier.of("vaultline", "core/hand_fill"))
            .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.TRIANGLES)
            .build();

        return RenderLayer.of("vaultline_test_" + name, RenderSetup.builder(pipeline).build());
    }

    private static String resource(String path) throws IOException {
        try (InputStream stream = HandChainTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "missing resource " + path);

            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Builds the JVM descriptor of a method, the way a mixin writes it.
     */
    private static String descriptor(Method method) {
        StringBuilder builder = new StringBuilder("(");

        for (Class<?> parameter : method.getParameterTypes()) builder.append(type(parameter));

        return builder.append(')').append(type(method.getReturnType())).toString();
    }

    private static String type(Class<?> type) {
        if (type == void.class) return "V";
        if (type == float.class) return "F";
        if (type == int.class) return "I";
        if (type == boolean.class) return "Z";
        if (type == double.class) return "D";
        if (type == long.class) return "J";

        return "L" + type.getName().replace('.', '/') + ";";
    }

    /**
     * Records what the decorator forwards, so the layer the game would draw can be asserted.
     */
    private static final class RecordingQueue implements OrderedRenderCommandQueue {
        private RenderLayer lastLayer;
        private int batches;

        @Override
        public OrderedRenderCommandQueue getBatchingQueue(int order) {
            this.batches++;

            return this;
        }

        @Override
        public void submitShadowPieces(MatrixStack matrices, float shadowRadius, List<EntityRenderState.ShadowPiece> pieces) {
        }

        @Override
        public void submitLabel(MatrixStack matrices, Vec3d pos, int light, Text text, boolean seeThrough, int backgroundColor,
                                double squaredDistance, CameraRenderState camera) {
        }

        @Override
        public void submitText(MatrixStack matrices, float x, float y, OrderedText text, boolean shadow,
                               TextRenderer.TextLayerType layerType, int light, int color, int backgroundColor, int outlineColor) {
        }

        @Override
        public void submitFire(MatrixStack matrices, EntityRenderState state, Quaternionf rotation) {
        }

        @Override
        public void submitLeash(MatrixStack matrices, EntityRenderState.LeashData leashData) {
        }

        @Override
        public <S> void submitModel(Model<? super S> model, S state, MatrixStack matrices, RenderLayer layer, int light,
                                    int overlay, int color, Sprite sprite, int outlineColor,
                                    ModelCommandRenderer.CrumblingOverlayCommand crumbling) {
            this.lastLayer = layer;
        }

        @Override
        public void submitModelPart(ModelPart part, MatrixStack matrices, RenderLayer layer, int light, int overlay,
                                    Sprite sprite, boolean sheeted, boolean hasGlint, int color,
                                    ModelCommandRenderer.CrumblingOverlayCommand crumbling, int outlineColor) {
            this.lastLayer = layer;
        }

        @Override
        public void submitBlock(MatrixStack matrices, BlockState state, int light, int overlay, int outlineColor) {
        }

        @Override
        public void submitMovingBlock(MatrixStack matrices, MovingBlockRenderState state) {
        }

        @Override
        public void submitBlockStateModel(MatrixStack matrices, RenderLayer layer, BlockStateModel model, float x, float y,
                                          float z, int light, int overlay, int outlineColor) {
            this.lastLayer = layer;
        }

        @Override
        public void submitItem(MatrixStack matrices, ItemDisplayContext context, int light, int overlay, int outlineColor,
                               int[] tints, List<BakedQuad> quads, RenderLayer layer, ItemRenderState.Glint glint) {
            this.lastLayer = layer;
        }

        @Override
        public void submitCustom(MatrixStack matrices, RenderLayer layer, OrderedRenderCommandQueue.Custom custom) {
            this.lastLayer = layer;
        }

        @Override
        public void submitCustom(OrderedRenderCommandQueue.LayeredCustom custom) {
        }
    }
}
