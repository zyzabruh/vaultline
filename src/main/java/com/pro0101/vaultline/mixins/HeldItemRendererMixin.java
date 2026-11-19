package com.pro0101.vaultline.mixins;

import com.pro0101.vaultline.modules.visual.HandShader;
import com.pro0101.vaultline.render.HandFx;
import com.pro0101.vaultline.render.HandQueue;
import com.pro0101.vaultline.render.HandTarget;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Puts the first-person hands on Vaultline's pipelines.
 *
 * <p>The pass is re-run through a {@link HandQueue}: the geometry, the poses, the swing, the second
 * hand, swimming, sleeping and the FOV handling all stay vanilla, and only the render type each hand
 * part is submitted with changes. The flag is raised by {@link HandFx#handPass} for exactly the
 * length of the re-run and restored in its {@code finally}, so a pass that throws leaves nothing
 * behind.
 *
 * <p>The flag also guards the injection itself: the re-run goes through the same method, so a pass
 * that sees the flag already raised for its own look lets the body run instead of wrapping it again.
 * With both options off the body runs untouched, which is the vanilla path.
 */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    /**
     * Runs the hand pass through the queue decorator.
     *
     * @param tickDelta partial tick
     * @param matrices hand matrix stack
     * @param queue queue the game built for this pass
     * @param player rendered player
     * @param light packed light
     * @param info injection callback
     */
    @Inject(method = HandTarget.RENDER_ITEM, at = @At("HEAD"), cancellable = true)
    private void vaultline$handPass(float tickDelta, MatrixStack matrices, OrderedRenderCommandQueue queue,
                                   ClientPlayerEntity player, int light, CallbackInfo info) {
        HandShader module = Modules.get().get(HandShader.class);
        HandFx.Look look = module != null && module.isActive() ? module.current().look() : HandFx.Look.OFF;

        if (!HandFx.retargets(look)) {
            // Vanilla path: nothing is intercepted and no flag may survive into it.
            HandFx.reset();

            return;
        }

        if (HandFx.shouldRetarget(look)) {
            // Our own re-run: the flag is already up for this look, so let the body run.
            return;
        }

        info.cancel();

        HandFx.handPass(look, () -> ((HeldItemRenderer) (Object) this)
            .renderItem(tickDelta, matrices, new HandQueue(queue, look), player, light));
    }
}
