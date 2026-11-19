package com.pro0101.vaultline.mixins;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the setup of a built render layer.
 *
 * <p>Retargeting a hand layer means building a second layer around the same textures; the setup is
 * where those textures live, and the class offers no getter for it.
 */
@Mixin(RenderLayer.class)
public interface RenderLayerAccessor {
    /**
     * The setup this layer was built from.
     *
     * @return the render setup
     */
    @Accessor("renderSetup")
    RenderSetup vaultline$renderSetup();
}
