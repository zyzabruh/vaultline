package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The chrome under the panels, which has to stay clear of the button beside it and inside the
 * screen at both ends of the zoom.
 */
class ZioChromeTest {
    // A narrow window at 125% leaves the least layout room, a wide one at 50% the most.
    private static final int NARROW = 320;
    private static final int WIDE = 854;
    private static final int VIEW_HEIGHT = 240;

    private static List<ZioChrome.Rect> controls(int viewWidth) {
        return List.of(
            ZioChrome.presetsButton(viewWidth, VIEW_HEIGHT),
            ZioChrome.zoomMinus(viewWidth, VIEW_HEIGHT),
            ZioChrome.zoomLabel(viewWidth, VIEW_HEIGHT),
            ZioChrome.zoomPlus(viewWidth, VIEW_HEIGHT),
            ZioChrome.gameButton(viewWidth, VIEW_HEIGHT),
            ZioChrome.fxButton(viewWidth, VIEW_HEIGHT)
        );
    }

    @Test
    void everyControlIsOnScreenAtBothEndsOfTheZoom() {
        for (int width : new int[]{NARROW, WIDE}) {
            for (ZioChrome.Rect rect : controls(width)) {
                assertTrue(ZioChrome.inside(rect, width, VIEW_HEIGHT), rect + " is off screen at " + width);
            }
        }
    }

    @Test
    void everyControlSitsInTheStripThePanelsLeaveFree() {
        for (int width : new int[]{NARROW, WIDE}) {
            for (ZioChrome.Rect rect : controls(width)) {
                assertTrue(ZioChrome.inStrip(rect, VIEW_HEIGHT), rect + " is over the panels at " + width);
            }
        }
    }

    @Test
    void noControlOverlapsAnother() {
        for (int width : new int[]{NARROW, WIDE}) {
            List<ZioChrome.Rect> rects = controls(width);

            for (int i = 0; i < rects.size(); i++) {
                for (int j = i + 1; j < rects.size(); j++) {
                    assertFalse(rects.get(i).overlaps(rects.get(j)),
                        rects.get(i) + " overlaps " + rects.get(j) + " at " + width);
                }
            }
        }
    }

    @Test
    void thePresetButtonAndTheZoomControlsAreLaidOutLeftToRight() {
        ZioChrome.Rect presets = ZioChrome.presetsButton(NARROW, VIEW_HEIGHT);
        ZioChrome.Rect minus = ZioChrome.zoomMinus(NARROW, VIEW_HEIGHT);
        ZioChrome.Rect label = ZioChrome.zoomLabel(NARROW, VIEW_HEIGHT);
        ZioChrome.Rect plus = ZioChrome.zoomPlus(NARROW, VIEW_HEIGHT);

        assertTrue(presets.right() < minus.x());
        assertTrue(minus.right() < label.x());
        assertTrue(label.right() <= plus.x());
        assertTrue(plus.right() <= NARROW);

        // All four share the same line.
        assertTrue(presets.y() == minus.y() && minus.y() == label.y() && label.y() == plus.y());
    }

    @Test
    void aClickInsideAControlHitsItAndOneOutsideDoesNot() {
        ZioChrome.Rect plus = ZioChrome.zoomPlus(NARROW, VIEW_HEIGHT);

        assertTrue(plus.contains(plus.x(), plus.y()));
        assertTrue(plus.contains(plus.right(), plus.bottom()));
        assertFalse(plus.contains(plus.x() - 1, plus.y()));
        assertFalse(plus.contains(plus.right() + 1, plus.y()));

        ZioChrome.Rect presets = ZioChrome.presetsButton(NARROW, VIEW_HEIGHT);

        assertTrue(presets.contains(presets.x() + 1, presets.y() + 1));
        assertFalse(presets.contains(presets.right() + 1, presets.y() + 1));
    }

    @Test
    void theControlsClearThePanelLimit() {
        int limit = VIEW_HEIGHT - ZioChrome.HEIGHT_MARGIN;

        for (int width : new int[]{NARROW, WIDE}) {
            for (ZioChrome.Rect rect : controls(width)) {
                assertTrue(rect.y() >= limit, rect + " reaches into the panels at " + width);
                assertTrue(rect.bottom() <= VIEW_HEIGHT);
            }
        }
    }
}
