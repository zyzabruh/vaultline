package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The two animations the copied client uses, at the numbers it uses them with. */
class ZioAnimTest {
    @Test
    void theFrameFactorIsHalfASecondsShareClampedToOne() {
        assertEquals(0.033333333f, ZioAnim.frameFactor(60), 1e-6);
        assertEquals(0.1f, ZioAnim.frameFactor(20), 1e-6);
        // Below two frames a second the factor would pass one, and it is clamped.
        assertEquals(1f, ZioAnim.frameFactor(1), 1e-6);
        assertEquals(1f, ZioAnim.frameFactor(0), 1e-6);
    }

    @Test
    void theEasingStartsAtNothingAndEndsAtOne() {
        assertEquals(0f, ZioAnim.ease(0f), 1e-6);
        assertEquals(1f, ZioAnim.ease(1f), 1e-6);
        // Cubic ease-out: it is already past half way at a third of the factor.
        assertTrue(ZioAnim.ease(0.3333f) > 0.7f);
        assertTrue(ZioAnim.ease(0.5f) > ZioAnim.ease(0.4f));
    }

    @Test
    void anApproachClosesTheDistanceAndSnapsWhenItIsSmall() {
        double value = 0;
        int fps = 60;

        for (int i = 0; i < 200; i++) value = ZioAnim.approach(value, 1, 2, fps);

        assertEquals(1, value, 1e-9);

        // The step grows with the distance: a first frame moves less than the second.
        double first = ZioAnim.approach(0, 1, 2, fps);
        double second = ZioAnim.approach(first, 1, 2, fps);
        assertTrue(second - first < first);
        assertTrue(first > 0 && first < 1);
    }

    @Test
    void aLinearApproachMovesByItsOwnShareWithNoEasing() {
        // At sixty frames a second the frame factor is a thirtieth; at the numeric slider's speed of
        // six that is a fifth of the distance, straight, with no ease-out to enlarge it.
        // The frame factor is a float, so a fifth arrives at a fifth within rounding.
        assertEquals(0.2, ZioAnim.linear(0, 1, ZioAnim.NUMERIC_FILL_SPEED, 60), 1e-6);
        // A fifth of the distance between one and three is four tenths.
        assertEquals(1.4, ZioAnim.linear(1, 3, ZioAnim.NUMERIC_FILL_SPEED, 60), 1e-6);
        // The eased approach covers more of the same distance at the same speed.
        assertTrue(ZioAnim.approach(0, 1, ZioAnim.NUMERIC_FILL_SPEED, 60)
            > ZioAnim.linear(0, 1, ZioAnim.NUMERIC_FILL_SPEED, 60));
        // And it is three times the panel's speed, which is why a slider tracks its value.
        assertTrue(ZioAnim.linear(0, 1, ZioAnim.NUMERIC_FILL_SPEED, 60)
            > 3 * ZioAnim.linear(0, 1, 2, 60) * 0.99);
    }

    @Test
    void aLinearApproachSnapsWhenCloseAndAtLowFrameRates() {
        assertEquals(1, ZioAnim.linear(0.995, 1, 0.35, 60), 1e-9);
        assertEquals(1, ZioAnim.linear(0, 1, 6, 12), 1e-9);
        assertEquals(0, ZioAnim.linear(1, 0, 6, 0), 1e-9);
    }

    @Test
    void aSlowFrameRateSnaps() {
        assertEquals(1, ZioAnim.approach(0, 1, 2, 12), 1e-9);
        assertEquals(0, ZioAnim.approach(1, 0, 2, 12), 1e-9);
        // An unmeasured frame rate is treated the same way, so a first frame cannot stall.
        assertEquals(1, ZioAnim.approach(0, 1, 2, 0), 1e-9);
    }

    @Test
    void aTimedValueRunsStraightBetweenTwoValues() {
        ZioAnim.Timed timed = new ZioAnim.Timed(0f);

        timed.setTarget(1000L, 1f, 200L);

        assertEquals(0f, timed.value(1000L), 1e-6);
        assertEquals(0.5f, timed.value(1100L), 1e-6);
        assertEquals(1f, timed.value(1200L), 1e-6);
        assertEquals(1f, timed.value(5000L), 1e-6);
    }

    @Test
    void aTimedValueRetargetsFromWhereItIsNow() {
        ZioAnim.Timed timed = new ZioAnim.Timed(0f);

        timed.setTarget(1000L, 1f, 200L);
        // Half way, then reversed: the new run starts at a half, not at zero.
        timed.setTarget(1100L, 0f, 100L);

        assertEquals(0.5f, timed.value(1100L), 1e-6);
        assertEquals(0.25f, timed.value(1150L), 1e-6);
        assertEquals(0f, timed.value(1200L), 1e-6);
    }

    @Test
    void aZeroDurationJumpIsImmediate() {
        ZioAnim.Timed timed = new ZioAnim.Timed(0f);

        timed.set(1000L, 0.75f);

        assertEquals(0.75f, timed.value(1000L), 1e-6);
        assertEquals(0.75f, timed.target(), 1e-6);
    }
}
