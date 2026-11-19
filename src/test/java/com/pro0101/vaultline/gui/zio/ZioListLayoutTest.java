package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The arithmetic the settings lines of the copied screen rely on. */
class ZioListLayoutTest {
    @Test
    void foldedLinesAreAllTheSameHeight() {
        assertEquals(13, ZioListLayout.rowHeight(false, false));
    }

    @Test
    void anUnfoldedListMakesItsLineTallerByItsOwnBox() {
        assertEquals(13 + ZioListLayout.LIST_HEIGHT, ZioListLayout.rowHeight(false, true));
        assertEquals(ZioListLayout.LIST_HEIGHT,
            ZioListLayout.rowHeight(false, true) - ZioListLayout.rowHeight(false, false));
    }

    @Test
    void anUnfoldedPickerAndAnUnfoldedListBothCount() {
        assertEquals(13 + ZioListLayout.COLOUR_PICKER_HEIGHT + ZioListLayout.LIST_HEIGHT,
            ZioListLayout.rowHeight(true, true));
    }

    @Test
    void aBlockOfLinesIsAsTallAsItsLines() {
        assertEquals(0, ZioListLayout.totalHeight(new int[0]));
        assertEquals(26, ZioListLayout.totalHeight(new int[]{13, 13}));
        assertEquals(126, ZioListLayout.totalHeight(new int[]{13, 100, 13}));
    }

    @Test
    void aClickIsResolvedAgainstTheHeightOfEachLine() {
        int folded = ZioListLayout.rowHeight(false, false);
        int open = ZioListLayout.rowHeight(false, true);
        int[] heights = {folded, open, folded};

        // First line: a plain row at the top of the block.
        assertEquals(0, ZioListLayout.indexAt(0, heights));
        assertEquals(0, ZioListLayout.indexAt(folded - 0.5, heights));

        // Second line: an unfolded list, so every pixel of its box belongs to it.
        assertEquals(1, ZioListLayout.indexAt(folded, heights));
        assertEquals(1, ZioListLayout.indexAt(folded + open / 2.0, heights));
        assertEquals(1, ZioListLayout.indexAt(folded + open - 0.5, heights));

        // Third line: the row underneath, which the list must not have pushed the click onto.
        assertEquals(2, ZioListLayout.indexAt(folded + open, heights));
        assertEquals(2, ZioListLayout.indexAt(folded + open + folded - 0.5, heights));

        // Past the block nothing is hit.
        assertEquals(-1, ZioListLayout.indexAt(folded + open + folded, heights));
    }

    @Test
    void aListInTheMiddleOfTheBlockDoesNotShiftTheLinesBelow() {
        int folded = ZioListLayout.rowHeight(false, false);
        int open = ZioListLayout.rowHeight(false, true);
        int[] heights = {folded, folded, open, folded, folded};

        int start = 2 * folded;

        assertEquals(2, ZioListLayout.indexAt(start + 1, heights));
        assertEquals(3, ZioListLayout.indexAt(start + open, heights));
        assertEquals(4, ZioListLayout.indexAt(start + open + folded, heights));
    }

    @Test
    void aListAtTheBottomOnlyCoversItsOwnBox() {
        int folded = ZioListLayout.rowHeight(false, false);
        int open = ZioListLayout.rowHeight(false, true);
        int[] heights = {folded, folded, open};

        assertEquals(2, ZioListLayout.indexAt(2 * folded, heights));
        assertEquals(2, ZioListLayout.indexAt(2 * folded + open - 1, heights));
        assertEquals(-1, ZioListLayout.indexAt(2 * folded + open, heights));
    }

    @Test
    void scrollingIsBoundedByWhatTheListCanShow() {
        int visible = ZioListLayout.LIST_ROWS * ZioListLayout.ROW_HEIGHT;

        assertEquals(0, ZioListLayout.maxScroll(0));
        assertEquals(0, ZioListLayout.maxScroll(ZioListLayout.LIST_ROWS));
        assertEquals(ZioListLayout.ROW_HEIGHT, ZioListLayout.maxScroll(ZioListLayout.LIST_ROWS + 1));
        assertEquals(visible, ZioListLayout.maxScroll(ZioListLayout.LIST_ROWS * 2));
    }

    @Test
    void aScrollStaysInsideTheList() {
        int entries = ZioListLayout.LIST_ROWS + 2;
        double max = ZioListLayout.maxScroll(entries);

        assertEquals(0, ZioListLayout.clampScroll(-40, entries));
        assertEquals(max, ZioListLayout.clampScroll(max + 400, entries));
        assertEquals(13, ZioListLayout.clampScroll(13, entries), 1e-9);
    }

    @Test
    void theFilterIgnoresCase() {
        assertTrue(ZioListLayout.matches("Stone", "minecraft:stone", "STONE"));
        assertTrue(ZioListLayout.matches("Stone", "minecraft:stone", "sto"));
        assertTrue(ZioListLayout.matches("Stone", "minecraft:stone", "  StOnE  "));
    }

    @Test
    void theFilterAcceptsRegistryNames() {
        assertTrue(ZioListLayout.matches("Stone Bricks", "minecraft:stone_bricks", "minecraft:stone"));
        assertTrue(ZioListLayout.matches("Stone Bricks", "minecraft:stone_bricks", "bricks"));
        assertFalse(ZioListLayout.matches("Stone Bricks", "minecraft:stone_bricks", "dirt"));
    }

    @Test
    void anEmptyFilterKeepsEverything() {
        assertTrue(ZioListLayout.matches("Stone", "minecraft:stone", ""));
        assertTrue(ZioListLayout.matches("Stone", "minecraft:stone", "   "));
        assertTrue(ZioListLayout.matches("Stone", "minecraft:stone", null));

        List<String> entries = List.of("a", "b");

        assertEquals(entries, ZioListLayout.filtered(entries, value -> value, value -> value, ""));
    }

    @Test
    void aFilteredListKeepsItsOrder() {
        List<String> entries = List.of("Stone", "Stone Bricks", "Oak Log", "Stick");
        List<String> kept = ZioListLayout.filtered(entries, value -> value, value -> "minecraft:" + value, "sto");

        assertEquals(List.of("Stone", "Stone Bricks"), kept);
    }

    @Test
    void aRegistryNameIsShownAsReadableText() {
        assertEquals("stone", ZioListLayout.readableId("minecraft:stone"));
        assertEquals("trapped chest", ZioListLayout.readableId("minecraft:trapped_chest"));
        assertEquals("stone", ZioListLayout.readableId("stone"));
        assertEquals("", ZioListLayout.readableId(""));
        assertEquals("", ZioListLayout.readableId(null));
    }

    @Test
    void checkingAnEntryWritesItAndUncheckingTakesItOut() {
        List<String> current = List.of("a", "b");

        List<String> added = ZioListLayout.toggled(current, "c");
        assertEquals(List.of("a", "b", "c"), added);
        // The setting is given a new list; the one it held is untouched.
        assertEquals(List.of("a", "b"), current);

        assertEquals(List.of("a", "b"), ZioListLayout.toggled(List.of("a", "b", "c"), "c"));
        assertEquals(List.of("a"), ZioListLayout.toggled(List.of("b", "a"), "b"));
    }

    @Test
    void checkingTwiceEndsWhereItStarted() {
        List<String> once = ZioListLayout.toggled(List.of("a"), "b");
        List<String> twice = ZioListLayout.toggled(once, "b");

        assertEquals(List.of("a"), twice);
    }
}
