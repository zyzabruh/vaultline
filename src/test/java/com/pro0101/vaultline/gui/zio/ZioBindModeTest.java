package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The two modes of a bind and the order a right click walks them in. */
class ZioBindModeTest {
    @Test
    void theModesCarryTheCopiedClientsNames() {
        assertEquals("Toggle", ZioBindMode.TOGGLE.displayName());
        assertEquals("Hold", ZioBindMode.HOLD.displayName());
    }

    @Test
    void aRightClickStepsTheModeAndComesBackAround() {
        ZioBindMode mode = ZioBindMode.TOGGLE;

        mode = mode.next();
        assertEquals(ZioBindMode.HOLD, mode);

        mode = mode.next();
        assertEquals(ZioBindMode.TOGGLE, mode, "two steps come back to the default");
    }

    @Test
    void aStoredNameReadsBackAndAnythingElseIsTheDefault() {
        assertEquals(ZioBindMode.HOLD, ZioBindMode.parse("HOLD"));
        assertEquals(ZioBindMode.HOLD, ZioBindMode.parse("hold"));
        assertEquals(ZioBindMode.TOGGLE, ZioBindMode.parse("TOGGLE"));
        assertEquals(ZioBindMode.TOGGLE, ZioBindMode.parse("neither"));
        assertEquals(ZioBindMode.TOGGLE, ZioBindMode.parse(null));
    }
}
