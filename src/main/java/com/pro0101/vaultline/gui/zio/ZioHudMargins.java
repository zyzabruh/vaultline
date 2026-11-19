package com.pro0101.vaultline.gui.zio;

/**
 * The safe-zone margins of the copied client's HUD editor, ported from its {@code gui/screens/
 * HudMarginController}.
 *
 * <p>That editor lets the pointer drag the two black bands along the screen edges to set how far the
 * HUD keeps from them. The controller holds those margins, the band that is being shown, the one being
 * dragged and the two fade animations, and nothing else — the screen only forwards the pointer and
 * draws the bands.
 *
 * <p>Values copied from {@code HUDModule}: {@code new NumberSetting<>("SafeX", 1, 0, 20)} and
 * {@code new NumberSetting<>("SafeY", 1, 0, 10)}, and from {@code HudMarginController}: bands fade with
 * a target of {@code 1.4e+02F} over {@code 250L} ms, a margin counts as hit at {@code safe + 1} pixels,
 * and the vertical band only shows while the horizontal one is fully faded out.
 *
 * <p>The timing is the copied client's own {@code FloatAnimation}: linear from the value it had when the
 * target changed to the new target, over the given duration, measured in milliseconds.
 */
public final class ZioHudMargins {
    public static final int DEFAULT_SAFE_X = 1;
    public static final int DEFAULT_SAFE_Y = 1;
    public static final int MAX_SAFE_X = 20;
    public static final int MAX_SAFE_Y = 10;

    /** The alpha the bands fade in to, and how long that takes. */
    public static final float OVERLAY_ALPHA = 140f;
    public static final long OVERLAY_MILLIS = 250L;

    /** How far inside the band the pointer already counts as over it. */
    private static final int EDGE = 1;

    private int safeX = DEFAULT_SAFE_X;
    private int safeY = DEFAULT_SAFE_Y;

    private boolean draggingHorizontal;
    private boolean draggingVertical;
    private boolean fromRight;
    private boolean fromBottom;

    private final Slide horizontal = new Slide();
    private final Slide vertical = new Slide();

    public int safeX() {
        return safeX;
    }

    public int safeY() {
        return safeY;
    }

    public void setSafeX(int value) {
        safeX = Math.max(0, Math.min(MAX_SAFE_X, value));
    }

    public void setSafeY(int value) {
        safeY = Math.max(0, Math.min(MAX_SAFE_Y, value));
    }

    /** Whether the pointer is on one of the two side bands. */
    public boolean overHorizontalMargin(double mouseX, int screenWidth) {
        return mouseX < safeX + EDGE || mouseX > screenWidth - safeX - EDGE;
    }

    /** Whether the pointer is on one of the two top or bottom bands. */
    public boolean overVerticalMargin(double mouseY, int screenHeight) {
        return mouseY < safeY + EDGE || mouseY > screenHeight - safeY - EDGE;
    }

    public boolean draggingHorizontal() {
        return draggingHorizontal;
    }

    public boolean draggingVertical() {
        return draggingVertical;
    }

    /** Starts a drag on whichever band the left button was pressed on. */
    public void click(double mouseX, double mouseY, int button, int screenWidth, int screenHeight) {
        if (button != 0) return;

        draggingHorizontal = overHorizontalMargin(mouseX, screenWidth);

        if (!draggingHorizontal) draggingVertical = overVerticalMargin(mouseY, screenHeight);

        fromRight = mouseX > screenWidth / 2.0;
        fromBottom = mouseY > screenHeight / 2.0;
    }

    public void release(int button) {
        if (button != 0) return;

        draggingHorizontal = false;
        draggingVertical = false;
    }

    /**
     * One frame of the editor: works out which band shows, updates both fades and moves the margin a
     * drag is controlling. The margins themselves are what the HUD reads to stay clear of the edges.
     */
    public void update(long nowMillis, double mouseX, double mouseY, int screenWidth, int screenHeight) {
        boolean showHorizontal = overHorizontalMargin(mouseX, screenWidth) || draggingHorizontal;
        boolean showVertical = (overVerticalMargin(mouseY, screenHeight) || draggingVertical)
            && horizontal.value(nowMillis) == 0f;

        horizontal.setTarget(showHorizontal ? OVERLAY_ALPHA : 0f, OVERLAY_MILLIS, nowMillis);
        vertical.setTarget(showVertical ? OVERLAY_ALPHA : 0f, OVERLAY_MILLIS, nowMillis);

        if (draggingHorizontal) {
            setSafeX((int) Math.round(fromRight ? Math.abs(mouseX - screenWidth) : mouseX));
        } else if (draggingVertical) {
            setSafeY((int) Math.round(fromBottom ? Math.abs(mouseY - screenHeight) : mouseY));
        }
    }

    public float horizontalOverlay(long nowMillis) {
        return horizontal.value(nowMillis);
    }

    /** The alpha of the top and bottom bands this frame. */
    public float verticalOverlay(long nowMillis) {
        return vertical.value(nowMillis);
    }

    /** One value that slides to its target over a fixed time, the copied client's {@code FloatAnimation}. */
    private static final class Slide {
        private float start;
        private float target;
        private long endMillis;
        private long durationMillis;

        private float value(long nowMillis) {
            if (nowMillis > endMillis) return target;

            float remaining = (float) (endMillis - nowMillis);
            float remainingFraction = remaining / (float) Math.max(durationMillis, 1L);
            float progress = 1f - Math.max(0f, Math.min(1f, remainingFraction));

            return progress * (target - start) + start;
        }

        private void setTarget(float newTarget, long newDurationMillis, long nowMillis) {
            if (target == newTarget) return;

            start = value(nowMillis);
            target = newTarget;
            endMillis = nowMillis + newDurationMillis;
            durationMillis = newDurationMillis;
        }
    }
}
