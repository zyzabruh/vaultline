package com.pro0101.vaultline.gui.zio;

import java.util.List;
import java.util.Locale;

/**
 * The arithmetic behind the copied screen's settings lines.
 *
 * <p>Every line under a module is a row of the same height, except two kinds that unfold: the colour
 * picker and a list of choices. Keeping the heights, the click resolution, the filter and the scroll
 * bound here means the screen only draws and dispatches, and this part can be checked without a game
 * client.
 */
public final class ZioListLayout {
    /** One line of text: four of padding above and below the nine pixel font. */
    public static final int ROW_HEIGHT = 13;

    /** An unfolded colour picker: padding, square, hue bar, alpha bar, padding. */
    public static final int COLOUR_PICKER_HEIGHT = 3 + 93 + 3 + 6 + 3 + 6 + 3;

    public static final int LIST_ROWS = 6;

    /** An unfolded list: padding, search field, padding, the entries, padding. */
    public static final int LIST_HEIGHT = 3 + 13 + 3 + LIST_ROWS * ROW_HEIGHT + 3;

    private ZioListLayout() {
    }

    /** The height of one settings line, which its picker or its list makes taller. */
    public static int rowHeight(boolean pickerOpen, boolean listOpen) {
        return ROW_HEIGHT + (pickerOpen ? COLOUR_PICKER_HEIGHT : 0) + (listOpen ? LIST_HEIGHT : 0);
    }

    /** The height of a block of lines, in order. */
    public static int totalHeight(int[] heights) {
        int total = 0;

        for (int height : heights) total += height;

        return total;
    }

    /**
     * Which line sits at {@code position} of a block of lines, or -1 past the last one.
     *
     * <p>Lines are not all the same height, so a click cannot be resolved by dividing by the row
     * height: the heights are walked in order until the position falls inside one.
     */
    public static int indexAt(double position, int[] heights) {
        double offset = 0;

        for (int i = 0; i < heights.length; i++) {
            if (position >= offset && position < offset + heights[i]) return i;
            offset += heights[i];
        }

        return -1;
    }

    /** How far a list of {@code entries} choices can be scrolled, in pixels. */
    public static double maxScroll(int entries) {
        int content = Math.max(0, entries) * ROW_HEIGHT;
        int visible = LIST_ROWS * ROW_HEIGHT;

        return Math.max(0, content - visible);
    }

    /** A scroll of {@code scroll} kept inside what the list can show. */
    public static double clampScroll(double scroll, int entries) {
        return Math.min(Math.max(0, scroll), maxScroll(entries));
    }

    /**
     * Whether a choice matches the text typed in a list's own search.
     *
     * <p>The label is what the player reads, the identifier is the registry name a block, an item or
     * an entity is addressed by ({@code namespace:path}), and either one may be typed.
     */
    public static boolean matches(String label, String identifier, String query) {
        String trimmed = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) return true;

        String readable = label == null ? "" : label.toLowerCase(Locale.ROOT);
        String name = identifier == null ? "" : identifier.toLowerCase(Locale.ROOT);

        return readable.contains(trimmed) || name.contains(trimmed);
    }

    /**
     * A copy of {@code current} with {@code value} added when absent, or removed when present.
     *
     * <p>This is the write a click on a checkbox performs: the setting is given a new list, the old
     * one is left alone, and the order of the entries already there is kept.
     */
    public static <T> List<T> toggled(List<T> current, T value) {
        java.util.ArrayList<T> next = new java.util.ArrayList<>(current);

        if (!next.remove(value)) next.add(value);

        return next;
    }

    /**
     * Where a click inside an unfolded list falls.
     *
     * <p>The line itself folds the list, the search field takes the keyboard, and the box below is
     * where the choices are toggled.
     */
    public enum ClickZone {
        NONE,
        HEADER,
        SEARCH,
        ENTRIES
    }

    private static final int LIST_PADDING = 3;
    private static final int LIST_SEARCH_HEIGHT = 13;

    /** Where the search field of an unfolded list starts, given the top of its line. */
    public static int listSearchTop(int rowTop) {
        return rowTop + ROW_HEIGHT + LIST_PADDING;
    }

    /** Where the choices of an unfolded list start, given the top of its line. */
    public static int listEntriesTop(int rowTop) {
        return listSearchTop(rowTop) + LIST_SEARCH_HEIGHT + LIST_PADDING;
    }

    /** The zone of an unfolded list that {@code mouseY} falls in, given the top of its line. */
    public static ClickZone listZone(double mouseY, int rowTop) {
        double searchTop = listSearchTop(rowTop);
        double entriesTop = listEntriesTop(rowTop);

        if (mouseY >= rowTop && mouseY < rowTop + ROW_HEIGHT) return ClickZone.HEADER;
        if (mouseY >= searchTop && mouseY < searchTop + LIST_SEARCH_HEIGHT) return ClickZone.SEARCH;
        if (mouseY >= entriesTop && mouseY < entriesTop + LIST_ROWS * ROW_HEIGHT) return ClickZone.ENTRIES;

        return ClickZone.NONE;
    }

    /** The index of the choice under {@code mouseY} in an unfolded list, or -1 past the last one. */
    public static int entryIndex(double mouseY, int rowTop, double scroll) {
        double position = mouseY - listEntriesTop(rowTop) + scroll;

        if (position < 0) return -1;

        return (int) (position / ROW_HEIGHT);
    }

    /**
     * A registry name as a player reads it: {@code minecraft:trapped_chest} becomes
     * {@code trapped chest}. Used where a registry entry has no name of its own.
     */
    public static String readableId(String identifier) {
        if (identifier == null || identifier.isEmpty()) return "";

        int colon = identifier.indexOf(':');
        String path = colon < 0 ? identifier : identifier.substring(colon + 1);

        return path.replace('_', ' ');
    }

    /** Whether a setting's current values hold this choice: the read side of a checkbox. */
    public static boolean selected(java.util.Collection<?> current, Object value) {
        return current != null && current.contains(value);
    }

    /** The choices a list shows for a query, keeping their order. */
    public static <T> List<T> filtered(List<T> entries, java.util.function.Function<T, String> label,
        java.util.function.Function<T, String> identifier, String query) {
        if (query == null || query.trim().isEmpty()) return entries;

        java.util.ArrayList<T> kept = new java.util.ArrayList<>();

        for (T entry : entries) {
            if (matches(label.apply(entry), identifier.apply(entry), query)) kept.add(entry);
        }

        return kept;
    }
}
