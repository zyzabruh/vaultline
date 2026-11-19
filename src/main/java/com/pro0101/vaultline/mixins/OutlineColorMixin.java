package com.pro0101.vaultline.mixins;

import com.pro0101.vaultline.modules.render.Shaders;
import com.pro0101.vaultline.render.HandFx;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Colours outlines the game tints without asking the entity.
 *
 * <p>{@link EntityGlowMixin} answers {@code getTeamColorValue}, but some paths call
 * {@code setColor} with a plain white instead of asking. Only that white is replaced: a colour the
 * entity already answered is left untouched, so the per-kind colours still win.
 */
@Mixin(OutlineVertexConsumerProvider.class)
public abstract class OutlineColorMixin {
    /**
     * Substitutes the fallback colour for the plain white outline tint.
     *
     * @param color tint the game is about to use
     * @return tint to use instead
     */
    @ModifyVariable(method = "setColor", at = @At("HEAD"), argsOnly = true)
    private int vaultline$outlineColor(int color) {
        // The hands are drawn by their own pipeline and must never pick up an entity outline tint, so
        // the whole entity outline colouring stands down for the length of the hand pass.
        if (HandFx.ignoresHands()) return color;

        Shaders module = Modules.get().get(Shaders.class);

        if (module == null || !module.isActive()) return color;
        if (color != -1) return color;

        return module.fallback();
    }
}
