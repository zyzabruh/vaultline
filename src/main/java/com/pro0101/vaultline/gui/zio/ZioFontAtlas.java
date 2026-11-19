package com.pro0101.vaultline.gui.zio;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * The glyph atlas the copied client builds for its GUI text.
 *
 * <p>The font is rasterised with AWT at twice the wanted size, laid out in cells eight pixels wider
 * than the glyph so neighbouring letters never bleed, and drawn back at a quarter of that size. Only
 * the printable ASCII range is covered, which is all the screen shows.
 *
 * <p>Nothing here touches Minecraft: the raster and its metrics are the part that can be checked
 * without a game client.
 */
public final class ZioFontAtlas {
    /** The atlas is drawn back at a quarter of the size it was rasterised at. */
    public static final float ATLAS_SCALE = 0.25f;

    public static final String DEFAULT_FAMILY = "Arial";
    public static final int DEFAULT_SIZE = 16;

    /** The glyph cell keeps this much empty room, four pixels on each side. */
    private static final int CELL_PADDING = 8;

    private static final char FIRST = 32;
    private static final char LAST = 126;
    private static final int ATLAS_WIDTH = 512;

    /** One glyph's place in the atlas, in atlas pixels. */
    public record Glyph(int x, int y, int width, int height) {}

    private final Map<Character, Glyph> glyphs = new HashMap<>();
    private final Map<Character, Float> advances = new HashMap<>();
    private final BufferedImage image;
    private final int imageHeight;
    private final int lineHeight;

    private ZioFontAtlas(BufferedImage image, int imageHeight, int lineHeight) {
        this.image = image;
        this.imageHeight = imageHeight;
        this.lineHeight = lineHeight;
    }

    /** Builds the atlas for a font family and size, the way the copied client does. */
    public static ZioFontAtlas build(String family, int size) {
        Font font = new Font(family == null || family.isBlank() ? DEFAULT_FAMILY : family, Font.PLAIN,
            Math.max(4, size) * 2);

        BufferedImage metricsImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D metricsGraphics = metricsImage.createGraphics();
        configure(metricsGraphics);
        metricsGraphics.setFont(font);

        FontMetrics metrics = metricsGraphics.getFontMetrics();

        // The atlas is laid out first: each cell is the glyph's own box, padded by four pixels a side.
        Map<Character, int[]> cells = new HashMap<>();
        Map<Character, Float> advanceValues = new HashMap<>();
        int cursorX = 0;
        int cursorY = 0;
        int rowHeight = 0;
        int tallestCell = 0;

        for (char character = FIRST; character <= LAST; character++) {
            java.awt.geom.Rectangle2D glyphBounds = metrics.getStringBounds(String.valueOf(character), metricsGraphics);
            Rectangle bounds = glyphBounds.getBounds();
            int width = align(bounds.width + CELL_PADDING, 2);
            int height = align(Math.max(1, bounds.height), 2);

            // The pen advance is the glyph's own fractional width, never a rounded one.
            advanceValues.put(character, (float) glyphBounds.getWidth());

            if (cursorX + width >= ATLAS_WIDTH) {
                cursorX = 0;
                cursorY += rowHeight + 2;
                rowHeight = 0;
            }

            cells.put(character, new int[]{cursorX, cursorY, width, height});
            rowHeight = Math.max(rowHeight, height);
            tallestCell = Math.max(tallestCell, height);
            cursorX += width;
        }

        int atlasHeight = align(Math.max(1, cursorY + rowHeight), 2);

        metricsGraphics.dispose();
        metricsImage.flush();

        // The glyphs are drawn in white, the colour comes from the vertices when they are used.
        BufferedImage atlas = new BufferedImage(ATLAS_WIDTH, atlasHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = atlas.createGraphics();
        configure(graphics);
        graphics.setFont(font);
        graphics.setColor(Color.WHITE);

        int ascent = metrics.getAscent();

        cells.forEach((character, cell) -> graphics.drawString(String.valueOf(character),
            cell[0] + CELL_PADDING / 2, cell[1] + ascent));

        graphics.dispose();

        // The copied client's line height: the tallest glyph cell scaled back, less one — its
        // FontAtlas keeps {@code fontHeight = max(cell height)} and FontRenderer subtracts one from the
        // drawn size ({@code getRenderer().getHeight() - 1}).
        int lineHeight = Math.max(1, (int) (tallestCell * ATLAS_SCALE) - 1);

        ZioFontAtlas result = new ZioFontAtlas(atlas, atlasHeight, lineHeight);

        for (char character = FIRST; character <= LAST; character++) {
            int[] cell = cells.get(character);

            result.glyphs.put(character, new Glyph(cell[0], cell[1], cell[2], cell[3]));
            result.advances.put(character, advanceValues.getOrDefault(character, (float) cell[2] - CELL_PADDING));
        }

        return result;
    }

    private static void configure(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    private static int align(int value, int alignment) {
        return (value + alignment - 1) / alignment * alignment;
    }

    /** The height of one line of this text, in layout pixels. */
    public int lineHeight() {
        return lineHeight;
    }

    /** The glyph for a character, or the one for {@code ?} when it is not covered. */
    public Glyph glyph(char character) {
        Glyph glyph = glyphs.get(character);

        return glyph != null ? glyph : glyphs.get('?');
    }

    /**
     * How far the pen moves after this character, in atlas pixels, kept fractional.
     *
     * <p>The copied client does the same: {@code drawGlyph} returns {@code glyph.width - 8.0F} and the
     * pen is a float, so a word's width is the sum of its glyphs' own widths rather than a sum of
     * rounded ones. Rounding each advance first shifts every following letter and makes centred lines
     * sit a pixel off.
     */
    public float advanceF(char character) {
        Float advance = advances.get(character);

        return advance != null ? advance : advances.getOrDefault('?', 0f);
    }

    /** The advance rounded to whole atlas pixels, for the places that compare glyph sizes. */
    public int advance(char character) {
        return Math.round(advanceF(character));
    }

    /**
     * The width a line of text takes, the way the copied client measures it: every glyph's advance
     * summed, formatting codes skipped along with the character they dress, and one pixel added after
     * the line is scaled back to drawn size.
     */
    public int width(String text) {
        return (int) widthF(text);
    }

    /** The same measurement before it is rounded, which is what centring and wrapping need. */
    public float widthF(String text) {
        if (text == null || text.isEmpty()) return 0f;

        float width = 0f;

        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);

            if (character == '\u00a7') {
                i++;
                continue;
            }

            width += advanceF(character);
        }

        return width * ATLAS_SCALE + 1;
    }

    /** The left edge that centres a line of text on {@code centreX}. */
    public int centeredX(String text, int centreX) {
        return centreX - width(text) / 2;
    }

    /** A colour that carries no alpha is drawn opaque, which is what the copied client does first. */
    public static int opaque(int argb) {
        return (argb >>> 24) == 0 ? argb | 0xFF000000 : argb;
    }

    /**
     * The colour of the shadow pass: alpha is kept and every other channel is quartered, which is the
     * copied client's own {@code alpha | ((argb & 0x00FCFCFC) >>> 2)}.
     */
    public static int shadow(int argb) {
        int alpha = argb & 0xFF000000;

        return alpha | ((argb & 0x00FCFCFC) >>> 2);
    }

    public BufferedImage image() {
        return image;
    }

    public int imageHeight() {
        return imageHeight;
    }
}
