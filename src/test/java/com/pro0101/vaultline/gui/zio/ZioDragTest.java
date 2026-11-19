package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/** Dragging a panel: what comes to the front, and where it is allowed to land. */
class ZioDragTest {
    @Test
    void theGrabbedPanelGoesToTheEndAndTheOthersKeepTheirOrder() {
        List<String> panels = List.of("combat", "movement", "render");

        assertEquals(List.of("movement", "render", "combat"), ZioDrag.raised(panels, "combat"));
        assertEquals(List.of("combat", "render", "movement"), ZioDrag.raised(panels, "movement"));
        assertEquals(List.of("combat", "movement", "render"), ZioDrag.raised(panels, "render"));
    }

    @Test
    void raisingDoesNotChangeTheListItIsGiven() {
        List<String> panels = new ArrayList<>(List.of("a", "b", "c"));

        List<String> raised = ZioDrag.raised(panels, "a");

        assertNotSame(panels, raised);
        assertEquals(List.of("a", "b", "c"), panels);
    }

    @Test
    void raisingSomethingThatIsNotInTheListPutsItAtTheEnd() {
        assertEquals(List.of("a", "b", "new"), ZioDrag.raised(List.of("a", "b"), "new"));
        assertEquals(List.of("only"), ZioDrag.raised(List.of(), "only"));
    }

    @Test
    void thePanelStaysInsideTheScreenOnTheLeftAndRightEdges() {
        // 99 wide on a 320 wide screen: from 0 to 221.
        assertEquals(0, ZioDrag.clamp(-40, 320, 99));
        assertEquals(0, ZioDrag.clamp(0, 320, 99));
        assertEquals(120, ZioDrag.clamp(120, 320, 99));
        assertEquals(221, ZioDrag.clamp(221, 320, 99));
        assertEquals(221, ZioDrag.clamp(900, 320, 99));
    }

    @Test
    void thePanelStaysInsideTheScreenOnTheTopAndBottomEdges() {
        // A 200 high panel on a 240 high screen: from 0 to 40.
        assertEquals(0, ZioDrag.clamp(-10, 240, 200));
        assertEquals(20, ZioDrag.clamp(20, 240, 200));
        assertEquals(40, ZioDrag.clamp(40, 240, 200));
        assertEquals(40, ZioDrag.clamp(1000, 240, 200));
    }

    @Test
    void aPanelAsLargeAsTheScreenSitsAtZero() {
        assertEquals(0, ZioDrag.clamp(0, 320, 320));
        assertEquals(0, ZioDrag.clamp(15, 320, 320));
    }

    @Test
    void aPanelTallerThanTheScreenSitsAtZeroRatherThanOffTheTop() {
        // There is no position that shows all of a 400 high panel on a 240 high screen: the copied
        // client's own clamp would have an empty range, so zero is the only usable answer.
        assertEquals(0, ZioDrag.clamp(0, 240, 400));
        assertEquals(0, ZioDrag.clamp(60, 240, 400));
        assertEquals(0, ZioDrag.clamp(-60, 240, 400));
    }

    @Test
    void clampingTwiceChangesNothing() {
        for (int position = -300; position <= 600; position += 7) {
            int once = ZioDrag.clamp(position, 320, 99);

            assertEquals(once, ZioDrag.clamp(once, 320, 99), "clamping " + position + " again moved it");
        }
    }

    @Test
    void theCornerIsThePointerLessTheGrabOffsetClampedToTheScreen() {
        // Grabbed 10 in from the left edge of a 99 wide panel, on a 320 wide screen from 0 to 221.
        assertEquals(90, ZioDrag.corner(100, 10, 320, 99));
        assertEquals(0, ZioDrag.corner(5, 10, 320, 99));
        assertEquals(221, ZioDrag.corner(500, 10, 320, 99));
        // A panel taller than the screen lands at the top edge, whatever the pointer does.
        assertEquals(0, ZioDrag.corner(200, 30, 240, 400));
    }
}
