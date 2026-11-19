package com.pro0101.vaultline.gui.zio;

import net.minecraft.client.MinecraftClient;

/**
 * The motion the copied client animates with, copied out of it rather than chosen here.
 *
 * <p>Two kinds exist there and both are reproduced: {@code InterpolatedFloat}, a frame-rate independent
 * ease-out that approaches a target over roughly half a second, used for panel expansion, scrolling, the
 * enable sweep of a module and the height of an open group; and {@code FloatAnimation}, a straight
 * interpolation over a fixed number of milliseconds, used for the hover lift (65 ms) and the title bar
 * fade (200 ms).
 *
 * <p>Its frame factor is {@code 1 / (fps * 0.5)} clamped to one, its easing is
 * {@code 1 - (factor - 1)^4}, it snaps once the remaining distance is under a hundredth, and at fifteen
 * frames a second or fewer it gives up and snaps to the target.
 */
public final class ZioAnim {
    /** How long the hover lift takes, from {@code hoverAnimation.setTarget(..., 65L)}. */
    public static final long HOVER_MILLIS = 65L;

    /** How long the title bar takes to fade between half and full, {@code ..., 200L}. */
    public static final long FADE_MILLIS = 200L;

    /** How long the screen takes to appear, {@code TRANSITION_DURATION_MILLIS}. */
    public static final long OPEN_MILLIS = 150L;

    /** Below this frame rate the copied client stops animating and snaps. */
    private static final int SLOW_FRAME_RATE = 15;

    /**
     * The numeric slider's fill, {@code new InterpolatedFloat(6.0F, false)}: three times the panel
     * speed, and uneased, so the bar tracks the value instead of drifting behind it.
     */
    public static final float NUMERIC_FILL_SPEED = 6f;

    private ZioAnim() {
    }

    /** The game's current frame rate, or zero before it has measured one. */
    public static int fps() {
        MinecraftClient client = MinecraftClient.getInstance();

        return client == null ? 0 : client.getCurrentFps();
    }

    /** One frame's share of a second at half rate, clamped to one. */
    public static float frameFactor(int fps) {
        if (fps <= 0) return 1f;

        return Math.min(1f, 1f / (fps * 0.5f));
    }

    /** The ease-out applied to the interpolation factor: {@code 1 - (factor - 1)^4}. */
    public static float ease(float factor) {
        float shifted = factor - 1f;

        return 1f - shifted * shifted * shifted * shifted;
    }

    /** One frame of an eased approach, snapped once it is close enough. */
    public static double approach(double value, double target, double speed, int fps) {
        // Zero means the frame rate has not been measured yet; it is treated like a slow one so the
        // first frame of a screen cannot leave an animation stuck.
        if (fps <= SLOW_FRAME_RATE) return target;

        double factor = ease((float) (frameFactor(fps) * speed));
        double next = value + (target - value) * factor;

        return Math.abs(target - next) < 0.01 ? target : next;
    }

    /**
     * One frame of an uneased approach, which is what {@code new InterpolatedFloat(speed, false)}
     * runs: the same frame factor and the same snapping, without the ease-out curve. The numeric
     * slider's fill and the colour marker's border use it.
     */
    public static double linear(double value, double target, double speed, int fps) {
        if (fps <= SLOW_FRAME_RATE) return target;

        double next = value + (target - value) * (frameFactor(fps) * speed);

        return Math.abs(target - next) < 0.01 ? target : next;
    }

    /** A straight interpolation over a fixed duration, with the wall clock as its only input. */
    public static final class Timed {
        private float start;
        private float target;
        private long endMillis;
        private long durationMillis;

        public Timed(float value) {
            this.start = value;
            this.target = value;
        }

        public float target() {
            return target;
        }

        public void set(long nowMillis, float value) {
            start = value;
            target = value;
            endMillis = nowMillis;
            durationMillis = 0L;
        }

        public void setTarget(long nowMillis, boolean state, long durationMillis) {
            setTarget(nowMillis, state ? 1f : 0f, durationMillis);
        }

        public void setTarget(long nowMillis, float newTarget, long durationMillis) {
            if (target == newTarget) return;

            start = value(nowMillis);
            target = newTarget;
            this.endMillis = nowMillis + durationMillis;
            this.durationMillis = durationMillis;
        }

        public float value(long nowMillis) {
            if (nowMillis > endMillis) return target;

            float remaining = (endMillis - nowMillis) / (float) Math.max(durationMillis, 1L);
            float progress = 1f - Math.min(1f, Math.max(0f, remaining));

            return progress * (target - start) + start;
        }
    }
}
