package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The zoom limits and label, checked without a game client. */
class ZioScaleTest {
    @Test
    void theDefaultIsInsideTheRange() {
        assertEquals(ZioScale.DEFAULT, ZioScale.clamp(ZioScale.DEFAULT), 1e-6);
    }

    @Test
    void aScaleIsKeptInsideWhatIsReadable() {
        assertEquals(ZioScale.MIN, ZioScale.clamp(0.1f), 1e-6);
        assertEquals(ZioScale.MAX, ZioScale.clamp(4f), 1e-6);
    }

    @Test
    void aScaleSnapsToWholeSteps() {
        assertEquals(0.5f, ZioScale.clamp(0.51f), 1e-6);
        assertEquals(0.75f, ZioScale.clamp(0.7f), 1e-6);
        assertEquals(1.0f, ZioScale.clamp(1.05f), 1e-6);
    }

    @Test
    void steppingMovesByOneSizeAndStopsAtTheEnds() {
        assertEquals(1.0f, ZioScale.stepped(0.75f, 1), 1e-6);
        assertEquals(0.5f, ZioScale.stepped(0.75f, -1), 1e-6);
        assertEquals(ZioScale.MAX, ZioScale.stepped(ZioScale.MAX, 3), 1e-6);
        assertEquals(ZioScale.MIN, ZioScale.stepped(ZioScale.MIN, -3), 1e-6);
    }

    @Test
    void theWheelStepsAreTakenAsTheyCome() {
        assertEquals(1.0f, ZioScale.stepped(0.5f, 2), 1e-6);
        assertEquals(0.5f, ZioScale.stepped(1.0f, -2), 1e-6);
    }

    @Test
    void aSavedScaleIsReadBackExactly() {
        for (float value = ZioScale.MIN; value <= ZioScale.MAX; value += ZioScale.STEP) {
            assertEquals(ZioScale.clamp(value), ZioScale.parse(ZioScale.format(value)), 1e-6);
        }

        assertEquals("0.75", ZioScale.format(0.75f));
    }

    @Test
    void aSavedScaleThatIsMissingOrBrokenFallsBackToTheDefault() {
        assertEquals(ZioScale.DEFAULT, ZioScale.parse(null), 1e-6);
        assertEquals(ZioScale.DEFAULT, ZioScale.parse(""), 1e-6);
        assertEquals(ZioScale.DEFAULT, ZioScale.parse("   "), 1e-6);
        assertEquals(ZioScale.DEFAULT, ZioScale.parse("not a number"), 1e-6);
    }

    @Test
    void aSavedScaleOutOfRangeIsBroughtBackIn() {
        assertEquals(ZioScale.MAX, ZioScale.parse("9.5"), 1e-6);
        assertEquals(ZioScale.MIN, ZioScale.parse("0.05"), 1e-6);
        assertEquals(ZioScale.MAX, ZioScale.parse(" 1.25 "), 1e-6);
    }

    @Test
    void theScaleIsShownAsAPercentage() {
        assertEquals("50%", ZioScale.label(0.5f));
        assertEquals("75%", ZioScale.label(0.75f));
        assertEquals("100%", ZioScale.label(1.0f));
        assertEquals("125%", ZioScale.label(1.25f));
        assertEquals("125%", ZioScale.label(9f));
    }
}
