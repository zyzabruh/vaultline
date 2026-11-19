package com.pro0101.vaultline.gui.zio;

/**
 * The colours the copied client paints its games with.
 *
 * <p>Its Snake and Tetris draw flat cells — {@code Color.red} for the apple, {@code Color.GREEN} for
 * the snake's head, {@code new Color(0, 150, 0)} for its body and {@code Color.GRAY}/{@code
 * Color.GRAY.brighter()} once it is over, and the seven shape colours of {@code TetrisShape}. There is
 * no sprite sheet for either game in its resources, so none is invented here: the same colours are
 * kept, as ARGB.
 */
public final class ZioGameColors {
    public static final int APPLE = 0xFFFF0000;
    public static final int SNAKE_HEAD = 0xFF00FF00;
    public static final int SNAKE_BODY = 0xFF009600;
    public static final int SNAKE_HEAD_OVER = 0xFFBFBFBF;
    public static final int SNAKE_BODY_OVER = 0xFF808080;

    /** The see-through copy of a shape ({@code ColorUtils.withAlpha(colour, 50)}). */
    public static final int GHOST_ALPHA = 50;

    private ZioGameColors() {
    }

    /** A colour with its alpha replaced, the way the copied client tints its ghost piece. */
    public static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0xFFFFFF);
    }
}
