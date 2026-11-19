package com.pro0101.vaultline.gui.zio;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The scissor regions of one frame, each clipped to the one already open.
 *
 * <p>The game does not combine scissors: the newest rectangle simply replaces the previous one, so a
 * nested region that is larger than its parent lets its contents escape. This keeps the nesting the
 * copied client gets from {@code ScissorStack.pushIntersection}, and hands back the region that is
 * actually open so the screen can pass it to the fills, to the text batch and to the game alike.
 */
final class ZioScissor {
    private final Deque<int[]> stack = new ArrayDeque<>();

    /** Forgets every region, at the start of a frame. */
    void clear() {
        stack.clear();
    }

    /**
     * Opens a region {@code (x1, y1)-(x2, y2)}, clipped to the innermost one already open.
     *
     * @return the region that is now open, as {@code {x1, y1, x2, y2}}, always at least one pixel wide
     *     and high
     */
    int[] push(int x1, int y1, int x2, int y2) {
        int[] parent = stack.peek();

        if (parent != null) {
            x1 = Math.max(x1, parent[0]);
            y1 = Math.max(y1, parent[1]);
            x2 = Math.min(x2, parent[2]);
            y2 = Math.min(y2, parent[3]);
        }

        // A region clipped away completely still needs a rectangle the game accepts: it collapses onto
        // the parent's corner, which nothing can be drawn into anyway.
        if (x2 <= x1) {
            x1 = parent == null ? x1 : parent[0];
            x2 = x1 + 1;
        }

        if (y2 <= y1) {
            y1 = parent == null ? y1 : parent[1];
            y2 = y1 + 1;
        }

        int[] region = {x1, y1, x2, y2};
        stack.push(region);

        return region;
    }

    /**
     * Closes the innermost region.
     *
     * @return the region now open, or {@code null} when none is
     */
    int[] pop() {
        stack.poll();

        return stack.peek();
    }
}
