package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Themable HUD safe zones: defaults, hit bands, drag and fades. */
class ZioHudMarginsTest {
    private static final int WIDTH = 320;
    private static final int HEIGHT = 240;

    private ZioHudMargins margins;

    @BeforeEach
    void setUp() {
        margins = new ZioHudMargins();
    }

    @Test
    void theDefaultsAreTheCopiedClientsDefaults() {
        assertEquals(1, ZioHudMargins.DEFAULT_SAFE_X);
        assertEquals(1, ZioHudMargins.DEFAULT_SAFE_Y);
        assertEquals(1, margins.safeX());
        assertEquals(1, margins.safeY());
        assertEquals(20, ZioHudMargins.MAX_SAFE_X);
        assertEquals(10, ZioHudMargins.MAX_SAFE_Y);
    }

    @Test
    void aMarginCountsAsHitOnePixelInsideIt() {
        margins.setSafeX(4);
        margins.setSafeY(6);

        assertTrue(margins.overHorizontalMargin(0, WIDTH));
        assertTrue(margins.overHorizontalMargin(4, WIDTH));
        assertFalse(margins.overHorizontalMargin(5, WIDTH));

        assertTrue(margins.overHorizontalMargin(WIDTH - 4, WIDTH));
        assertFalse(margins.overHorizontalMargin(WIDTH - 5, WIDTH));

        assertTrue(margins.overVerticalMargin(6, HEIGHT));
        assertFalse(margins.overVerticalMargin(7, HEIGHT));
        assertTrue(margins.overVerticalMargin(HEIGHT - 6, HEIGHT));
        assertFalse(margins.overVerticalMargin(HEIGHT - 7, HEIGHT));
    }

    @Test
    void aPressOnASideBandDragsItAndTheMarginFollowsThePointer() {
        margins.setSafeX(4);

        margins.click(2, 120, 0, WIDTH, HEIGHT);
        assertTrue(margins.draggingHorizontal());
        assertFalse(margins.draggingVertical());

        margins.update(0, 9, 120, WIDTH, HEIGHT);
        assertEquals(9, margins.safeX());

        margins.release(0);
        assertFalse(margins.draggingHorizontal());

        margins.update(1000, 40, 120, WIDTH, HEIGHT);
        assertEquals(9, margins.safeX());
    }

    @Test
    void aPressOnTheRightHalfMeasuresTheMarginFromTheEdge() {
        margins.setSafeX(4);
        margins.click(WIDTH - 2, 120, 0, WIDTH, HEIGHT);

        margins.update(0, WIDTH - 12, 120, WIDTH, HEIGHT);

        assertEquals(12, margins.safeX());
    }

    @Test
    void theVerticalBandsTakeOverOnlyWhenTheSideBandsAreAway() {
        margins.setSafeY(3);

        // A press on the bottom band, with the pointer away from the sides.
        margins.click(160, HEIGHT - 2, 0, WIDTH, HEIGHT);
        assertTrue(margins.draggingVertical());

        margins.update(0, 160, HEIGHT - 9, WIDTH, HEIGHT);
        assertEquals(9, margins.safeY());
    }

    @Test
    void aMarginIsKeptInsideTheCopiedClientsBounds() {
        margins.setSafeX(500);
        assertEquals(ZioHudMargins.MAX_SAFE_X, margins.safeX());

        margins.setSafeY(-4);
        assertEquals(0, margins.safeY());
    }

    @Test
    void theBandsFadeInOverTheirQuarterSecond() {
        // Pointer on the left band.
        margins.update(0, 0, 120, WIDTH, HEIGHT);

        assertEquals(0f, margins.horizontalOverlay(0));
        assertEquals(ZioHudMargins.OVERLAY_ALPHA, margins.horizontalOverlay(ZioHudMargins.OVERLAY_MILLIS));

        float half = margins.horizontalOverlay(ZioHudMargins.OVERLAY_MILLIS / 2);

        assertTrue(half > 0f && half < ZioHudMargins.OVERLAY_ALPHA);

        // Pointer away from every band: it fades back out.
        margins.update(ZioHudMargins.OVERLAY_MILLIS, 160, 120, WIDTH, HEIGHT);

        assertEquals(ZioHudMargins.OVERLAY_ALPHA, margins.horizontalOverlay(ZioHudMargins.OVERLAY_MILLIS));
        assertEquals(0f, margins.horizontalOverlay(ZioHudMargins.OVERLAY_MILLIS * 2));
    }

    @Test
    void theVerticalBandsWaitForTheSideBandsToFade() {
        // The pointer sits in a corner, inside both bands; the side ones win.
        margins.setSafeX(5);
        margins.setSafeY(5);

        margins.update(0, 1, 1, WIDTH, HEIGHT);
        margins.update(ZioHudMargins.OVERLAY_MILLIS, 1, 1, WIDTH, HEIGHT);

        assertTrue(margins.horizontalOverlay(ZioHudMargins.OVERLAY_MILLIS) > 0f);

        // The frame after that sends the top bands back to zero, because the sides are showing.
        assertEquals(0f, margins.verticalOverlay(ZioHudMargins.OVERLAY_MILLIS * 2));

        // The pointer leaves the sides, so they fade out, and only then does the top band come in.
        margins.update(ZioHudMargins.OVERLAY_MILLIS * 2, 160, 1, WIDTH, HEIGHT);
        margins.update(ZioHudMargins.OVERLAY_MILLIS * 3, 160, 1, WIDTH, HEIGHT);

        assertEquals(0f, margins.horizontalOverlay(ZioHudMargins.OVERLAY_MILLIS * 3));
        assertEquals(ZioHudMargins.OVERLAY_ALPHA, margins.verticalOverlay(ZioHudMargins.OVERLAY_MILLIS * 4));
    }
}
