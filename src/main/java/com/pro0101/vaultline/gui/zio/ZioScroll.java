package com.pro0101.vaultline.gui.zio;

/**
 * Scrolling a list past its end, as the copied client does.
 *
 * <p>{@code Panel.recoverScrollOffset} lets the wheel run forty pixels beyond either end and then
 * drags the offset back, one pixel per frame after a wait that follows the frame rate: the divisor is
 * {@code fps / 200 * 20}, halved at forty frames a second or fewer, so a slow machine springs back
 * sooner. The offset itself is only ever kept inside the bounds plus that forty pixel allowance.
 */
public final class ZioScroll {
    /** {@code MAX_SCROLL_OVERSHOOT}: how far past either end the wheel may run. */
    public static final int MAX_OVERSHOOT = 40;

    /** {@code SCROLL_RECOVERY_FPS_BASE}. */
    private static final float RECOVERY_FPS_BASE = 200f;

    /** {@code SCROLL_RECOVERY_SPEED}. */
    private static final float RECOVERY_SPEED = 20f;

    /** {@code SLOW_FRAME_RATE}, below which the spring back runs at twice the speed. */
    private static final int SLOW_FRAME_RATE = 40;

    private ZioScroll() {
    }

    /** A target kept in bounds, plus the copied client's forty pixels of over-scroll at each end. */
    public static double target(double target, double limit) {
        return Math.max(-MAX_OVERSHOOT, Math.min(limit + MAX_OVERSHOOT, target));
    }

    /**
     * One frame of the spring back.
     *
     * <p>While the offset sits past an end it is dragged towards it by a step that grows with the frames
     * spent there; once it is inside the bounds the counter starts again, which is what makes a fresh
     * over-scroll wait before it moves.
     */
    public static Recovery recover(double offset, double limit, int frames, int fps) {
        double divisor = Math.max(1, fps) / RECOVERY_FPS_BASE * RECOVERY_SPEED;

        if (fps > 0 && fps <= SLOW_FRAME_RATE) divisor /= 2;

        if (offset < 0) {
            frames++;
            offset += (int) Math.min(frames / divisor, -offset);
        } else if (offset > limit) {
            frames++;
            offset -= (int) Math.min(frames / divisor, offset - limit);
        } else {
            frames = 0;
        }

        return new Recovery(target(offset, limit), frames);
    }

    /** Where the offset ended up, and how many frames it has spent waiting to move. */
    public record Recovery(double offset, int frames) {
    }
}
