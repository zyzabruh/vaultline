package com.pro0101.vaultline.mixins;

import com.pro0101.vaultline.modules.render.Skybox;
import com.pro0101.vaultline.render.SkyboxRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.CloudRenderer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the vanilla clouds while the shader sky is drawn.
 *
 * <p>Clouds are drawn after the sky pass and in front of it, so a shader sky with its own cloud noise
 * gets two weather systems stacked on top of each other. Cancelling the cloud pass is enough: nothing
 * else in the frame reads it, and the option is off by default so the vanilla clouds stay unless the
 * user asks for it.
 */
@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin {
    /**
     * Skips the cloud pass when the option is on and the sky is replaced.
     *
     * @param color cloud colour
     * @param mode cloud render mode
     * @param cloudHeight cloud height
     * @param cameraPos camera position
     * @param time world time
     * @param tickProgress partial tick
     * @param info injection callback
     */
    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void vaultline$hideClouds(int color, CloudRenderMode mode, float cloudHeight, Vec3d cameraPos,
                                      long time, float tickProgress, CallbackInfo info) {
        Skybox module = Modules.get().get(Skybox.class);

        if (module == null || !module.isActive() || !SkyboxRenderer.available()) return;
        if (!module.enabledInDimension() || !module.hideClouds()) return;

        info.cancel();
    }
}
