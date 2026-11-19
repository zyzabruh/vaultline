package com.pro0101.vaultline.mixins;

import java.util.Map;
import net.minecraft.client.render.LayeringTransform;
import net.minecraft.client.render.OutputTarget;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.TextureTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the flags and textures of a render setup so a layer can be rebuilt around another pipeline.
 *
 * <p>Only reads are needed: the values are copied onto a {@link RenderSetup.Builder}, which then
 * builds the hand layer. Writing through these accessors would change vanilla layers, which the
 * effect must never do.
 *
 * <p>Every flag is read, not just the obvious ones. A layer rebuilt without its output target or its
 * outline mode draws in the wrong place, or stops contributing to the outline pass, and the hand
 * then looks broken in a way that is hard to trace back to a missing copy.
 */
@Mixin(RenderSetup.class)
public interface RenderSetupAccessor {
    /**
     * Textures the setup binds, keyed by sampler name.
     *
     * @return the texture map, values are the setup's texture specs
     */
    @Accessor("textures")
    Map<String, ?> vaultline$textures();

    /**
     * Whether the setup samples the lightmap.
     *
     * @return true when the lightmap is bound
     */
    @Accessor("useLightmap")
    boolean vaultline$useLightmap();

    /**
     * Whether the setup samples the overlay.
     *
     * @return true when the overlay is bound
     */
    @Accessor("useOverlay")
    boolean vaultline$useOverlay();

    /**
     * Whether the setup supports crumbling overlay pieces.
     *
     * @return true when crumbling is on
     */
    @Accessor("hasCrumbling")
    boolean vaultline$crumbling();

    /**
     * Whether the setup is drawn with translucency.
     *
     * @return true when the layer is translucent
     */
    @Accessor("translucent")
    boolean vaultline$translucent();

    /**
     * Target the layer draws into.
     *
     * @return the output target
     */
    @Accessor("outputTarget")
    OutputTarget vaultline$outputTarget();

    /**
     * Transform applied before the layer is drawn.
     *
     * @return the layering transform
     */
    @Accessor("layeringTransform")
    LayeringTransform vaultline$layeringTransform();

    /**
     * Transform applied to the texture coordinates.
     *
     * @return the texture transform
     */
    @Accessor("textureTransform")
    TextureTransform vaultline$textureTransform();

    /**
     * How the layer takes part in the outline pass.
     *
     * @return the outline mode
     */
    @Accessor("outlineMode")
    RenderSetup.OutlineMode vaultline$outlineMode();

    /**
     * Buffer size the layer asks for.
     *
     * @return the expected buffer size
     */
    @Accessor("expectedBufferSize")
    int vaultline$expectedBufferSize();
}
