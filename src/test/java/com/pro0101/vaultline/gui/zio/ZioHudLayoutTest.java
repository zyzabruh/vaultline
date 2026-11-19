package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Moving, resizing and snapping HUD elements in the editor. */
class ZioHudLayoutTest {
    private static final int WIDTH = 320;
    private static final int HEIGHT = 240;

    private static ZioHudLayout.Rect rect(int x, int y, int width, int height) {
        return new ZioHudLayout.Rect(x, y, width, height);
    }

    @Test
    void aForcedSizeGrowsAnElementButNeverShrinksItUnderItsOwnContent() {
        // A 120x40 request on a 60x20 element: the box grows.
        assertArrayEquals(new int[]{120, 40}, ZioHudLayout.forcedBox(60, 20, new int[]{120, 40}));

        // A 30x10 request on the same element: the measurement wins, so no text spills out.
        assertArrayEquals(new int[]{60, 20}, ZioHudLayout.forcedBox(60, 20, new int[]{30, 10}));

        // Without a forced size the measurement is the box.
        assertArrayEquals(new int[]{60, 20}, ZioHudLayout.forcedBox(60, 20, null));
    }

    @Test
    void aDragKeepsTheOffsetTheElementWasGrabbedAt() {
        ZioHudLayout.Rect start = rect(40, 30, 60, 20);

        // Grabbed 10 pixels inside, moved to 100/80.
        ZioHudLayout.Rect moved = ZioHudLayout.drag(start, 10, 5, 100, 80, WIDTH, HEIGHT);

        assertEquals(90, moved.x());
        assertEquals(75, moved.y());
        assertEquals(60, moved.width());
        assertEquals(20, moved.height());
    }

    @Test
    void aDragNeverLeavesTheScreen() {
        ZioHudLayout.Rect start = rect(40, 30, 60, 20);

        assertEquals(0, ZioHudLayout.drag(start, 10, 5, -100, -100, WIDTH, HEIGHT).x());
        assertEquals(0, ZioHudLayout.drag(start, 10, 5, -100, -100, WIDTH, HEIGHT).y());

        ZioHudLayout.Rect far = ZioHudLayout.drag(start, 10, 5, 1000, 1000, WIDTH, HEIGHT);

        assertEquals(WIDTH - 60, far.x());
        assertEquals(HEIGHT - 20, far.y());
    }

    @Test
    void aClampKeepsAnElementInsideTheScreen() {
        assertEquals(rect(0, 0, 60, 20), ZioHudLayout.clamp(rect(-30, -10, 60, 20), WIDTH, HEIGHT));
        assertEquals(rect(WIDTH - 60, HEIGHT - 20, 60, 20),
            ZioHudLayout.clamp(rect(999, 999, 60, 20), WIDTH, HEIGHT));
    }

    @Test
    void theResizeCornerGrowsTheElementButNotBelowTheMinimum() {
        ZioHudLayout.Rect start = rect(20, 20, 40, 20);

        ZioHudLayout.Rect grown = ZioHudLayout.resize(start, 100, 90);

        assertEquals(20, grown.x());
        assertEquals(20, grown.y());
        assertEquals(80, grown.width());
        assertEquals(70, grown.height());

        ZioHudLayout.Rect shrunk = ZioHudLayout.resize(start, 21, 21);

        assertEquals(ZioHudLayout.MIN_WIDTH, shrunk.width());
        assertEquals(ZioHudLayout.MIN_HEIGHT, shrunk.height());
    }

    @Test
    void aResizeStopsAtTheScreenEdges() {
        ZioHudLayout.Rect start = rect(280, 200, 20, 20);

        ZioHudLayout.Rect resized = ZioHudLayout.resize(start, 900, 900, WIDTH, HEIGHT);

        assertEquals(280, resized.x());
        assertEquals(200, resized.y());
        assertEquals(WIDTH - 280, resized.width());
        assertEquals(HEIGHT - 200, resized.height());
    }

    @Test
    void aResizedElementStillDragsWithItsNewSize() {
        ZioHudLayout.Rect resized = ZioHudLayout.resize(rect(20, 20, 40, 20), 120, 90, WIDTH, HEIGHT);

        assertEquals(100, resized.width());
        assertEquals(70, resized.height());

        ZioHudLayout.Rect dragged = ZioHudLayout.drag(resized, 5, 5, WIDTH + 100, HEIGHT + 100, WIDTH, HEIGHT);

        assertEquals(100, dragged.width());
        assertEquals(70, dragged.height());
        assertEquals(WIDTH - 100, dragged.x());
        assertEquals(HEIGHT - 70, dragged.y());
    }

    @Test
    void aResizedElementStillSnapsToTheNearEdgeKeepingItsSize() {
        ZioHudLayout.Rect resized = ZioHudLayout.resize(rect(100, 100, 40, 20), 160, 130, WIDTH, HEIGHT);

        assertEquals(60, resized.width());
        assertEquals(30, resized.height());

        ZioHudLayout.Rect snapped = ZioHudLayout.snap(new ZioHudLayout.Rect(3, 100, 60, 30),
            WIDTH, HEIGHT, 2, 2);

        assertEquals(2, snapped.x());
        assertEquals(60, snapped.width());
        assertEquals(30, snapped.height());
    }

    @Test
    void theResizeCornerIsTheBottomRightPatch() {
        ZioHudLayout.Rect r = rect(20, 20, 40, 20);

        assertTrue(ZioHudLayout.overResizeHandle(r, 59, 39));
        assertTrue(ZioHudLayout.overResizeHandle(r, 60 - ZioHudLayout.RESIZE_HANDLE, 40 - ZioHudLayout.RESIZE_HANDLE));
        assertFalse(ZioHudLayout.overResizeHandle(r, 30, 30));
        assertFalse(ZioHudLayout.overResizeHandle(r, 61, 41));
    }

    @Test
    void anEdgeWithinFivePixelsSnapsToTheSafeMargin() {
        // Left edge, 3 pixels from the margin of 2.
        assertEquals(2, ZioHudLayout.snap(rect(5, 100, 40, 20), WIDTH, HEIGHT, 2, 2).x());

        // Right edge: 320 - 2 - 40 = 278; the element's right side is 314, one past 313.
        assertEquals(WIDTH - 2 - 40, ZioHudLayout.snap(rect(274, 100, 40, 20), WIDTH, HEIGHT, 2, 2).x());

        // Top and bottom.
        assertEquals(2, ZioHudLayout.snap(rect(100, 4, 40, 20), WIDTH, HEIGHT, 2, 2).y());
        assertEquals(HEIGHT - 2 - 20, ZioHudLayout.snap(rect(100, 215, 40, 20), WIDTH, HEIGHT, 2, 2).y());
    }

    @Test
    void theCentreSnapsWhenTheMiddleIsClose() {
        // Element 40 wide: its centre is x + 20; the screen centre is 160.
        assertEquals(140, ZioHudLayout.snap(rect(143, 100, 40, 20), WIDTH, HEIGHT, 1, 1).x());
        assertEquals(140, ZioHudLayout.snap(rect(137, 100, 40, 20), WIDTH, HEIGHT, 1, 1).x());
    }

    @Test
    void anElementAwayFromEveryGuideStaysWhereItWas() {
        ZioHudLayout.Rect free = rect(120, 90, 40, 20);

        assertEquals(free, ZioHudLayout.snap(free, WIDTH, HEIGHT, 1, 1));
    }
}
