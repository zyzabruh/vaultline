package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The open enumeration's sliding bar and the outline around its box. */
class ZioEnumBoxTest {
    @Test
    void theBarStartsOnTheFirstChoice() {
        ZioEnumBox.Rect bar = ZioEnumBox.selection(10, 40, 92, 11, 0);

        assertEquals(13, bar.left());
        assertEquals(51, bar.top());
        assertEquals(100, bar.right());
        assertEquals(62, bar.bottom());
    }

    @Test
    void theBarFollowsHowFarItHasSlid() {
        assertEquals(51, ZioEnumBox.selection(10, 40, 92, 11, 0).top());
        assertEquals(62, ZioEnumBox.selection(10, 40, 92, 11, 11).top());
        assertEquals(84, ZioEnumBox.selection(10, 40, 92, 11, 33).top());
        // Half way between two choices reads half way, before it is rounded to a pixel.
        assertEquals(56, ZioEnumBox.selection(10, 40, 92, 11, 5.4).top());
    }

    @Test
    void theBarNeverSitsAboveTheFirstChoice() {
        // A negative slide is how the bar hides behind the setting's own line when the list folds.
        assertEquals(51, ZioEnumBox.selection(10, 40, 92, 11, -11).top());
        assertEquals(51, ZioEnumBox.selection(10, 40, 92, 11, -50).top());
    }

    @Test
    void theBarIsAlwaysOneLineTallAndTwoPixelsShortOfTheEdge() {
        ZioEnumBox.Rect bar = ZioEnumBox.selection(10, 40, 92, 11, 22);

        assertEquals(11, bar.bottom() - bar.top());
        assertEquals(3, bar.left() - 10);
        assertEquals(2, 10 + 92 - bar.right());
    }

    @Test
    void theOutlineClosesTheSettingLineAndEveryChoice() {
        ZioEnumBox.Rect box = ZioEnumBox.outline(10, 40, 92, 11, 3);

        assertEquals(12, box.left());
        assertEquals(40, box.top());
        assertEquals(100, box.right());
        assertEquals(84, box.bottom(), "three choices plus the setting's own line");
    }

    @Test
    void aListWithOneChoiceIsStillOneLineTallBelowItsSetting() {
        assertEquals(62, ZioEnumBox.outline(10, 40, 92, 11, 1).bottom());
    }

    @Test
    void theSlideApproachesTheChosenChoiceAndSettlesOnIt() {
        double slide = 0;
        double target = 3 * 11;

        for (int frame = 0; frame < 200; frame++) {
            double next = ZioEnumBox.slide(slide, target, 200);

            assertTrue(next >= slide, "the bar slid backwards");
            slide = next;
        }

        assertEquals(target, slide, 1e-9);
    }

    @Test
    void theSlideStartsMovingAtOnceAndDoesNotOvershoot() {
        double first = ZioEnumBox.slide(0, 11, 200);

        assertTrue(first > 0 && first < 11, "the first frame should be part of the way there");
        assertEquals(11, ZioEnumBox.slide(11, 11, 200), 1e-9);
    }
}
