package com.pro0101.vaultline.gui.zio;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.gl.RenderPipelines;
import com.pro0101.vaultline.mixins.DrawContextStateAccessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Draws the copied screen's text with the glyph atlas instead of the game's bitmap font.
 *
 * <p>The atlas is built once from {@link ZioFontConfig} (Arial at 16 by default, rasterised at twice
 * that size) and uploaded as a clamped, linearly sampled, mipmapped texture. Every character is then
 * one textured quad at a quarter of its rasterised size, tinted with the colour the call asks for.
 *
 * <p>Three details are copied from that client rather than chosen here: a colour with no alpha is made
 * opaque before it is used; the shadow pass is drawn first, under the line, at {@link #SHADOW_OFFSET}
 * floating point pixels down and right and with every channel but alpha quartered; and positions and
 * advances stay fractional, the way its {@code draw(poses, text, float x, float y, …)} keeps them.
 *
 * <p>Every glyph is one textured quad through the game's own GUI draw call. A batch that handed a run
 * of quads over as one GUI render state was tried and reverted: the game files simple elements into its
 * own blur layers, and a state whose bounds sit inside the previous one is pushed to a parent layer, so
 * the text came out behind the panels it belonged to.
 *
 * <p>If the atlas cannot be built — a headless JVM, a font that refuses to load — the renderer disables
 * itself once and the screen simply draws no text rather than breaking the frame.
 */
public final class ZioFontRenderer {
    private static final Logger LOG = LoggerFactory.getLogger("Vaultline GUI text");

    private static final Identifier ATLAS_ID = Identifier.of("vaultline", "font/zio_atlas");

    /**
     * How far the shadow sits from the line, in floating point pixels, exactly as the copied client
     * uses it: {@code drawPass(poses, text, x + FontRenderer.getShadowOffset(), y + ...)} where that
     * offset is {@code FontsModule.shadowSetting.getValue()}, a float whose default is 0.7
     * ({@code new NumberSetting<>("Shadow", 0.7F, 0.5F, 1.0F)}).
     */
    public static final float SHADOW_OFFSET = 0.7f;

    private static final String ATLAS_LABEL = "vaultline-zio-atlas";

    private static ZioFontAtlas atlas;
    private static boolean failed;
    private static boolean loggedFailure;

    static {
        ZioFontConfig.onChanged(ZioFontRenderer::dropAtlas);
    }

    private ZioFontRenderer() {
    }

    // Batching

    /** Opens the frame's glyph batch, which hands the text over as one element per scissor region. */
    public static void begin(DrawContext context, float guiScale) {
        ZioTextBatch.begin(((DrawContextStateAccessor) context).vaultline$state(), guiScale);
    }

    /** Hands the last run over and closes the batch. */
    public static void end() {
        ZioTextBatch.end();
    }

    /** Hands the run collected so far over, so the fills drawn next land above it. */
    public static void flush() {
        ZioTextBatch.flush();
    }

    /** Sets the scissor the next glyphs belong to, in layout units. */
    public static void scissor(int x1, int y1, int x2, int y2) {
        float guiScale = ZioTextBatch.scale();

        ZioTextBatch.scissor(Math.round(x1 * guiScale), Math.round(y1 * guiScale),
            Math.round(x2 * guiScale), Math.round(y2 * guiScale));
    }

    public static void noScissor() {
        ZioTextBatch.noScissor();
    }

    // Metrics

    /** The height of one line of text, in layout pixels. */
    public static int lineHeight() {
        ZioFontAtlas current = atlas();

        return current == null ? 9 : current.lineHeight();
    }

    /** The width a line of text takes, measured with the atlas. */
    public static int width(String text) {
        ZioFontAtlas current = atlas();

        return current == null ? 0 : current.width(text);
    }

    /** The width as it is measured before it is rounded, which is what centring needs. */
    public static float widthF(String text) {
        ZioFontAtlas current = atlas();

        return current == null ? 0f : current.widthF(text);
    }

    /** The left edge that centres a line on {@code centreX}. */
    public static float centeredX(String text, float centreX) {
        return centreX - widthF(text) / 2f;
    }

    public static int centeredX(String text, int centreX) {
        return Math.round(centeredX(text, (float) centreX));
    }

    // Drawing

    /** Draws a line of text, with the copied client's shadow pass, tinted with {@code argb}. */
    public static void draw(DrawContext context, String text, float x, float y, int argb) {
        draw(context, text, x, y, argb, true);
    }

    /**
     * Draws a line of text at a fraction of its size, which is how the copied client fits a label into
     * its eight pixel game header ({@code drawWithShadow(..., 0.8F, ...)}).
     *
     * <p>Nothing is rounded: the scale is folded into the quad coordinates, so a label at 0.8 keeps its
     * fractional place instead of snapping to a whole layout pixel first.
     */
    public static void drawScaled(DrawContext context, String text, float x, float y, int argb, float scale) {
        if (text == null || text.isEmpty() || scale <= 0f) return;

        ZioFontAtlas current = atlas();
        if (current == null) return;

        int colour = ZioFontAtlas.opaque(argb);

        if (ZioTextBatch.active()) {
            appendRun(current, text, x + SHADOW_OFFSET * scale, y + SHADOW_OFFSET * scale,
                ZioFontAtlas.shadow(colour), scale);
            appendRun(current, text, x, y, colour, scale);

            return;
        }

        Matrix3x2fStack matrices = context.getMatrices();

        matrices.pushMatrix();
        matrices.scale(scale, scale);
        drawImmediate(context, current, text, x, y, colour, true);
        matrices.popMatrix();
    }

    /** Draws a line of text, optionally without its shadow. */
    public static void draw(DrawContext context, String text, float x, float y, int argb, boolean shadow) {
        if (text == null || text.isEmpty()) return;

        ZioFontAtlas current = atlas();
        if (current == null) return;

        int colour = ZioFontAtlas.opaque(argb);

        if (ZioTextBatch.active()) {
            if (shadow) appendRun(current, text, x + SHADOW_OFFSET, y + SHADOW_OFFSET, ZioFontAtlas.shadow(colour), 1f);
            appendRun(current, text, x, y, colour, 1f);

            return;
        }

        drawImmediate(context, current, text, x, y, colour, shadow);
    }

    /** Appends one run of glyphs to the batch, in GUI pixels, at {@code scale}. */
    private static void appendRun(ZioFontAtlas current, String text, float x, float y, int argb, float scale) {
        float guiScale = ZioTextBatch.scale();
        int textureWidth = current.image().getWidth();
        int textureHeight = current.imageHeight();
        float unit = ZioFontAtlas.ATLAS_SCALE * scale * guiScale;
        float originX = x * guiScale;
        float originY = y * guiScale;
        float pen = 0f;

        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);

            if (character == '\u00a7') {
                i++;
                continue;
            }

            ZioFontAtlas.Glyph glyph = current.glyph(character);

            if (glyph != null) {
                float x1 = originX + pen * unit;
                float y1 = originY;
                float x2 = x1 + glyph.width() * unit;
                float y2 = y1 + glyph.height() * unit;

                ZioTextBatch.add(x1, y1, x2, y2,
                    glyph.x() / (float) textureWidth, glyph.y() / (float) textureHeight,
                    (glyph.x() + glyph.width()) / (float) textureWidth,
                    (glyph.y() + glyph.height()) / (float) textureHeight, argb);
            }

            pen += current.advanceF(character);
        }
    }

    /** The one path: a shadow pass first, one quad per glyph, kept to the copied client's fractions. */
    private static void drawImmediate(DrawContext context, ZioFontAtlas current, String text, float x, float y,
                                      int argb, boolean shadow) {
        if (shadow) {
            Matrix3x2fStack matrices = context.getMatrices();

            matrices.pushMatrix();
            matrices.translate(SHADOW_OFFSET, SHADOW_OFFSET);
            drawPass(context, current, text, x, y, ZioFontAtlas.shadow(argb));
            matrices.popMatrix();
        }

        drawPass(context, current, text, x, y, argb);
    }

    /** One pass of a line, one quad per glyph, placed at whole pixels for the immediate draw call. */
    private static void drawPass(DrawContext context, ZioFontAtlas current, String text, float x, float y, int argb) {
        BufferedImage image = current.image();
        if (image == null) return;

        int textureWidth = image.getWidth();
        int textureHeight = current.imageHeight();
        float unit = ZioFontAtlas.ATLAS_SCALE;
        float pen = 0f;

        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);

            if (character == '\u00a7') {
                i++;
                continue;
            }

            ZioFontAtlas.Glyph glyph = current.glyph(character);

            if (glyph != null) {
                // The glyph's atlas row is a source coordinate only: the destination is the line itself,
                // otherwise every glyph is pushed down by its place in the atlas.
                context.drawTexture(RenderPipelines.GUI_TEXTURED, ATLAS_ID,
                    Math.round(x + pen * unit), Math.round(y),
                    glyph.x(), glyph.y(),
                    Math.round(glyph.width() * unit), Math.round(glyph.height() * unit), glyph.width(), glyph.height(),
                    textureWidth, textureHeight, argb);
            }

            pen += current.advanceF(character);
        }
    }

    private static synchronized ZioFontAtlas atlas() {
        if (atlas != null || failed) return atlas;

        try {
            atlas = ZioFontAtlas.build(ZioFontConfig.name(), ZioFontConfig.size());
            upload(atlas);
        } catch (Throwable failure) {
            failed = true;
            atlas = null;

            if (!loggedFailure) {
                loggedFailure = true;
                LOG.warn("Could not build the GUI text atlas; the screen will draw no text", failure);
            }
        }

        return atlas;
    }

    /** Throws the built atlas away so the next frame rebuilds it from the new font. */
    private static synchronized void dropAtlas() {
        atlas = null;
        failed = false;
        loggedFailure = false;
    }

    /** Copies the atlas into a game texture, white glyphs with their own alpha, plus one halved level. */
    private static void upload(ZioFontAtlas built) {
        if (MeteorClient.mc == null || MeteorClient.mc.getTextureManager() == null) return;

        BufferedImage image = built.image();

        NativeImage[] levels = new NativeImage[]{
            toNativeImage(image, built.imageHeight()),
            toNativeImage(halve(image, built.imageHeight()), Math.max(1, built.imageHeight() / 2))
        };

        MeteorClient.mc.getTextureManager().registerTexture(ATLAS_ID,
            new ZioFontTexture(() -> ATLAS_LABEL, levels));
    }

    private static NativeImage toNativeImage(BufferedImage image, int height) {
        NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, image.getWidth(), height, false);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                nativeImage.setColorArgb(x, y, image.getRGB(x, y));
            }
        }

        return nativeImage;
    }

    /** The halved copy the second mip level is built from. */
    private static BufferedImage halve(BufferedImage image, int height) {
        int width = image.getWidth() / 2;
        int halfHeight = Math.max(1, height / 2);

        BufferedImage half = new BufferedImage(Math.max(1, width), halfHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = half.createGraphics();

        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(image, 0, 0, Math.max(1, width), halfHeight, null);
        graphics.dispose();

        return half;
    }
}
