package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZioColorMarkerTest {
    @Test
    void theMarkerIsThreePixelsAtRestAndFourWhileHeld() {
        assertEquals(3, ZioColorMarker.half(0));
        assertEquals(4, ZioColorMarker.half(0.6));
        assertEquals(4, ZioColorMarker.half(1));
    }

    @Test
    void theMarkerGlidesAtItsOwnSlowerSpeed() {
        double slow = ZioColorMarker.slide(0, 80, 200);
        double fast = ZioAnim.approach(0, 80, 2.0, 200);

        assertTrue(slow > 0, "the first frame should already move");
        assertTrue(slow < fast, "the marker should lag behind the panel animations");
        assertEquals(80, ZioColorMarker.slide(79.999, 80, 200), 1e-9);
    }

    @Test
    void aLightColourTakesABlackBorderOnceItHasFadedIn() {
        assertEquals(ZioColorMarker.BLACK, ZioColorMarker.outline(0.9, 0.9));
        assertEquals(ZioColorMarker.WHITE, ZioColorMarker.outline(0.9, 0.1));
        assertEquals(ZioColorMarker.WHITE, ZioColorMarker.outline(0.1, 0.9));
    }

    @Test
    void theInsideIsTheChosenColourDarkened() {
        int inside = ZioColorMarker.inner(0xFFFFFF);

        assertEquals(0xFF, (inside >>> 24) & 0xFF, "the inside must be opaque");
        assertTrue(ZioColorMarker.luminance(inside) < 1, "white should come out darker");
        assertEquals(ZioColorMarker.inner(0x000000), ZioColorMarker.inner(0x000000));
    }

    @Test
    void aSliderMarkerTurnsWhiteWhileItIsHeld() {
        assertEquals(ZioColorMarker.WHITE, ZioColorMarker.sliderOutline(true));
        assertEquals(ZioColorMarker.ACCENT, ZioColorMarker.sliderOutline(false));
    }

    @Test
    void theAlphaMarkerSitsAtTheOpaqueEndOfTheBar() {
        assertEquals(0, ZioColorMarker.alphaMarker(86, 255), 1e-9);
        assertEquals(85, ZioColorMarker.alphaMarker(86, 0), 1e-9);
        assertEquals(85 * (1 - 128 / 255.0), ZioColorMarker.alphaMarker(86, 128), 1e-9);
    }

    @Test
    void theAlphaBarRunsFromOpaqueToClear() {
        assertEquals(255, ZioColorMarker.alphaAt(86, 0));
        assertEquals(126, ZioColorMarker.alphaAt(86, 43));
        assertEquals(0, ZioColorMarker.alphaAt(86, 85));
        assertEquals(255, ZioColorMarker.alphaAt(1, 0));
    }

    @Test
    void theCheckerboardAlternatesWhiteAndGrey() {
        assertEquals(0xFFFFFFFF, ZioColorMarker.checker(true));
        assertEquals(0xFFAAAAAA, ZioColorMarker.checker(false));
        assertEquals(0xFF999999, ZioColorMarker.alphaChecker(false));
    }

    @Test
    void luminanceReadsWhiteAsOneAndBlackAsZero() {
        assertEquals(1.0, ZioColorMarker.luminance(0xFFFFFF), 1e-3);
        assertEquals(0.0, ZioColorMarker.luminance(0x000000), 1e-9);
    }
}
