package com.pro0101.vaultline.gui.zio;

/**
 * Where a HUD element goes while it is dragged or resized in the editor.
 *
 * <p>Geometry only, in the screen's own pixels, so every rule can be checked without a game client.
 *
 * <p>The rules are the copied client's: a dragged element follows the pointer with the offset it was
 * grabbed at, it can never leave the screen, and on release it snaps when it is within
 * {@code 5.0F} pixels of an edge or of the centre — {@code HudWidgetRenderer.updateSnapping(screenWidth,
 * screenHeight, 5.0F)}:<pre>
 * if (x &lt; snapDistance)                       align LEFT
 * else if (x + width &gt; screenWidth - snapDistance) align RIGHT
 * else if (|centreX - screenWidth / 2| &lt;= snapDistance) align CENTER</pre>
 * and the same three cases vertically.
 *
 * <p>One difference, taken from this addon rather than from that client: the snapped edge is the safe
 * margin the HUD keeps from the screen edge, because the elements here respect those margins. The snap
 * distance and the shape of the three cases are unchanged.
 */
public final class ZioHudLayout {
    public static final int SNAP_DISTANCE = 5;

    /** How close to a corner the resize handle reaches. */
    public static final int RESIZE_HANDLE = 8;

    /** Smallest an element can be resized to, so a handle can always be grabbed again. */
    public static final int MIN_WIDTH = 8;
    public static final int MIN_HEIGHT = 8;

    private ZioHudLayout() {
    }

    /** An element's place and size, in screen pixels. */
    public record Rect(int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public boolean contains(double px, double py) {
            return px >= x && px < right() && py >= y && py < bottom();
        }

        public Rect withSize(int width, int height) {
            return new Rect(x, y, width, height);
        }
    }

    /**
     * The box an element reports when a size was forced on it: the forced size, but never smaller than
     * what the element just measured for itself.
     *
     * <p>That floor is the constraint the editor needs: these elements draw their text from their own
     * top left corner, so a box smaller than their content would let the text spill outside its
     * background. The forced size can therefore grow an element freely and shrink it only down to its
     * own measured size.
     */
    public static int[] forcedBox(int measuredWidth, int measuredHeight, int[] forced) {
        if (forced == null) return new int[]{measuredWidth, measuredHeight};

        return new int[]{Math.max(forced[0], measuredWidth), Math.max(forced[1], measuredHeight)};
    }

    /** Where the element goes when the pointer moves to {@code mouse} while holding it at a point. */
    public static Rect drag(Rect start, double grabX, double grabY, double mouseX, double mouseY,
        int screenWidth, int screenHeight) {
        int x = (int) Math.round(mouseX - grabX);
        int y = (int) Math.round(mouseY - grabY);

        return clamp(new Rect(x, y, start.width(), start.height()), screenWidth, screenHeight);
    }

    public static Rect clamp(Rect rect, int screenWidth, int screenHeight) {
        int x = Math.max(0, Math.min(rect.x(), screenWidth - rect.width()));
        int y = Math.max(0, Math.min(rect.y(), screenHeight - rect.height()));

        return new Rect(x, y, rect.width(), rect.height());
    }

    /** The element resized by its bottom right corner, never below the minimum size. */
    public static Rect resize(Rect start, double mouseX, double mouseY) {
        int width = Math.max(MIN_WIDTH, (int) Math.round(mouseX) - start.x());
        int height = Math.max(MIN_HEIGHT, (int) Math.round(mouseY) - start.y());

        return new Rect(start.x(), start.y(), width, height);
    }

    /**
     * The element resized by its bottom right corner and kept on screen: a handle dragged past an edge
     * stops at it instead of growing a box that could never be grabbed again.
     */
    public static Rect resize(Rect start, double mouseX, double mouseY, int screenWidth, int screenHeight) {
        Rect resized = resize(start, mouseX, mouseY);

        int width = Math.min(resized.width(), Math.max(MIN_WIDTH, screenWidth - start.x()));
        int height = Math.min(resized.height(), Math.max(MIN_HEIGHT, screenHeight - start.y()));

        return resized.withSize(width, height);
    }

    /** Whether the pointer is on an element's resize corner. */
    public static boolean overResizeHandle(Rect rect, double mouseX, double mouseY) {
        return mouseX >= rect.right() - RESIZE_HANDLE && mouseX <= rect.right()
            && mouseY >= rect.bottom() - RESIZE_HANDLE && mouseY <= rect.bottom();
    }

    /**
     * The element snapped to the nearest edge or to the centre when it is close enough, the way the
     * copied client does on release. The margins are where a snapped edge lands.
     */
    public static Rect snap(Rect rect, int screenWidth, int screenHeight, int marginX, int marginY) {
        int x = rect.x();
        int width = rect.width();
        int centreX = x + width / 2;

        if (x < marginX + SNAP_DISTANCE) {
            x = marginX;
        } else if (x + width > screenWidth - marginX - SNAP_DISTANCE) {
            x = screenWidth - marginX - width;
        } else if (Math.abs(centreX - screenWidth / 2) <= SNAP_DISTANCE) {
            x = screenWidth / 2 - width / 2;
        }

        int y = rect.y();
        int height = rect.height();
        int centreY = y + height / 2;

        if (y < marginY + SNAP_DISTANCE) {
            y = marginY;
        } else if (y + height > screenHeight - marginY - SNAP_DISTANCE) {
            y = screenHeight - marginY - height;
        } else if (Math.abs(centreY - screenHeight / 2) <= SNAP_DISTANCE) {
            y = screenHeight / 2 - height / 2;
        }

        return clamp(new Rect(x, y, width, height), screenWidth, screenHeight);
    }

    /** The hover label's own easing speed: {@code new ClampedInterpolatedFloat(2.0F, true)}. */
    public static final double HOVER_SPEED = 2.0;

    /** How far a hover label slides in from the pointer, ten pixels while it is still invisible. */
    public static int labelOffset(double opacity, int direction) {
        double visible = Math.min(1, Math.max(0, opacity));

        return (int) Math.round(10 * (1 - visible) * direction);
    }

    public static int labelAlpha(double opacity) {
        return (int) Math.round(255 * Math.min(1, Math.max(0, opacity)));
    }

    /**
     * The editor's centre guides: one vertical line through the middle and two horizontal halves that
     * meet there, which is what {@code HudEditorScreen.runGuides} fills at forty percent of the fade.
     */
    public static Rect[] centreGuides(int screenWidth, int screenHeight) {
        int centreX = screenWidth / 2;
        int centreY = screenHeight / 2;

        return new Rect[] {
            new Rect(centreX, 0, 1, screenHeight),
            new Rect(0, centreY, centreX, 1),
            new Rect(centreX + 1, centreY, Math.max(0, screenWidth - centreX - 1), 1)
        };
    }
}
