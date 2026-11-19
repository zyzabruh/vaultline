package com.pro0101.vaultline.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderCommandQueue;

/**
 * The replacement queue that puts the hands on Vaultline's pipelines.
 *
 * <p>Everything the game submits while drawing the first-person hand goes through this decorator:
 * each submission is forwarded to the real queue unchanged except for its render layer, which is
 * retargeted by {@link HandFx#layer(RenderLayer, HandFx.Look)}. The batching queues the ordered
 * queue hands out are decorated as well, because a batching queue is a plain
 * {@link RenderCommandQueue} and the item half of the hand is sorted through one.
 *
 * <p>With the look off the decorator is not installed, and this class is never on the path: the
 * caller keeps the vanilla queue, which is how the default configuration stays bit-for-bit vanilla.
 */
public final class HandQueue extends HandCommands implements OrderedRenderCommandQueue {
    /**
     * Wraps an ordered queue with the production layer builder.
     *
     * @param delegate vanilla queue every call is forwarded to
     * @param look look the hand submissions are retargeted to
     */
    public HandQueue(OrderedRenderCommandQueue delegate, HandFx.Look look) {
        super(delegate, look);
    }

    /**
     * Wraps an ordered queue with an explicit layer builder.
     *
     * @param delegate vanilla queue every call is forwarded to
     * @param look look the hand submissions are retargeted to
     * @param resolver layer builder to use for every submission
     */
    public HandQueue(OrderedRenderCommandQueue delegate, HandFx.Look look, LayerResolver resolver) {
        super(delegate, look, resolver);
    }

    public OrderedRenderCommandQueue ordered() {
        return (OrderedRenderCommandQueue) delegate();
    }

    @Override
    public RenderCommandQueue getBatchingQueue(int order) {
        // A batching queue is not an ordered queue, so it is wrapped by the plain decorator; handing
        // it back untouched would leave the sorted half of the hands on the vanilla pipeline.
        return wrap(ordered().getBatchingQueue(order), look(), resolver());
    }
}
