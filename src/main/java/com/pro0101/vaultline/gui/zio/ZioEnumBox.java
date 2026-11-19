package com.pro0101.vaultline.gui.zio;

/**
 * The box an open enumeration draws: the accent bar that slides between its choices, and the one pixel
 * outline that closes the box.
 *
 * <p>{@code EnumSettingComponent} keeps its open list as a single component, so its selection bar is a
 * rectangle that moves from the first choice to the chosen one as the value changes
 * ({@code selectionOffsetAnimation}, an eased approach at twice the animation speed). This screen keeps
 * one line per choice instead, so the same rectangle is computed from the line the setting sits on and
 * how far the bar has slid.
 */
public final class ZioEnumBox {
    /** The bar's left inset: the setting line's two pixel padding and the client's one more. */
    public static final int BAR_INSET = 3;

    /** The bar and the outline stop two pixels short of the panel's right edge. */
    public static final int EDGE_INSET = 2;

    private ZioEnumBox() {
    }

    /**
     * The sliding bar, given how far it has slid from the first choice.
     *
     * <p>A slide that is still negative — the bar is on its way back behind the setting's own line — is
     * held at the first choice, which is what the client's own {@code Math.max} does.
     */
    public static Rect selection(int panelLeft, int rowTop, int width, int rowHeight, double slide) {
        int top = rowTop + rowHeight + Math.max(0, (int) Math.round(slide));

        return new Rect(panelLeft + BAR_INSET, top, panelLeft + width - EDGE_INSET, top + rowHeight);
    }

    /** The outline around the whole open list: the setting's line, then one line per choice. */
    public static Rect outline(int panelLeft, int rowTop, int width, int rowHeight, int choices) {
        return new Rect(panelLeft + EDGE_INSET, rowTop, panelLeft + width - EDGE_INSET,
            rowTop + rowHeight * (choices + 1));
    }

    /** One frame of the bar sliding towards a choice, at the client's eased speed of two. */
    public static double slide(double current, double target, int fps) {
        return ZioAnim.approach(current, target, 2.0, fps);
    }

    /** A rectangle in screen pixels. */
    public record Rect(int left, int top, int right, int bottom) {
    }
}
