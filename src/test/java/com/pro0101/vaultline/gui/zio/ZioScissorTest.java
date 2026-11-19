package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The nesting of scissor regions, and the parent each close hands back. */
class ZioScissorTest {
    @Test
    void aRegionOnItsOwnIsOpenedAsAsked() {
        ZioScissor scissors = new ZioScissor();

        assertArrayEquals(new int[]{10, 20, 40, 60}, scissors.push(10, 20, 40, 60));
    }

    @Test
    void aNestedRegionIsClippedToItsParent() {
        ZioScissor scissors = new ZioScissor();

        scissors.push(10, 20, 100, 200);

        // Wider than the parent on every side: only the overlap survives.
        assertArrayEquals(new int[]{10, 20, 100, 200}, scissors.push(0, 0, 500, 500));

        // And the reverse: a small region inside a big one is left alone.
        assertArrayEquals(new int[]{30, 40, 50, 60}, scissors.push(30, 40, 50, 60));
    }

    @Test
    void aNestedRegionOnlyTouchingOnOneSideIsClippedThere() {
        ZioScissor scissors = new ZioScissor();

        scissors.push(10, 20, 100, 200);

        assertArrayEquals(new int[]{10, 60, 100, 90}, scissors.push(0, 60, 120, 90));
    }

    @Test
    void aRegionClippedAwayCollapsesOntoTheParentEdge() {
        ZioScissor below = new ZioScissor();
        ZioScissor beside = new ZioScissor();

        below.push(10, 20, 100, 200);
        beside.push(10, 20, 100, 200);

        // Entirely below the parent: nothing of it can show, and the game still needs a rectangle. Only
        // the collapsed axis moves, onto the parent's edge, so the region stays inside it.
        assertArrayEquals(new int[]{30, 20, 60, 21}, below.push(30, 300, 60, 340));
        assertArrayEquals(new int[]{10, 30, 11, 60}, beside.push(300, 30, 340, 60));
    }

    @Test
    void closingHandsBackTheParentEachTime() {
        ZioScissor scissors = new ZioScissor();

        scissors.push(0, 0, 800, 600);
        scissors.push(10, 20, 100, 200);
        scissors.push(30, 40, 50, 60);

        assertArrayEquals(new int[]{10, 20, 100, 200}, scissors.pop());
        assertArrayEquals(new int[]{0, 0, 800, 600}, scissors.pop());
        assertNull(scissors.pop());
    }

    @Test
    void closingWhenNothingIsOpenIsHarmless() {
        ZioScissor scissors = new ZioScissor();

        assertNull(scissors.pop());

        // The stack still works afterwards.
        assertArrayEquals(new int[]{1, 2, 3, 4}, scissors.push(1, 2, 3, 4));
        assertNull(scissors.pop());
    }

    @Test
    void clearingForgetsEveryRegion() {
        ZioScissor scissors = new ZioScissor();

        scissors.push(0, 0, 800, 600);
        scissors.push(10, 20, 100, 200);
        scissors.clear();

        // A fresh region is no longer clipped by the cleared ones.
        assertArrayEquals(new int[]{500, 500, 700, 700}, scissors.push(500, 500, 700, 700));
    }

    @Test
    void aPanelClippedSettingsBlockStaysInsideThePanel() {
        // The reported case: a module open near the bottom of the screen asks for a tall settings
        // region, and only the part still inside the panel body may be drawn.
        ZioScissor scissors = new ZioScissor();

        scissors.push(10, 100, 109, 300);          // the panel body
        int[] settings = scissors.push(11, 250, 108, 340); // a module's settings

        assertArrayEquals(new int[]{11, 250, 108, 300}, settings);
    }
}
