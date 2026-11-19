package com.pro0101.vaultline.gui.zio;

import java.util.ArrayList;
import java.util.List;

/**
 * The rule the copied client applies to its open modules: the last opened is remembered last, and a
 * fourth one closes the first.
 *
 * <p>{@code ClickGuiScreen.renderContents} keeps {@code openModuleComponents} in that order and drops
 * {@code getFirst()} once three are open. The bookkeeping is here, with no client types, so it can be
 * checked on its own; the screen only carries out the result.
 */
public final class ZioOpenRows {
    private ZioOpenRows() {
    }

    /** The open rows after {@code row} is opened: it moves to the end, and the oldest falls off. */
    public static <T> List<T> opened(List<T> open, T row, int max) {
        List<T> next = new ArrayList<>(open);

        next.remove(row);
        next.add(row);

        while (next.size() > max) next.remove(0);

        return next;
    }

    /** The open rows after {@code row} is folded, in the order the others were opened in. */
    public static <T> List<T> closed(List<T> open, T row) {
        List<T> next = new ArrayList<>(open);

        next.remove(row);

        return next;
    }
}
