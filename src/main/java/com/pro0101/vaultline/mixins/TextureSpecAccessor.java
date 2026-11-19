package com.pro0101.vaultline.mixins;

import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the texture identifier out of a render setup's texture spec.
 *
 * <p>The spec is package-private, so the accessor mixin is the only way to get from a built layer
 * back to the identifier a new setup can be given.
 */
@Mixin(targets = "net.minecraft.client.render.RenderSetup$TextureSpec")
public interface TextureSpecAccessor {
    /**
     * The texture the spec resolves.
     *
     * @return the texture identifier
     */
    @Accessor("location")
    Identifier vaultline$location();
}
