package com.pro0101.vaultline.gui.zio;

/**
 * The little a drawing routine needs from the game, as a seam.
 *
 * <p>The copied client's shapes are painted straight onto its pose stack. Here they are painted
 * through this interface instead, so {@link ZioPaint} can be handed a recorder in a test and the
 * calls it makes — positions, sizes, colours, the scale of a line — can be checked without a client,
 * a window or a GPU. In the screen, the only implementation writes into the frame's {@code
 * DrawContext}.
 */
public interface ZioCanvas {
    void fill(int x1, int y1, int x2, int y2, int argb);

    /** A rectangle from one colour at its top edge to another at its bottom edge. */
    void fillGradient(int x1, int y1, int x2, int y2, int topArgb, int bottomArgb);

    /** One line of text, at the copied client's own size. */
    void text(String value, float x, float y, int argb);

    /** One line of text at a fraction of its size, which is how the client draws a status. */
    void textScaled(String value, float x, float y, int argb, float scale);

    /** How wide a line of text is, in layout units, with the client's own metrics. */
    float textWidth(String value);

    /** The height of a line of text, in layout units. */
    int lineHeight();
}
