package com.pro0101.vaultline.render;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.block.BlockState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.MovingBlockRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The hand shader's selection, flag and cache rules, without a GPU.
 *
 * <p>What is checked here is everything the game cannot be asked about cheaply: the defaults ask for
 * nothing, the off look takes the vanilla path, the flag is raised only for the pass and read by the
 * queue gate, a failed build is retried instead of remembered, and a look never reuses another
 * look's cached object. Shader compilation and the pixels it produces are not exercised here; the
 * resources and uniforms behind the pipelines are checked in {@link HandPipelineTest}.
 */
class HandFxTest {
    /**
     * Every flag test starts from the idle state; a leaked flag would make the gate tests lie.
     */
    @BeforeEach
    void resetFlag() {
        HandFx.reset();
    }

    /**
     * Both toggles off selects no explicit look, which the module then turns into its default look.
     */
    @Test
    void bothTogglesOffSelectNoExplicitLook() {
        assertEquals(HandFx.Look.OFF, HandFx.Look.of(false, false));
        assertEquals(HandFx.Look.OFF, HandFx.Settings.DEFAULT.explicitLook());
        assertFalse(HandFx.Settings.DEFAULT.anyToggle());
    }

    /**
     * An enabled module always has a look to draw: no configuration makes it active and invisible.
     */
    @Test
    void enabledModuleAlwaysHasAVisibleLook() {
        assertEquals(HandFx.Look.GLOW, HandFx.DEFAULT_LOOK);
        assertEquals(HandFx.DEFAULT_LOOK, HandFx.Settings.DEFAULT.look());
        assertTrue(HandFx.Settings.DEFAULT.enabled());
        assertTrue(HandFx.retargets(HandFx.Settings.DEFAULT.look()));

        assertEquals(HandFx.Look.FILL, new HandFx.Settings(true, false).look());
        assertEquals(HandFx.Look.OUTLINE, new HandFx.Settings(false, true).look());
        assertEquals(HandFx.Look.GLOW, new HandFx.Settings(true, true).look());
    }

    /**
     * The toggles themselves still ship off, and the module builds its settings from these constants.
     */
    @Test
    void toggleDefaultsAreOff() {
        assertFalse(HandFx.DEFAULT_HAND_FILL);
        assertFalse(HandFx.DEFAULT_HAND_OUTLINE);
        assertEquals(new HandFx.Settings(false, false), HandFx.Settings.DEFAULT);
    }

    /**
     * Every other combination names the look the user asked for.
     */
    @Test
    void togglesSelectTheirLook() {
        assertEquals(HandFx.Look.FILL, HandFx.Look.of(true, false));
        assertEquals(HandFx.Look.OUTLINE, HandFx.Look.of(false, true));
        assertEquals(HandFx.Look.GLOW, HandFx.Look.of(true, true));
    }

    /**
     * The off look has no pipeline and no spec, which is why the vanilla layer is never retargeted.
     */
    @Test
    void offLookHasNoPipeline() {
        assertNull(HandFx.pipeline(HandFx.Look.OFF));
        assertNull(HandFx.spec(HandFx.Look.OFF));
        assertFalse(HandFx.retargets(HandFx.Look.OFF));
        assertFalse(HandFx.shouldRetarget(HandFx.Look.OFF));
        assertTrue(HandFx.retargets(HandFx.Look.FILL));
        assertTrue(HandFx.retargets(HandFx.Look.OUTLINE));
        assertTrue(HandFx.retargets(HandFx.Look.GLOW));
    }

    /**
     * A null layer stays null, and the off look hands the original back unchanged.
     */
    @Test
    void offLookReturnsTheOriginalLayer() {
        assertNull(HandFx.layer(null, HandFx.Look.GLOW));
        assertNull(HandFx.layer(null, HandFx.Look.OFF));
    }

    /**
     * The flag is raised for the hand pass and cleared at its end.
     */
    @Test
    void flagTracksTheHandPass() {
        assertFalse(HandFx.renderingHands());

        HandFx.Look previous = HandFx.enter(HandFx.Look.GLOW);

        assertTrue(HandFx.renderingHands());
        assertEquals(HandFx.Look.GLOW, HandFx.activeLook());

        HandFx.exit(previous);

        assertFalse(HandFx.renderingHands());
        assertEquals(HandFx.Look.OFF, HandFx.activeLook());
    }

    /**
     * Resetting works even from a nested enter, which is what a skipped pass needs.
     */
    @Test
    void resetClearsANestedPass() {
        HandFx.enter(HandFx.Look.FILL);
        HandFx.enter(HandFx.Look.GLOW);
        HandFx.reset();

        assertFalse(HandFx.renderingHands());
    }

    /**
     * The pass wrapper clears the flag even when the pass throws.
     */
    @Test
    void handPassRestoresTheFlagAfterAnException() {
        assertThrows(IllegalStateException.class, () -> HandFx.handPass(HandFx.Look.GLOW, () -> {
            throw new IllegalStateException("pass failed");
        }));

        assertFalse(HandFx.renderingHands());
    }

    /**
     * The flag is really consumed: the queue gate and the effects gate both see it.
     */
    @Test
    void flagIsConsumedByThePassAndByTheEffects() {
        assertFalse(HandFx.ignoresHands());
        assertFalse(HandFx.shouldRetarget(HandFx.Look.FILL));

        HandFx.handPass(HandFx.Look.FILL, () -> {
            assertTrue(HandFx.ignoresHands());
            assertTrue(HandFx.shouldRetarget(HandFx.Look.FILL));

            // A look other than the one the pass runs with is never retargeted, even inside the pass.
            assertFalse(HandFx.shouldRetarget(HandFx.Look.GLOW));
            assertFalse(HandFx.shouldRetarget(HandFx.Look.OFF));
        });

        assertFalse(HandFx.ignoresHands());
        assertFalse(HandFx.shouldRetarget(HandFx.Look.FILL));
    }

    /**
     * The pass wrapper hands back the body's result, so a pass can stay a single expression.
     */
    @Test
    void handPassReturnsTheBodyResult() {
        String result = HandFx.handPass(HandFx.Look.OUTLINE, () -> HandFx.activeLook().id());

        assertEquals("outline", result);
        assertFalse(HandFx.renderingHands());
    }

    /**
     * A failed build caches nothing, so the next attempt retries and the vanilla value is used until
     * one succeeds.
     */
    @Test
    void failedBuildIsRetriedAndNotCached() {
        HandCache<String> cache = new HandCache<>();
        AtomicInteger attempts = new AtomicInteger();

        String first = cache.resolve(HandFx.Look.FILL, look -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("shader missing");
        }, "vanilla");

        assertEquals("vanilla", first);
        assertEquals(1, attempts.get());
        assertEquals(0, cache.size());
        assertFalse(cache.has(HandFx.Look.FILL));

        String second = cache.resolve(HandFx.Look.FILL, look -> {
            attempts.incrementAndGet();

            return "hand";
        }, "vanilla");

        assertEquals("hand", second);
        assertEquals(2, attempts.get());
        assertEquals(1, cache.size());

        String third = cache.resolve(HandFx.Look.FILL, look -> {
            attempts.incrementAndGet();

            return "rebuilt";
        }, "vanilla");

        assertEquals("hand", third);
        assertEquals(2, attempts.get());
    }

    /**
     * A builder that returns null is a failure too, and leaves the cache empty.
     */
    @Test
    void nullBuildIsNotCached() {
        HandCache<String> cache = new HandCache<>();

        assertEquals("vanilla", cache.resolve(HandFx.Look.GLOW, look -> null, "vanilla"));
        assertEquals(0, cache.size());
    }

    /**
     * The off look never even calls the builder, which is how the vanilla path stays untouched.
     */
    @Test
    void offLookNeverBuildsOrCaches() {
        HandCache<String> cache = new HandCache<>();
        AtomicInteger attempts = new AtomicInteger();

        String result = cache.resolve(HandFx.Look.OFF, look -> {
            attempts.incrementAndGet();

            return "hand";
        }, "vanilla");

        assertEquals("vanilla", result);
        assertEquals(0, attempts.get());
        assertEquals(0, cache.size());
    }

    /**
     * Two looks never share an entry, so switching the toggles cannot serve the previous look.
     */
    @Test
    void looksDoNotShareEntries() {
        HandCache<String> cache = new HandCache<>();

        assertEquals("fill", cache.resolve(HandFx.Look.FILL, look -> "fill", "vanilla"));
        assertEquals("outline", cache.resolve(HandFx.Look.OUTLINE, look -> "outline", "vanilla"));
        assertEquals(2, cache.size());

        cache.clear();

        assertEquals(0, cache.size());
        assertFalse(cache.has(HandFx.Look.FILL));
    }

    /**
     * Clearing drops the flag as well, so a reload cannot leave the pass marked as running.
     */
    @Test
    void clearResetsTheFlag() {
        HandFx.enter(HandFx.Look.GLOW);
        HandFx.clear();

        assertFalse(HandFx.renderingHands());
    }

    /**
     * The off look is not even a decorator decision: the queue reports it and forwards the layer.
     */
    @Test
    void offQueueForwardsTheLayerUntouched() {
        RecordingQueue recorder = new RecordingQueue();
        HandQueue queue = new HandQueue(recorder, HandFx.Look.OFF);

        assertEquals(HandFx.Look.OFF, queue.look());
        assertSame(recorder, queue.delegate());

        queue.submitModelPart(null, null, null, 0, 0, null, false, false, 0, null, 0);

        assertEquals(1, recorder.modelParts);
        assertNull(recorder.lastLayer);
    }

    /**
     * Batch queues are decorated too: a batching queue is not an ordered one, and leaving it
     * untouched put the sorted half of the hands back on the vanilla pipeline.
     */
    @Test
    void batchingQueueIsDecorated() {
        RecordingQueue recorder = new RecordingQueue();
        HandQueue queue = new HandQueue(recorder, HandFx.Look.GLOW);

        Object batch = queue.getBatchingQueue(2);

        assertTrue(batch instanceof HandCommands, "batching queue was not decorated");
        assertEquals(HandFx.Look.GLOW, ((HandCommands) batch).look());
        assertSame(recorder, ((HandCommands) batch).delegate());
        assertTrue(recorder.batches > 0);
    }

    /**
     * The ordered queue is still an ordered queue, so the pass can keep using it.
     */
    @Test
    void orderedQueueKeepsItsDelegate() {
        RecordingQueue recorder = new RecordingQueue();
        HandQueue queue = new HandQueue(recorder, HandFx.Look.FILL);

        assertSame(recorder, queue.ordered());
        assertSame(recorder, queue.delegate());
        assertEquals(HandFx.Look.FILL, queue.look());
    }

    /**
     * The off look installs no decorator at all, which is the vanilla path at queue level.
     */
    @Test
    void wrapLeavesTheQueueAloneWhenOff() {
        RecordingQueue recorder = new RecordingQueue();

        assertSame(recorder, HandCommands.wrap(recorder, HandFx.Look.OFF));
        assertSame(recorder, HandCommands.wrap(recorder, null));

        Object wrapped = HandCommands.wrap(recorder, HandFx.Look.GLOW);

        assertTrue(wrapped instanceof HandCommands);
        assertSame(recorder, ((HandCommands) wrapped).delegate());
    }

    /**
     * Records the calls the decorator makes, so the vanilla path can be checked without a layer.
     */
    private static final class RecordingQueue implements OrderedRenderCommandQueue {
        private int modelParts;
        private int batches;
        private RenderLayer lastLayer;

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
            this.modelParts++;
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
