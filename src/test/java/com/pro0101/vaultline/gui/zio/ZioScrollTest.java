package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Scrolling past the end, and the spring back the copied client plays. */
class ZioScrollTest {
    @Test
    void aWheelPastEitherEndRunsFortyPixelsAndNoFurther() {
        assertEquals(-40, ZioScroll.target(-400, 500), 1e-9);
        assertEquals(540, ZioScroll.target(4000, 500), 1e-9);
        assertEquals(25, ZioScroll.target(25, 500), 1e-9);
    }

    @Test
    void theSpringBackStartsOnlyAfterItsWait() {
        // At two hundred frames a second the divisor is twenty, so the first frames do not move it.
        assertEquals(-40, ZioScroll.recover(-40, 500, 0, 200).offset(), 1e-9);
        assertEquals(-40, ZioScroll.recover(-40, 500, 18, 200).offset(), 1e-9);
        assertEquals(-39, ZioScroll.recover(-40, 500, 20, 200).offset(), 1e-9);
    }

    @Test
    void theSpringBackComesBackToTheEndAndStartsItsWaitAgain() {
        double offset = -40;
        int frames = 0;

        for (int frame = 0; frame < 400; frame++) {
            ZioScroll.Recovery recovery = ZioScroll.recover(offset, 500, frames, 200);

            offset = recovery.offset();
            frames = recovery.frames();
        }

        assertEquals(0, offset, 1e-9, "the over-scroll never came back");
        assertEquals(0, frames, "the counter did not start again once it was inside");
    }

    @Test
    void aSlowFrameRateSpringsBackSooner() {
        double slow = ZioScroll.recover(-40, 500, 5, 30).offset();
        double fast = ZioScroll.recover(-40, 500, 5, 200).offset();

        assertTrue(slow > fast, "thirty frames a second should move sooner than two hundred");
    }

    @Test
    void aSlowSpringBackDoesNotJumpPastTheEnd() {
        for (int frame = 0; frame < 400; frame++) {
            double offset = ZioScroll.recover(-1, 500, 200, 200).offset();

            assertEquals(0, offset, 1e-9, "the step moved past the end");
        }
    }

    @Test
    void insideTheBoundsTheCounterStartsAgain() {
        ZioScroll.Recovery recovery = ZioScroll.recover(120, 500, 12, 200);

        assertEquals(120, recovery.offset(), 1e-9);
        assertEquals(0, recovery.frames());
    }

    @Test
    void theOverScrollIsClippedAtFortyEvenAfterAJump() {
        assertEquals(540, ZioScroll.recover(700, 500, 3, 200).offset(), 1e-9);
        assertEquals(-40, ZioScroll.recover(-90, 500, 3, 200).offset(), 1e-9);
    }
}
