package com.pro0101.vaultline.gui.zio;

/**
 * A list row fading in and out as a filter brings it back or takes it away.
 *
 * <p>{@code RegistryEntryComponent.visibilityAnimation} eases over the client's animation speed of two,
 * but a row the search hides is <em>set</em> to zero rather than eased there
 * ({@code visibilityAnimation.setValue(false)} on a mismatch), so typing in the list's own search makes
 * rows vanish at once and fade back in when the query is cleared.
 */
public final class ZioRowFade {
    /** The client's own speed for this animation, {@code animSpeed * 2}. */
    public static final double SPEED = 2.0;

    /** Below this the row is gone and its state may be dropped. */
    public static final double GONE = 0.01;

    private ZioRowFade() {
    }

    public static double next(double current, boolean visible, int fps) {
        return ZioAnim.approach(current, visible ? 1 : 0, SPEED, fps);
    }

    /** A row the search hides is set, not eased: the client resets it there. */
    public static double reset(boolean visible) {
        return visible ? 1 : 0;
    }

    /** What is left of a colour at this point of the fade. */
    public static double alpha(double progress) {
        return Math.min(1, Math.max(0, progress));
    }

    /** Whether the row has faded out and its state can be forgotten. */
    public static boolean gone(double progress) {
        return progress < GONE;
    }
}
