package com.pro0101.vaultline.gui.zio;

/**
 * The order one frame decides its tooltip in, as the copied client decides it.
 *
 * <p>{@code ClientScreen} lets every panel update and render — expansion, scroll, the offset of every
 * row — and only then reads the line under the pointer, lays out its rectangle and blurs it. Doing it
 * the other way round is what made the ported tooltip's text lag one frame behind its blurred
 * rectangle while a panel opened or closed: the rectangle moved with the frame, the text did not.
 *
 * <p>This exists so that order is one call, and can be checked without a screen: {@link #resolve}
 * steps the panels first, then asks them what is under the pointer, then lays the tooltip out from
 * that answer. The rectangle it returns is the one the blur pass is handed and the one the tooltip is
 * drawn in, so all three — line, rectangle, blur — belong to the same image.
 */
public final class ZioTooltipFrame {
    /** What a frame needs from the panel list: step its state, then read the line under a point. */
    public interface Panels {
        /** Advances expansion, scroll and every row's offset by one frame. */
        void step();

        /** The description under a point, after {@link #step} has run for this frame, or null. */
        String descriptionAt(double mouseX, double mouseY);
    }

    /** The tooltip of one frame: the lines to draw, and the rectangle the blur and drawing share. */
    public record Resolved(String[] lines, ZioPaint.Bounds bounds) {
    }

    private ZioTooltipFrame() {
    }

    /**
     * Steps the panels, then lays the tooltip out from the state they have just reached.
     *
     * @return the tooltip of this frame, or null when nothing under the pointer has a description
     */
    public static Resolved resolve(Panels panels, ZioCanvas canvas, int screenWidth, double pointerX,
                                   double pointerY, double mouseX, double mouseY) {
        panels.step();

        String description = panels.descriptionAt(mouseX, mouseY);

        if (description == null || description.isEmpty()) return null;

        String[] lines = description.split("\n");

        return new Resolved(lines, ZioPaint.tooltipBounds(canvas, screenWidth, (float) pointerX,
            (float) pointerY, lines));
    }
}
