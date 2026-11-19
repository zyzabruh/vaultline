package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The numbers the panels are painted with: the sheet's place, its alpha, the outline and the header. */
class ZioPanelPaintTest {
    @Test
    void theSheetIsShiftedByThePanelsPlaceNotStretched() {
        // The copied client draws at -300 + panelX / 3 inside a translate by panelX.
        assertEquals(-300, ZioPanelPaint.aeroLeft(0));
        assertEquals(-168, ZioPanelPaint.aeroLeft(99));
        assertEquals(100, ZioPanelPaint.aeroLeft(300));

        // Two panels apart by a panel each keep the sheet moving with the screen.
        assertEquals(ZioPanelPaint.aeroLeft(99) + 102 + 34, ZioPanelPaint.aeroLeft(201));
    }

    @Test
    void theSheetKeepsItsOwnSize() {
        assertEquals(600, ZioPanelPaint.AERO_WIDTH);
        assertEquals(450, ZioPanelPaint.AERO_HEIGHT);
    }

    @Test
    void theSheetIsDrawnAtHalfTheScreenFade() {
        assertEquals(0, ZioPanelPaint.aeroAlpha(0));
        assertEquals(127, ZioPanelPaint.aeroAlpha(1));
        assertEquals(64, ZioPanelPaint.aeroAlpha(0.5));
        // Out of range fades are clamped rather than wrapping the alpha.
        assertEquals(127, ZioPanelPaint.aeroAlpha(2));
        assertEquals(0, ZioPanelPaint.aeroAlpha(-1));
    }

    @Test
    void theOutlineNeverShorterThanTheHeader() {
        assertEquals(14, ZioPanelPaint.outlineHeight(14, 0, 14));
        assertEquals(14, ZioPanelPaint.outlineHeight(16, 0, 14));
        assertEquals(14, ZioPanelPaint.outlineHeight(40, 0, 14));
        assertEquals(14, ZioPanelPaint.outlineHeight(40, 0.1, 14));
        assertEquals(41, ZioPanelPaint.outlineHeight(40, 1, 14));
    }

    @Test
    void theHeaderIsTwelveInSlimAndFourteenOtherwise() {
        assertEquals(12, ZioPanelPaint.headerHeight(true));
        assertEquals(14, ZioPanelPaint.headerHeight(false));
        assertEquals(0f, ZioPanelPaint.headerVerticalOffset(true), 1e-6);
        assertEquals(0.5f, ZioPanelPaint.headerVerticalOffset(false), 1e-6);
    }

    @Test
    void theScreenTintAndGradientAreTheUiModulesDefaults() {
        assertEquals(0x64, (ZioPanelPaint.TINT >>> 24) & 0xFF);
        assertEquals(0x200D30, ZioPanelPaint.TINT & 0xFFFFFF);
        assertEquals(0x0F, (ZioPanelPaint.GRADIENT >>> 24) & 0xFF);
        assertEquals(0xC794F4, ZioPanelPaint.GRADIENT & 0xFFFFFF);

        // The gradient runs from its own colour at no alpha to the same colour at its alpha.
        int top = ZioPanelPaint.GRADIENT & 0x00FFFFFF;
        assertEquals(0, (top >>> 24) & 0xFF);
        assertEquals(ZioPanelPaint.GRADIENT & 0xFFFFFF, top & 0xFFFFFF);
        assertTrue((ZioPanelPaint.GRADIENT >>> 24) > 0);
    }
}
