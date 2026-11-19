package com.pro0101.vaultline.gui.zio;

/**
 * The numbers the panels are painted with, copied from the client's UI module and panel code.
 *
 * <p>Three of them are what makes the screen look like that client rather than like a flat list of
 * boxes:
 *
 * <ul>
 *   <li>the whole screen is laid over with a tint and then a vertical gradient, before any panel
 *       ({@code ClientScreen.renderBackgroundOverlay}: {@code fillRect(tint)} then
 *       {@code fillVerticalGradient(gradient with alpha 0, gradient)});</li>
 *   <li>each panel shows the 600×450 frosted sheet at its own size, never stretched, clipped to the
 *       panel and shifted by a third of the panel's place on screen
 *       ({@code translate(panelX); drawTexture(aero, -300 + panelX / 3, 0, 600, 450)}), which reads as
 *       one sheet behind all the panels;</li>
 *   <li>the sheet is drawn at half the screen's fade alpha ({@code getRenderColor()[3] / 2}).</li>
 * </ul>
 *
 * <p>The colours are the UI module's defaults: tint {@code #64200D30}, gradient {@code #0FC794F4},
 * background {@code #1EC794F4}, enabled {@code #5AC794F4}, colour {@code #82C794F4}, button
 * {@code #73C794F4}.
 */
public final class ZioPanelPaint {
    public static final int TINT = 0x64200D30;
    public static final int GRADIENT = 0x0FC794F4;

    public static final int AERO_WIDTH = 600;
    public static final int AERO_HEIGHT = 450;

    /** The sheet is shifted by this much, then by a third of the panel's x. */
    public static final int AERO_OFFSET = -300;

    /** The two header heights of the copied client: SLIM is 12, every other style 14. */
    public static final int SLIM_HEADER_HEIGHT = 12;
    public static final int HEADER_HEIGHT = 14;

    private ZioPanelPaint() {
    }

    public static int headerHeight(boolean slim) {
        return slim ? SLIM_HEADER_HEIGHT : HEADER_HEIGHT;
    }

    /**
     * The half pixel the copied client drops under the header when its one pixel line is off
     * ({@code headerVerticalOffset = lineSetting ? 0.0F : 0.5F}); the line is always drawn here, so
     * this is zero and kept only to name the rule.
     */
    public static float headerVerticalOffset(boolean line) {
        return line ? 0f : 0.5f;
    }

    /** Where the sheet's left edge lands for a panel at {@code panelX}. */
    public static int aeroLeft(int panelX) {
        return panelX + AERO_OFFSET + panelX / 3;
    }

    /**
     * The sheet's alpha: half of the screen fade, as {@code getRenderColor()[3] / 2} — the copied client
     * halves an integer alpha, so full fade gives 127 and not 128.
     */
    public static int aeroAlpha(double fade) {
        return (int) Math.round(Math.max(0, Math.min(1, fade)) * 255) / 2;
    }

    /**
     * How tall the one pixel outline is: the panel as it is animated open, plus the half pixel the
     * copied client adds, and never shorter than the header ({@code outlineHeight <= headerHeight + 2}
     * → {@code headerHeight}).
     */
    public static int outlineHeight(int height, double expansion, int headerHeight) {
        int outline = (int) (height * expansion) + 1;

        return outline <= headerHeight + 2 ? headerHeight : outline;
    }
}
