package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pointer shapes and the rule that picks them. */
class ZioCursorRuleTest {
    @AfterEach
    void reset() {
        ZioCursor.reset();
    }

    @Test
    void aFieldTakesTheIBeam() {
        assertEquals(ZioCursor.INPUT, ZioCursorRule.cursorFor(ZioCursorRule.Zone.FIELD));
    }

    @Test
    void emptySpaceKeepsTheArrow() {
        assertEquals(ZioCursor.STANDARD, ZioCursorRule.cursorFor(ZioCursorRule.Zone.NONE));
        assertEquals(ZioCursor.STANDARD, ZioCursorRule.cursorFor(null));
    }

    @Test
    void everythingThePointerCanActOnTakesTheHand() {
        for (ZioCursorRule.Zone zone : ZioCursorRule.Zone.values()) {
            if (zone == ZioCursorRule.Zone.NONE || zone == ZioCursorRule.Zone.FIELD) continue;

            assertEquals(ZioCursor.POINTER, ZioCursorRule.cursorFor(zone), zone.name());
        }
    }

    @Test
    void theFrameStartsFromTheArrowAndKeepsWhatWasChosen() {
        ZioCursor.set(ZioCursor.INPUT);
        assertEquals(ZioCursor.INPUT, ZioCursor.chosen());

        ZioCursor.reset();
        assertEquals(ZioCursor.STANDARD, ZioCursor.chosen());

        ZioCursor.set(null);
        assertEquals(ZioCursor.STANDARD, ZioCursor.chosen());
    }

    @Test
    void eachShapeIsItsOwnGlfwCursor() {
        // The copied client's three shapes are the arrow, the hand and the I-beam.
        assertEquals(3, ZioCursor.values().length);
        assertNotEquals(ZioCursor.STANDARD.shape(), ZioCursor.POINTER.shape());
        assertNotEquals(ZioCursor.STANDARD.shape(), ZioCursor.INPUT.shape());
        assertNotEquals(ZioCursor.POINTER.shape(), ZioCursor.INPUT.shape());
        assertTrue(ZioCursor.STANDARD.shape() > 0);
    }
}
