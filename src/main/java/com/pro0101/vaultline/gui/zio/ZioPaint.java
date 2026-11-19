package com.pro0101.vaultline.gui.zio;

import java.util.Locale;

/**
 * The shapes the copied client paints on its screen, taken out of the screen itself.
 *
 * <p>Every routine is a pure function of its arguments and of the {@link ZioCanvas} it is handed, so
 * a test can record exactly what would be submitted to the renderer — positions, sizes, colours and
 * the scale of a line — without a client, a window or a GPU. The numbers are the ones in
 * {@code ClientScreen.render} and {@code ClickGuiScreen.renderContents}.
 */
public final class ZioPaint {
    /** The fifth of black the copied client lays over the screen while its search is open. */
    public static final double SEARCH_DIM = 0.2;

    /** Nine pixels to the right of the pointer, and one pixel of padding inside the panel. */
    public static final int TOOLTIP_OFFSET_X = 9;

    public static final int TOOLTIP_PADDING = 1;

    private ZioPaint() {
    }

    /** A rectangle a routine covered, in layout units, so a test can read it back. */
    public record Bounds(int left, int top, int right, int bottom) {
    }

    /** A colour with its alpha scaled, which is the one operation every fade on this screen is. */
    public static int fade(int argb, double factor) {
        int alpha = (int) Math.round(((argb >>> 24) & 0xFF) * Math.max(0, Math.min(1, factor)));

        return (alpha << 24) | (argb & 0xFFFFFF);
    }

    /**
     * The tooltip of the copied client: a dark panel one pixel above the pointer and nine to its
     * right, one pixel of padding, a line every line height plus one, and flipped to the left of the
     * pointer when it would run past the screen edge.
     */
    public static Bounds tooltip(ZioCanvas canvas, int screenWidth, float pointerX, float pointerY,
                                 String[] lines, int background, int textColour) {
        Bounds bounds = tooltipBounds(canvas, screenWidth, pointerX, pointerY, lines);

        tooltip(canvas, bounds, lines, background, textColour);

        return bounds;
    }

    /**
     * Where the tooltip goes, decided without drawing it.
     *
     * <p>The screen decides the rectangle once a frame, before the blur pass runs, and hands the same
     * rectangle to the blur and to the drawing: the blurred patch and the panel over it cover the same
     * pixels of the same frame, including the frame the pointer moved and the one where the tooltip
     * flips to the other side of it.
     */
    public static Bounds tooltipBounds(ZioCanvas canvas, int screenWidth, float pointerX, float pointerY,
                                       String[] lines) {
        int lineHeight = canvas.lineHeight() + 1;
        int widest = 0;

        for (String line : lines) widest = Math.max(widest, Math.round(canvas.textWidth(line)));

        int left = (int) (pointerX + TOOLTIP_OFFSET_X);
        int right = (int) (pointerX + widest + TOOLTIP_OFFSET_X + TOOLTIP_PADDING);

        if (right > screenWidth) {
            left = (int) (pointerX - widest - 2);
            right = (int) (pointerX - 1);
        }

        int top = (int) pointerY - 1;
        int bottom = (int) (pointerY - 1 + lineHeight * lines.length);

        return new Bounds(left, top, right, bottom);
    }

    /** The tooltip's panel and lines, drawn inside a rectangle already decided by {@link #tooltipBounds}. */
    public static void tooltip(ZioCanvas canvas, Bounds bounds, String[] lines, int background, int textColour) {
        int lineHeight = canvas.lineHeight() + 1;

        canvas.fill(bounds.left(), bounds.top(), bounds.right(), bounds.bottom(), background);

        for (int index = 0; index < lines.length; index++) {
            canvas.text(lines[index], bounds.left() + TOOLTIP_PADDING,
                bounds.top() + TOOLTIP_PADDING + lineHeight * index, textColour);
        }
    }

    /** The dimming the copied client lays over the whole screen while its search has the keyboard. */
    public static void searchDimmer(ZioCanvas canvas, int width, int height, double focus, double screenAlpha) {
        if (focus <= 0.01) return;

        // Opaque black, then faded: a colour that starts at zero alpha would fade to nothing at all.
        canvas.fill(0, 0, width, height, fade(0xFF000000, SEARCH_DIM * focus * screenAlpha));
    }

    /**
     * The search itself: no field and no box, the query centred on the screen, its completion in grey
     * and a caret after it while it blinks.
     */
    public static void searchQuery(ZioCanvas canvas, int width, int height, String query, String completion,
                                   boolean caret, int textColour, int completionColour) {
        float textX = width / 2f - canvas.textWidth(query + completion + "_") / 2f;
        float textY = height / 2f - canvas.lineHeight() / 2f;
        float completionX = textX + canvas.textWidth(query);

        canvas.text(query, textX, textY, textColour);

        if (!completion.isEmpty()) canvas.text(completion, completionX, textY, completionColour);

        if (caret) canvas.text("_", completionX + canvas.textWidth(completion), textY, textColour);
    }

    /** The hint in the corner: one line up from the bottom edge, clear of the HUD's own margin. */
    public static void searchHint(ZioCanvas canvas, int width, int height, String hint, int margin, int colour) {
        canvas.text(hint, width - canvas.textWidth(hint) - margin, height - canvas.lineHeight() - margin, colour);
    }

    /** One frame of the flash the search gives the row it opens: red, dimming with the blink. */
    public static void highlight(ZioCanvas canvas, int left, int top, int right, int bottom, double flash,
                                 double screenAlpha) {
        canvas.fill(left, top, right, bottom,
            fade(((int) Math.round(flash * 255) << 24) | 0xFF0000, screenAlpha));
    }

    /**
     * The bind line the copied client keeps at the foot of an open module: {@code Key B} in the
     * default mode, the mode's own name as soon as it is anything else or the line is hovered, and an
     * ellipsis while it waits for a key.
     */
    public static String bindRowLabel(ZioBindMode mode, String key, boolean listening, boolean hovered) {
        String modeLabel = hovered || mode != ZioBindMode.TOGGLE ? mode.displayName() : "Key";

        return modeLabel + " " + (listening ? "..." : key);
    }

    /**
     * The mode and the key of a bind, in brackets and at half size, which is how the copied client's
     * keybind line writes them after a setting's name.
     */
    public static String bindStatusLabel(ZioBindMode mode, String key, boolean listening) {
        String modeLabel = mode.displayName().toUpperCase(Locale.ROOT);

        return "[" + modeLabel + " " + (listening ? "..." : key) + "]";
    }

    /** That status, drawn where the setting's name ends. */
    public static void bindStatus(ZioCanvas canvas, float x, float y, ZioBindMode mode, String key,
                                  boolean listening, int colour) {
        canvas.textScaled(bindStatusLabel(mode, key, listening), x, y, colour, STATUS_SCALE);
    }

    /** A module's bind after its name, in the copied client's brackets and at half size. */
    public static String statusLabel(String key) {
        return "[" + key.toUpperCase(Locale.ROOT) + "]";
    }

    /** That status, drawn where the module's name ends. */
    public static void status(ZioCanvas canvas, float x, float y, String key, int colour) {
        canvas.textScaled(statusLabel(key), x, y, colour, STATUS_SCALE);
    }

    /** A module's status after its name, and a bind's, are drawn at half size. */
    public static final float STATUS_SCALE = 0.5f;

    /** The game panel's name button: its label three pixels in from the panel's edge. */
    public static void gameButton(ZioCanvas canvas, int x, float textY, String label, int colour) {
        canvas.text(label, x + 3, textY, colour);
    }

    /**
     * The accent bar behind an enumeration: two pixels in from the panel's edge, one short of the
     * right edge, across the whole line — {@code EnumSettingComponent.render}'s fill.
     */
    public static void enumBar(ZioCanvas canvas, int panelLeft, int y, int rowHeight, int width, double fill,
                               int colour) {
        int left = panelLeft + 2;
        int right = panelLeft + (int) Math.round(Math.max(width * fill - 1, 2));

        canvas.fill(left, y, right, y + rowHeight - 1, colour);
    }

    /**
     * How fast the tooltip chases the pointer: {@code new InterpolatedFloat(2.5F, true)} in
     * {@code ClientScreen}. It is the only animation on that screen that runs faster than two.
     */
    public static final float TOOLTIP_FOLLOW_SPEED = 2.5f;

    /** One frame of the tooltip's point chasing the pointer. */
    public static double follow(double current, double target, int fps) {
        return ZioAnim.approach(current, target, TOOLTIP_FOLLOW_SPEED, fps);
    }

    /**
     * The button gradient every row of the client carries: its button colour at a tenth of its alpha
     * along the top edge, to nothing at the bottom ({@code ButtonComponent.renderButtonBackground}).
     */
    public static void buttonRow(ZioCanvas canvas, int left, int y, int width, int rowHeight, int colour,
                                 double alpha) {
        canvas.fillGradient(left + 1, y, left + width - 1, y + rowHeight - 1,
            fade(colour, 0.1 * alpha), fade(colour, 0));
    }

    /** The bind line at the foot of an open module, four pixels in from the panel's edge. */
    public static void bindRow(ZioCanvas canvas, int panelLeft, float textY, ZioBindMode mode, String key,
                               boolean listening, boolean hovered, int colour) {
        canvas.text(bindRowLabel(mode, key, listening, hovered), panelLeft + 4, textY, colour);
    }

    /**
     * Which description a tooltip shows: the setting's own when it has one, otherwise its module's.
     *
     * <p>The copied client asks its translation table per setting and falls back to the module; here
     * both texts already live on the setting and the module, so this is the same decision without a
     * table.
     */
    public static String descriptionToShow(String settingDescription, String moduleDescription) {
        if (settingDescription != null && !settingDescription.isEmpty()) return settingDescription;
        if (moduleDescription != null && !moduleDescription.isEmpty()) return moduleDescription;

        return null;
    }

    /**
     * The winter flakes the copied client falls over its screen: each one where its own drift puts it,
     * half the screen deep, ten pixels up from the top edge, fading as it falls.
     */
    public static void snow(ZioCanvas canvas, int width, int height, java.util.List<ZioSnow.Flake> flakes,
                            double screenAlpha) {
        for (ZioSnow.Flake flake : flakes) {
            float y = (float) (flake.y() * height * 0.5 - 10);

            if (y < -9) continue;

            int radius = flakeRadius(flake.scale());

            snowflake(canvas, (int) Math.round(flake.x() * width), (int) Math.round(y + radius), radius,
                fade(0xFFFFFF | flake.alpha() << 24, screenAlpha));
        }
    }

    /** How far a flake's arms reach: the client scales a nine-pixel glyph from one half to 1.15. */
    static int flakeRadius(float scale) {
        return Math.max(2, Math.round(4 * scale));
    }

    /**
     * A snowflake, drawn rather than typed: the client prints the font's {@code ❆}, which at nine
     * pixels is six one-pixel spokes, and the canvas has no lines, so each spoke is stamped.
     */
    public static void snowflake(ZioCanvas canvas, int centerX, int centerY, int radius, int argb) {
        canvas.fill(centerX, centerY, centerX + 1, centerY + 1, argb);

        // Three spokes are laid out and each is stamped opposite itself, so rounding can never make
        // one arm longer than the one facing it.
        for (int spoke = 0; spoke < 3; spoke++) {
            double angle = Math.PI * spoke / 3;
            int lastX = 0;
            int lastY = 0;

            for (int step = 1; step <= radius; step++) {
                int dx = (int) Math.round(Math.cos(angle) * step);
                int dy = (int) Math.round(Math.sin(angle) * step);

                if (dx == lastX && dy == lastY) continue;

                canvas.fill(centerX + dx, centerY + dy, centerX + dx + 1, centerY + dy + 1, argb);
                canvas.fill(centerX - dx, centerY - dy, centerX - dx + 1, centerY - dy + 1, argb);

                lastX = dx;
                lastY = dy;
            }
        }
    }
}
