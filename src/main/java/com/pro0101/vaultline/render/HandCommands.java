package com.pro0101.vaultline.render;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.MovingBlockRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderCommandQueue;
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

/**
 * A render queue that forwards every submission, swapping the render layer on the way through.
 *
 * <p>Two kinds of queue carry hand geometry: the ordered queue the pass is handed, and the batching
 * queues it hands out for sorted geometry. Both are decorated, which is why this class is typed on
 * the plain {@link RenderCommandQueue}: a batching queue is not an ordered one, and skipping it
 * leaves the item half of the hands on the vanilla pipeline without a word.
 *
 * <p>Only submissions that carry a layer are touched. Labels, name tags, fire, leashes and shadow
 * pieces have no layer and are forwarded as they came in; with the look off the decorator is not
 * created at all ({@link #wrap(RenderCommandQueue, HandFx.Look)}).
 */
public class HandCommands implements RenderCommandQueue {
    /**
     * Turns one vanilla layer into the layer to submit instead.
     *
     * <p>The seam exists so the chain can be exercised without a GPU: a test can hand the decorator a
     * resolver that records what it was asked for and answer with a sentinel, and then check that the
     * delegate received the sentinel. Production uses {@link HandFx#layer(RenderLayer, HandFx.Look)}.
     */
    public interface LayerResolver {
        /**
         * Resolves the layer to submit.
         *
         * @param look look the pass runs with
         */
        RenderLayer resolve(RenderLayer layer, HandFx.Look look);
    }

    private final RenderCommandQueue delegate;
    private final HandFx.Look look;
    private final LayerResolver resolver;

    /**
     * Wraps a queue for one look with the production layer builder.
     *
     * @param delegate queue every call is forwarded to
     * @param look look the submissions are retargeted to
     */
    public HandCommands(RenderCommandQueue delegate, HandFx.Look look) {
        this(delegate, look, HandFx::layer);
    }

    /**
     * Wraps a queue for one look.
     *
     * @param delegate queue every call is forwarded to
     * @param look look the submissions are retargeted to
     * @param resolver layer builder to use for every submission
     */
    public HandCommands(RenderCommandQueue delegate, HandFx.Look look, LayerResolver resolver) {
        this.delegate = delegate;
        this.look = look;
        this.resolver = resolver;
    }

    /**
     * Wraps a queue, or leaves it alone when the effect is off.
     *
     * <p>The off look must not install a decorator at all: that is what makes "off means vanilla"
     * true at the queue level and not only at the layer level.
     *
     * @param look look to draw with
     * @return the decorator, or the original queue when the look is off
     */
    public static RenderCommandQueue wrap(RenderCommandQueue delegate, HandFx.Look look) {
        return wrap(delegate, look, HandFx::layer);
    }

    /**
     * Wraps a queue with an explicit layer builder, or leaves it alone when the effect is off.
     *
     * @param look look to draw with
     * @param resolver layer builder to use
     * @return the decorator, or the original queue when the look is off
     */
    public static RenderCommandQueue wrap(RenderCommandQueue delegate, HandFx.Look look, LayerResolver resolver) {
        if (!HandFx.retargets(look)) return delegate;

        return new HandCommands(delegate, look, resolver);
    }

    public RenderCommandQueue delegate() {
        return this.delegate;
    }

    public HandFx.Look look() {
        return this.look;
    }

    public LayerResolver resolver() {
        return this.resolver;
    }

    /**
     * The layer to submit instead of the one the game asked for.
     *
     * <p>The hand-pass flag decides: outside the pass, or with a look other than the one this queue
     * was built for, the vanilla layer goes through untouched. That is what keeps a decorator that
     * outlives its pass from repainting anything else.
     *
     * @return our layer, or the vanilla one when the pass is not running with this look
     */
    protected RenderLayer hand(RenderLayer layer) {
        return HandFx.shouldRetarget(this.look) ? this.resolver.resolve(layer, this.look) : layer;
    }

    @Override
    public void submitShadowPieces(MatrixStack matrices, float shadowRadius, List<EntityRenderState.ShadowPiece> pieces) {
        this.delegate.submitShadowPieces(matrices, shadowRadius, pieces);
    }

    @Override
    public void submitLabel(MatrixStack matrices, Vec3d pos, int light, Text text, boolean seeThrough, int backgroundColor,
                            double squaredDistance, CameraRenderState camera) {
        this.delegate.submitLabel(matrices, pos, light, text, seeThrough, backgroundColor, squaredDistance, camera);
    }

    @Override
    public void submitText(MatrixStack matrices, float x, float y, OrderedText text, boolean shadow,
                           TextRenderer.TextLayerType layerType, int light, int color, int backgroundColor, int outlineColor) {
        this.delegate.submitText(matrices, x, y, text, shadow, layerType, light, color, backgroundColor, outlineColor);
    }

    @Override
    public void submitFire(MatrixStack matrices, EntityRenderState state, Quaternionf rotation) {
        this.delegate.submitFire(matrices, state, rotation);
    }

    @Override
    public void submitLeash(MatrixStack matrices, EntityRenderState.LeashData leashData) {
        this.delegate.submitLeash(matrices, leashData);
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, MatrixStack matrices, RenderLayer layer, int light,
                                int overlay, int color, Sprite sprite, int outlineColor,
                                ModelCommandRenderer.CrumblingOverlayCommand crumbling) {
        this.delegate.submitModel(model, state, matrices, hand(layer), light, overlay, color, sprite, outlineColor, crumbling);
    }

    @Override
    public void submitModelPart(ModelPart part, MatrixStack matrices, RenderLayer layer, int light, int overlay,
                                Sprite sprite, boolean sheeted, boolean hasGlint, int color,
                                ModelCommandRenderer.CrumblingOverlayCommand crumbling, int outlineColor) {
        this.delegate.submitModelPart(part, matrices, hand(layer), light, overlay, sprite, sheeted, hasGlint, color, crumbling,
            outlineColor);
    }

    @Override
    public void submitBlock(MatrixStack matrices, BlockState state, int light, int overlay, int outlineColor) {
        this.delegate.submitBlock(matrices, state, light, overlay, outlineColor);
    }

    @Override
    public void submitMovingBlock(MatrixStack matrices, MovingBlockRenderState state) {
        this.delegate.submitMovingBlock(matrices, state);
    }

    @Override
    public void submitBlockStateModel(MatrixStack matrices, RenderLayer layer, BlockStateModel model, float x, float y,
                                      float z, int light, int overlay, int outlineColor) {
        this.delegate.submitBlockStateModel(matrices, hand(layer), model, x, y, z, light, overlay, outlineColor);
    }

    @Override
    public void submitItem(MatrixStack matrices, ItemDisplayContext context, int light, int overlay, int outlineColor,
                           int[] tints, List<BakedQuad> quads, RenderLayer layer, ItemRenderState.Glint glint) {
        this.delegate.submitItem(matrices, context, light, overlay, outlineColor, tints, quads, hand(layer), glint);
    }

    @Override
    public void submitCustom(MatrixStack matrices, RenderLayer layer, OrderedRenderCommandQueue.Custom custom) {
        this.delegate.submitCustom(matrices, hand(layer), custom);
    }

    @Override
    public void submitCustom(OrderedRenderCommandQueue.LayeredCustom custom) {
        this.delegate.submitCustom(custom);
    }
}
