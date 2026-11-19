package com.pro0101.vaultline.render;

/**
 * The first-person pass the hand shader wraps.
 *
 * <p>Written out because the renderer has two {@code renderItem} overloads and only this one carries
 * the queue.
 *
 * <p>It lives here rather than on the mixin itself: Mixin refuses any static field that is not
 * private on a mixin class, and a private constant could not be checked from a test.
 */
public final class HandTarget {
    /**
     * Descriptor of the first-person pass.
     */
    public static final String RENDER_ITEM =
        "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;"
            + "Lnet/minecraft/client/network/ClientPlayerEntity;I)V";

    private HandTarget() {
    }
}
