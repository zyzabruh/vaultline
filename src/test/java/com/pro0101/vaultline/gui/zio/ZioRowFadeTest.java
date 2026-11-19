package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A list row appearing and disappearing. */
class ZioRowFadeTest {
    @Test
    void aReturningRowFadesInOverAFewFrames() {
        double progress = 0;

        for (int frame = 0; frame < 200; frame++) {
            double next = ZioRowFade.next(progress, true, 200);

            assertTrue(next >= progress, "the fade went backwards");
            progress = next;
        }

        assertEquals(1, progress, 1e-9);
    }

    @Test
    void aHiddenRowFadesOutAndIsThenForgotten() {
        double progress = 1;

        for (int frame = 0; frame < 200; frame++) progress = ZioRowFade.next(progress, false, 200);

        assertEquals(0, progress, 1e-9);
        assertTrue(ZioRowFade.gone(progress));
    }

    @Test
    void theSearchHidesARowAtOnceInsteadOfFadingIt() {
        // The client resets the animation on a mismatch: no fade out, the row is simply not there.
        assertEquals(0, ZioRowFade.reset(false), 1e-9);
        assertEquals(1, ZioRowFade.reset(true), 1e-9);
    }

    @Test
    void aFreshRowStartsClearAndAFreshlyShownOneIsWhole() {
        assertEquals(0, ZioRowFade.alpha(0), 1e-9);
        assertEquals(1, ZioRowFade.alpha(1), 1e-9);
        assertEquals(1, ZioRowFade.alpha(4), 1e-9);
        assertEquals(0, ZioRowFade.alpha(-1), 1e-9);
    }

    @Test
    void aRowIsGoneOnlyOnceItIsNearlyClear() {
        assertFalse(ZioRowFade.gone(0.5));
        assertFalse(ZioRowFade.gone(0.02));
        assertTrue(ZioRowFade.gone(0.005));
    }
}
