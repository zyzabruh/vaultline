package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The copied client's rule for open modules: last opened last, and a fourth closes the first. */
class ZioOpenRowsTest {
    @Test
    void openingKeepsTheNewestLast() {
        List<String> open = new ArrayList<>();

        open = ZioOpenRows.opened(open, "a", 3);
        open = ZioOpenRows.opened(open, "b", 3);

        assertEquals(List.of("a", "b"), open);
    }

    @Test
    void openingOneThatIsAlreadyOpenMovesItToTheEnd() {
        List<String> open = List.of("a", "b");

        assertEquals(List.of("b", "a"), ZioOpenRows.opened(open, "a", 3));
    }

    @Test
    void aFourthOpenClosesTheFirst() {
        List<String> open = List.of("a", "b", "c");

        List<String> next = ZioOpenRows.opened(open, "d", 3);

        assertEquals(List.of("b", "c", "d"), next);
        assertFalse(next.contains("a"), "the oldest is the one that closes");
        assertTrue(next.contains("d"));
    }

    @Test
    void closingTakesOneOutAndLeavesTheOrderOfTheOthers() {
        assertEquals(List.of("a", "c"), ZioOpenRows.closed(List.of("a", "b", "c"), "b"));
        assertEquals(List.of("a", "b"), ZioOpenRows.closed(List.of("a", "b"), "missing"));
        assertEquals(List.of(), ZioOpenRows.closed(List.of("a"), "a"));
    }

    @Test
    void theListIsNotChangedInPlace() {
        List<String> open = new ArrayList<>(List.of("a", "b"));

        ZioOpenRows.opened(open, "c", 3);
        ZioOpenRows.closed(open, "a");

        assertEquals(List.of("a", "b"), open);
    }
}
