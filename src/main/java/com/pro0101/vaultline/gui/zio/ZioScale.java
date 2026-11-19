package com.pro0101.vaultline.gui.zio;

/**
 * The zoom of the copied screen: how much of the game's GUI units one layout unit takes.
 *
 * <p>Kept apart so the limits and the label can be checked without a game client.
 */
public final class ZioScale {
    public static final float MIN = 0.5f;
    public static final float MAX = 1.25f;
    public static final float STEP = 0.25f;
    public static final float DEFAULT = 0.75f;

    private ZioScale() {
    }

    /** A scale kept inside the allowed range and snapped to whole steps. */
    public static float clamp(float value) {
        float stepped = Math.round(value / STEP) * STEP;

        return Math.min(MAX, Math.max(MIN, stepped));
    }

    /** The scale after a number of steps, positive to grow. */
    public static float stepped(float current, double steps) {
        return clamp((float) (current + steps * STEP));
    }

    /** The scale as the button shows it: {@code 0.75} reads as {@code 75%}. */
    public static String label(float value) {
        return Math.round(clamp(value) * 100) + "%";
    }

    /**
     * A scale read from the saved file: anything missing, unreadable or out of range falls back to
     * the default instead of being taken as is.
     */
    public static float parse(String text) {
        if (text == null) return DEFAULT;

        String trimmed = text.trim();
        if (trimmed.isEmpty()) return DEFAULT;

        try {
            return clamp(Float.parseFloat(trimmed));
        } catch (NumberFormatException ignored) {
            return DEFAULT;
        }
    }

    public static String format(float value) {
        return Float.toString(clamp(value));
    }
}
