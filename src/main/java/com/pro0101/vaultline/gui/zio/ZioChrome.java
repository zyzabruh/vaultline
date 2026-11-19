package com.pro0101.vaultline.gui.zio;

/**
 * Where the chrome under the panels sits: the preset button and the size controls.
 *
 * <p>They share the strip the panels leave free at the bottom of the screen. Keeping the rectangles
 * here means they can be compared with each other and with the screen bounds without drawing
 * anything.
 */
public final class ZioChrome {
    /** The strip the panels leave free at the bottom, in layout units. */
    public static final int HEIGHT_MARGIN = 50;

    public static final int PRESET_BUTTON_WIDTH = 48;
    public static final int ZOOM_BUTTON = 13;
    public static final int ZOOM_LABEL_WIDTH = 32;
    public static final int GAME_BUTTON_WIDTH = 40;
    public static final int FX_BUTTON_WIDTH = 24;

    private static final int MARGIN = 8;
    private static final int GAP = 4;

    /** A rectangle in layout units, with the tests the controls need. */
    public record Rect(int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public boolean contains(double px, double py) {
            return px >= x && px <= right() && py >= y && py <= bottom();
        }

        public boolean overlaps(Rect other) {
            return x < other.right() && other.x < right() && y < other.bottom() && other.y < bottom();
        }
    }

    private ZioChrome() {
    }

    /** The top of the free strip: everything below it is chrome, not panels. */
    public static int stripTop(int viewHeight) {
        return viewHeight - ZioListLayout.ROW_HEIGHT - MARGIN;
    }

    public static Rect presetsButton(int viewWidth, int viewHeight) {
        return new Rect(MARGIN, stripTop(viewHeight), PRESET_BUTTON_WIDTH, ZioListLayout.ROW_HEIGHT);
    }

    public static Rect zoomMinus(int viewWidth, int viewHeight) {
        return new Rect(presetsButton(viewWidth, viewHeight).right() + GAP, stripTop(viewHeight),
            ZOOM_BUTTON, ZioListLayout.ROW_HEIGHT);
    }

    public static Rect zoomLabel(int viewWidth, int viewHeight) {
        return new Rect(zoomMinus(viewWidth, viewHeight).right() + 2, stripTop(viewHeight),
            ZOOM_LABEL_WIDTH, ZioListLayout.ROW_HEIGHT);
    }

    public static Rect zoomPlus(int viewWidth, int viewHeight) {
        return new Rect(zoomLabel(viewWidth, viewHeight).right(), stripTop(viewHeight),
            ZOOM_BUTTON, ZioListLayout.ROW_HEIGHT);
    }

    /** The button that folds out the game panel, right of the size controls. */
    public static Rect gameButton(int viewWidth, int viewHeight) {
        return new Rect(zoomPlus(viewWidth, viewHeight).right() + GAP, stripTop(viewHeight),
            GAME_BUTTON_WIDTH, ZioListLayout.ROW_HEIGHT);
    }

    /** The button that turns the panel blur and bloom on and off, right of the game button. */
    public static Rect fxButton(int viewWidth, int viewHeight) {
        return new Rect(gameButton(viewWidth, viewHeight).right() + GAP, stripTop(viewHeight),
            FX_BUTTON_WIDTH, ZioListLayout.ROW_HEIGHT);
    }

    public static boolean inside(Rect rect, int viewWidth, int viewHeight) {
        return rect.x() >= 0 && rect.y() >= 0 && rect.right() <= viewWidth && rect.bottom() <= viewHeight;
    }

    /** Whether a control is inside the free strip and therefore clear of the panels. */
    public static boolean inStrip(Rect rect, int viewHeight) {
        return rect.y() >= viewHeight - HEIGHT_MARGIN && rect.bottom() <= viewHeight;
    }
}
