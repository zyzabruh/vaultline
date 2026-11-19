package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The HUD editor's centre guides and its hover label. */
class ZioHudEditorFxTest {
    @Test
    void theGuidesMeetInTheMiddleOfTheScreen() {
        ZioHudLayout.Rect[] guides = ZioHudLayout.centreGuides(320, 240);

        assertEquals(3, guides.length);
        assertEquals(160, guides[0].x());
        assertEquals(1, guides[0].width());
        assertEquals(240, guides[0].height(), "the vertical guide runs the full height");
        assertEquals(0, guides[1].x());
        assertEquals(120, guides[1].y());
        assertEquals(160, guides[1].width(), "the left half stops at the centre");
        assertEquals(1, guides[1].height());
        assertEquals(161, guides[2].x());
        assertEquals(120, guides[2].y());
        assertEquals(159, guides[2].width(), "the right half starts after the vertical line");
    }

    @Test
    void theGuidesStayInsideAnOddSizedScreen() {
        ZioHudLayout.Rect[] guides = ZioHudLayout.centreGuides(321, 241);

        assertEquals(160, guides[0].x());
        assertEquals(0, guides[1].x());
        assertEquals(160, guides[1].width());
        assertEquals(161, guides[2].x());
        assertEquals(160, guides[2].width());
        assertEquals(321, guides[2].right(), "the guides must reach the right edge");
    }

    @Test
    void aLabelSlidesInFromTenPixelsAway() {
        assertEquals(10, ZioHudLayout.labelOffset(0, 1));
        assertEquals(0, ZioHudLayout.labelOffset(1, 1));
        assertEquals(5, ZioHudLayout.labelOffset(0.5, 1));
        assertEquals(-10, ZioHudLayout.labelOffset(0, -1));
        assertEquals(0, ZioHudLayout.labelOffset(1.5, -1), "an overshooting fade must not push it past");
    }

    @Test
    void theLabelFadesToWhiteAndNeverPastIt() {
        assertEquals(0, ZioHudLayout.labelAlpha(0));
        assertEquals(128, ZioHudLayout.labelAlpha(0.5));
        assertEquals(255, ZioHudLayout.labelAlpha(1));
        assertEquals(255, ZioHudLayout.labelAlpha(2));
    }

    @Test
    void theLabelEasesAtTheClientsSpeedOfTwo() {
        double first = ZioAnim.approach(0, 1, ZioHudLayout.HOVER_SPEED, 200);

        assertEquals(0, ZioAnim.approach(0, 0, ZioHudLayout.HOVER_SPEED, 200), 1e-9);
        assertEquals(1, ZioAnim.approach(1, 1, ZioHudLayout.HOVER_SPEED, 200), 1e-9);
        assertEquals(ZioAnim.approach(0, 1, 2.0, 200), first, 1e-9);
    }
}
