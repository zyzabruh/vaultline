package com.pro0101.vaultline.gui.zio;

import java.util.ArrayList;
import java.util.List;

/**
 * Dragging a panel: what moves to the front while it is held, and where it is allowed to be.
 *
 * <p>Two rules, both taken from the copied client. {@code ClientScreen.mouseClicked} takes the panel
 * under the pointer out of the list and appends it, so the panel being dragged is the last one drawn,
 * and every other panel keeps the order it had. {@code Panel.updateDragPosition} keeps the whole
 * panel inside the screen, not just its title bar, on both axes.
 *
 * <p>Both are here, with no client types, so they can be checked without a screen.
 */
public final class ZioDrag {
    private ZioDrag() {
    }

    /**
     * The panels after one is grabbed: it goes to the end — the last one drawn is the one on top — and
     * the others keep their own order.
     *
     * <p>The list handed in is left untouched; the caller decides what to do with the result.
     */
    public static <T> List<T> raised(List<T> panels, T panel) {
        List<T> next = new ArrayList<>(panels);

        next.remove(panel);
        next.add(panel);

        return next;
    }

    /**
     * A left edge kept so that a thing {@code size} wide stays entirely inside {@code screen}.
     *
     * <p>A thing wider than the screen sits at zero: there is no position that shows all of it, and
     * the copied client's own clamp would have no range left to choose from. Clamping twice changes
     * nothing, since the result is already inside the range.
     */
    public static int clamp(int position, int screen, int size) {
        int maximum = Math.max(0, screen - size);

        return Math.max(0, Math.min(maximum, position));
    }

    /**
     * Where the top left corner of a panel lands when it is dropped with the pointer at
     * {@code pointer} and held {@code offset} in from that corner.
     */
    public static int corner(double pointer, double offset, int screen, int size) {
        return clamp((int) (pointer - offset), screen, size);
    }
}
