package com.pro0101.vaultline.gui.zio;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;

import java.util.function.Supplier;

/**
 * The GUI text atlas as a game texture.
 *
 * <p>This mirrors the copied client's own font texture: two mip levels written straight into a GPU
 * texture, clamped on both axes and sampled linearly. Linear sampling is what keeps the shrunk glyphs
 * smooth instead of stair-stepped, and it is the sampler that client's atlas is created with
 * ({@code MipmapFontTexture.createTexture}:
 * {@code getSamplerCache().getSampler(CLAMP_TO_EDGE, CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, true)}).
 *
 * <p>That sampler is the one the draw really uses. A {@code RenderPipeline} in this version only keeps
 * a list of sampler <em>names</em> ({@code RenderPipeline.getSamplers()}), never sampler state, and the
 * {@code GUI_TEXTURED} pipeline is built as
 * {@code RenderPipeline.builder(POSITION_TEX_COLOR_SNIPPET).withLocation("pipeline/gui_textured").build()}
 * with no sampler override at all (its snippet is only {@code core/position_tex_color},
 * {@code Sampler0}, {@code BlendFunction.TRANSLUCENT}, {@code VertexFormats.POSITION_TEXTURE_COLOR},
 * {@code QUADS}, {@code NO_DEPTH_TEST}). {@code DrawContext.drawTexturedQuad} then resolves the texture
 * by id and builds the draw state from {@code AbstractTexture.getGlTextureView()} and
 * {@code AbstractTexture.getSampler()} — this texture's sampler.
 */
public final class ZioFontTexture extends AbstractTexture {
    /** The base atlas plus one halved level, like the copied client's font texture. */
    private static final int MIP_LEVELS = 2;

    private NativeImage[] images;

    public ZioFontTexture(Supplier<String> label, NativeImage[] images) {
        this.images = images;
        createTexture(label);
        upload();
    }

    private void createTexture(Supplier<String> label) {
        GpuDevice device = RenderSystem.getDevice();
        NativeImage baseImage = images[0];

        glTexture = device.createTexture(label, 5, TextureFormat.RGBA8,
            baseImage.getWidth(), baseImage.getHeight(), 1, MIP_LEVELS);

        sampler = RenderSystem.getSamplerCache().get(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
            FilterMode.LINEAR, FilterMode.LINEAR, true);

        glTextureView = device.createTextureView(glTexture, 0, MIP_LEVELS);
    }

    private void upload() {
        if (glTexture == null || images == null) return;

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

        for (int level = 0; level < images.length; level++) {
            NativeImage image = images[level];

            encoder.writeToTexture(glTexture, image, level, 0, 0, 0,
                image.getWidth(), image.getHeight(), 0, 0);
        }
    }

    @Override
    public void close() {
        super.close();

        images = null;
    }
}
