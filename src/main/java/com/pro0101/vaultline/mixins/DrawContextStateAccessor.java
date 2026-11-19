package com.pro0101.vaultline.mixins;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reaches the render state a {@link DrawContext} collects its elements into. */
@Mixin(DrawContext.class)
public interface DrawContextStateAccessor {
    @Accessor("state")
    GuiRenderState vaultline$state();
}
