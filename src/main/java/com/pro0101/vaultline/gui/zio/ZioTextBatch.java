package com.pro0101.vaultline.gui.zio;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;

import java.util.Arrays;

/**
 * Collects the glyph quads of one GUI frame and hands them over as one element per scissor region, the
 * way the copied client draws its whole interface from a single glyph mesh.
 *
 * <p>Everything is written into one reused float array: drawing a line of text allocates nothing, and a
 * flush copies only the run's used prefix. A flush also happens on every scissor change and before the
 * fills that must sit above the text (tooltips, chrome, the game panel, presets), so the order on screen
 * stays the order the screen drew in.
 */
final class ZioTextBatch {
    /** Glyph quads in flight before an early hand-over, which keeps a frame from growing unbounded. */
    private static final int MAX_QUADS = 8192;

    private static final Identifier ATLAS_ID = Identifier.of("vaultline", "font/zio_atlas");

    private static GuiRenderState state;
    private static float guiScale = 1f;
    private static ScreenRect scissor;
    private static float[] quads = new float[MAX_QUADS * ZioTextRenderState.STRIDE];
    private static int count;
    private static boolean active;

    private ZioTextBatch() {
    }

    /** Opens a batch for one GUI frame drawn at {@code guiScale} GUI units per layout unit. */
    static void begin(GuiRenderState renderState, float scale) {
        state = renderState;
        guiScale = scale;
        count = 0;
        scissor = null;
        active = true;
    }

    static boolean active() {
        return active;
    }

    static float scale() {
        return guiScale;
    }

    /** Sets the scissor the next glyphs belong to, in GUI pixels. */
    static void scissor(int x1, int y1, int x2, int y2) {
        if (!active) return;

        if (x2 <= x1 || y2 <= y1) {
            noScissor();
            return;
        }

        ScreenRect next = new ScreenRect(x1, y1, x2 - x1, y2 - y1);

        if (next.equals(scissor)) return;

        flush();
        scissor = next;
    }

    static void noScissor() {
        if (!active || scissor == null) return;

        flush();
        scissor = null;
    }

    /** Appends one quad, in GUI pixels with atlas texture coordinates. */
    static void add(float x1, float y1, float x2, float y2, float u1, float v1, float u2, float v2, int argb) {
        if (!active) return;
        if (count >= MAX_QUADS) flush();

        int offset = count * ZioTextRenderState.STRIDE;

        if (offset + ZioTextRenderState.STRIDE > quads.length) quads = Arrays.copyOf(quads, quads.length * 2);

        quads[offset] = x1;
        quads[offset + 1] = y1;
        quads[offset + 2] = x2;
        quads[offset + 3] = y2;
        quads[offset + 4] = u1;
        quads[offset + 5] = v1;
        quads[offset + 6] = u2;
        quads[offset + 7] = v2;
        quads[offset + 8] = argb;
        count++;
    }

    /** Hands the run collected so far over, so the fills drawn next land above it. */
    static void flush() {
        if (!active || count == 0) return;

        float[] run = Arrays.copyOf(quads, count * ZioTextRenderState.STRIDE);
        int quadsInRun = count;

        count = 0;

        if (state == null) return;

        TextureSetup texture = atlasTexture();
        if (texture == null) return;

        state.addPreparedTextElement(
            new ZioTextRenderState(run, quadsInRun, scissor, RenderPipelines.GUI_TEXTURED, texture));
    }

    static void end() {
        flush();
        active = false;
        state = null;
        scissor = null;
    }

    private static TextureSetup atlasTexture() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client == null) return null;

        AbstractTexture texture = client.getTextureManager().getTexture(ATLAS_ID);
        if (texture == null) return null;

        GpuTextureView view = texture.getGlTextureView();
        if (view == null) return null;

        return TextureSetup.of(view, texture.getSampler());
    }
}
