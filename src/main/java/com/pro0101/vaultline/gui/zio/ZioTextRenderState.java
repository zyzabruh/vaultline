package com.pro0101.vaultline.gui.zio;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

/**
 * One run of glyph quads, handed to the game as a single prepared text element: every quad of a run is
 * written straight into the frame's buffer, so a line of text costs one element instead of one per
 * glyph — the same shape as the copied client's {@code glyphMesh}.
 *
 * <p>It is added through {@code addPreparedTextElement} rather than {@code addSimpleElement}: the latter
 * compares bounds with the previous element and pushes anything nested inside them into a parent layer,
 * which put the text behind the panels.
 */
final class ZioTextRenderState implements SimpleGuiElementRenderState {
    /** Nine floats per quad: two corners, two texture coordinates and one colour. */
    static final int STRIDE = 9;

    private final float[] quads;
    private final int count;
    private final Matrix3x2f pose = new Matrix3x2f();
    private final ScreenRect scissor;
    private final ScreenRect bounds;
    private final RenderPipeline pipeline;
    private final TextureSetup texture;

    ZioTextRenderState(float[] quads, int count, ScreenRect scissor, RenderPipeline pipeline, TextureSetup texture) {
        this.quads = quads;
        this.count = count;
        this.scissor = scissor;
        this.pipeline = pipeline;
        this.texture = texture;
        this.bounds = boundsOf(quads, count);
    }

    private static ScreenRect boundsOf(float[] quads, int count) {
        if (count <= 0) return null;

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (int i = 0; i < count; i++) {
            int offset = i * STRIDE;

            minX = Math.min(minX, quads[offset]);
            minY = Math.min(minY, quads[offset + 1]);
            maxX = Math.max(maxX, quads[offset + 2]);
            maxY = Math.max(maxY, quads[offset + 3]);
        }

        int left = (int) Math.floor(minX);
        int top = (int) Math.floor(minY);
        int width = Math.max(1, (int) Math.ceil(maxX) - left);
        int height = Math.max(1, (int) Math.ceil(maxY) - top);

        return new ScreenRect(left, top, width, height);
    }

    @Override
    public void setupVertices(VertexConsumer consumer) {
        for (int i = 0; i < count; i++) {
            int offset = i * STRIDE;
            float x1 = quads[offset];
            float y1 = quads[offset + 1];
            float x2 = quads[offset + 2];
            float y2 = quads[offset + 3];
            float u1 = quads[offset + 4];
            float v1 = quads[offset + 5];
            float u2 = quads[offset + 6];
            float v2 = quads[offset + 7];
            int argb = (int) quads[offset + 8];

            consumer.vertex(pose, x1, y1).texture(u1, v1).color(argb);
            consumer.vertex(pose, x1, y2).texture(u1, v2).color(argb);
            consumer.vertex(pose, x2, y2).texture(u2, v2).color(argb);
            consumer.vertex(pose, x2, y1).texture(u2, v1).color(argb);
        }
    }

    @Override
    public RenderPipeline pipeline() {
        return pipeline;
    }

    @Override
    public TextureSetup textureSetup() {
        return texture;
    }

    @Override
    public ScreenRect scissorArea() {
        return scissor;
    }

    @Override
    public ScreenRect bounds() {
        return bounds;
    }
}
