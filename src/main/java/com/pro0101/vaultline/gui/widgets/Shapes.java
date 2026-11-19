package com.pro0101.vaultline.gui.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;

/**
 * Rounded rectangles for the GUI themes.
 *
 * <p>Meteor draws its interface with axis-aligned quads, which is why every panel in it has square
 * corners. A rounded panel is the same rectangle with the four corners replaced by quarter discs,
 * so it is built here from one quad per side, one for the middle, and a small triangle fan per
 * corner. The fills take a top and a bottom colour so a panel can fade like the panels in newer
 * clients without a texture or a shader.
 */
public final class Shapes {
    /** Segments per quarter disc. Six is smooth at the radii a GUI uses and stays cheap. */
    private static final int STEPS = 6;
    private static final double HALF_PI = Math.PI / 2;

    private Shapes() {
    }

    /**
     * Fills a rounded rectangle with a vertical gradient.
     *
     * @param renderer renderer to draw with
     * @param x        left edge
     * @param y        top edge
     * @param width    width
     * @param height   height
     * @param radius   corner radius, clamped to half the shortest side
     * @param top      colour at the top edge
     * @param bottom   colour at the bottom edge
     */
    public static void fill(GuiRenderer renderer, double x, double y, double width, double height,
                            double radius, Color top, Color bottom) {
        if (width <= 0 || height <= 0) return;

        double r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));

        if (r < 0.5) {
            renderer.quad(x, y, width, height, top, top, bottom, bottom);
            return;
        }

        double midTop = y + r;
        double midBottom = y + height - r;

        // Middle band, full height, between the two corner columns.
        renderer.quad(x + r, y, width - r * 2, height, top, top, bottom, bottom);

        // Side bands, shortened by the corner radius.
        Color leftTop = at(top, bottom, r / height);
        Color leftBottom = at(top, bottom, (height - r) / height);
        renderer.quad(x, midTop, r, height - r * 2, leftTop, leftTop, leftBottom, leftBottom);
        renderer.quad(x + width - r, midTop, r, height - r * 2, leftTop, leftTop, leftBottom, leftBottom);

        corner(renderer, x + r, midTop, r, Math.PI, top, bottom, y, height);
        corner(renderer, x + width - r, midTop, r, Math.PI + HALF_PI, top, bottom, y, height);
        corner(renderer, x + width - r, midBottom, r, 0, top, bottom, y, height);
        corner(renderer, x + r, midBottom, r, HALF_PI, top, bottom, y, height);
    }

    /**
     * Fills a rounded rectangle with one colour.
     *
     * @param renderer renderer to draw with
     * @param x        left edge
     * @param y        top edge
     * @param width    width
     * @param height   height
     * @param radius   corner radius
     * @param color    fill colour
     */
    public static void fill(GuiRenderer renderer, double x, double y, double width, double height,
                            double radius, Color color) {
        fill(renderer, x, y, width, height, radius, color, color);
    }

    /**
     * Draws a rounded border as a ring, by filling the outer shape and then the inner one.
     *
     * @param renderer  renderer to draw with
     * @param x         left edge
     * @param y         top edge
     * @param width     width
     * @param height    height
     * @param radius    corner radius
     * @param thickness border thickness in pixels
     * @param border    border colour
     * @param top       fill colour at the top edge, drawn over the inner shape
     * @param bottom    fill colour at the bottom edge
     */
    public static void bordered(GuiRenderer renderer, double x, double y, double width, double height,
                                double radius, double thickness, Color border, Color top, Color bottom) {
        fill(renderer, x, y, width, height, radius, border);
        fill(renderer, x + thickness, y + thickness, width - thickness * 2, height - thickness * 2,
            Math.max(0, radius - thickness), top, bottom);
    }

    /**
     * Draws a soft halo around a rounded rectangle, used as a panel glow.
     *
     * @param renderer renderer to draw with
     * @param x        left edge of the shape the halo surrounds
     * @param y        top edge
     * @param width    width
     * @param height   height
     * @param radius   corner radius
     * @param color    halo colour, its alpha is scaled down per layer
     * @param layers   number of layers, three is a soft edge
     */
    public static void glow(GuiRenderer renderer, double x, double y, double width, double height,
                            double radius, Color color, int layers) {
        if (layers <= 0 || color.a <= 0) return;

        for (int layer = layers; layer >= 1; layer--) {
            int alpha = color.a / (layer * 3);
            if (alpha <= 0) continue;

            fill(renderer, x - layer, y - layer, width + layer * 2.0, height + layer * 2.0,
                radius + layer, new Color(color.r, color.g, color.b, alpha));
        }
    }

    /** One quarter disc, drawn as a fan of triangles from its centre. */
    private static void corner(GuiRenderer renderer, double cx, double cy, double r, double start,
                               Color top, Color bottom, double y, double height) {
        for (int step = 0; step < STEPS; step++) {
            double a1 = start + (step + 0.5) * HALF_PI / STEPS;
            double a2 = start + (step + 1.5) * HALF_PI / STEPS;

            double x1 = cx + Math.cos(a1) * r;
            double y1 = cy + Math.sin(a1) * r;
            double x2 = cx + Math.cos(a2) * r;
            double y2 = cy + Math.sin(a2) * r;

            double fraction = Math.max(0, Math.min(1, (cy + (y1 + y2) / 2.0 - y) / height));
            renderer.triangle(cx, cy, x1, y1, x2, y2, at(top, bottom, fraction));
        }
    }

    private static Color at(Color top, Color bottom, double fraction) {
        double f = Math.max(0, Math.min(1, fraction));
        return new Color(
            (int) Math.round(top.r + (bottom.r - top.r) * f),
            (int) Math.round(top.g + (bottom.g - top.g) * f),
            (int) Math.round(top.b + (bottom.b - top.b) * f),
            (int) Math.round(top.a + (bottom.a - top.a) * f)
        );
    }
}
