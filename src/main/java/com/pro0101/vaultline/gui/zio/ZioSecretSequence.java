package com.pro0101.vaultline.gui.zio;

/**
 * The copied client's secret sequence, without the screen.
 *
 * <p>{@code ClickGuiScreen} keeps a index into {@code SECRET_SEQUENCE} and advances it on every key
 * press while the Game panel is still locked: a key that matches the next one moves the index on,
 * anything else puts it back to the start. When the index reaches the end, the panel is unlocked for
 * good. The ten key codes are here so the rule can be checked without a window; the screen only
 * feeds it the keys it receives.
 *
 * <p>The codes are the client's own, which are GLFW's: the four arrows, then {@code B} and {@code A}.
 * They are written as numbers so this class needs nothing from the game.
 */
public final class ZioSecretSequence {
    public static final int UP = 265;
    public static final int DOWN = 264;
    public static final int LEFT = 263;
    public static final int RIGHT = 262;
    public static final int B = 66;
    public static final int A = 65;

    /** {@code SECRET_SEQUENCE}: up, up, down, down, left, right, left, right, B, A. */
    public static final int[] KEYS = {UP, UP, DOWN, DOWN, LEFT, RIGHT, LEFT, RIGHT, B, A};

    private ZioSecretSequence() {
    }

    /** How many keys the sequence takes. */
    public static int length() {
        return KEYS.length;
    }

    /**
     * The index after a key press: one further along when the key is the next one, back to the start
     * otherwise — including when the index was already at the end, as the client's own check does.
     */
    public static int accept(int progress, int key) {
        if (progress < 0 || progress >= KEYS.length) return 0;

        return KEYS[progress] == key ? progress + 1 : 0;
    }

    /** Whether the sequence has been typed in full. */
    public static boolean complete(int progress) {
        return progress >= KEYS.length;
    }
}
