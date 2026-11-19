package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two effects of the ported screen that live in {@link ZioPaint} but are not part of its main suite:
 * the gradient every row carries, and the speed at which the tooltip chases the pointer.
 *
 * <p>They are here rather than in {@code ZioPaintTest} so that file keeps exactly the tests it had
 * when the port was first closed.
 */
class ZioPaintEffectsTest {
    @Test
    void theTooltipChasesThePointerFasterThanThePanelsAnimate() {
        assertEquals(2.5f, ZioPaint.TOOLTIP_FOLLOW_SPEED);
        assertEquals(0, ZioPaint.follow(0, 0, 200), 1e-9, "a still pointer leaves it where it is");

        double firstFrame = ZioPaint.follow(0, 100, 200);

        assertTrue(firstFrame > 0 && firstFrame < 100);
        assertTrue(firstFrame > ZioAnim.approach(0, 100, 2.0, 200),
            "2.5 should catch up sooner than the panels' 2");
    }

    @Test
    void aButtonRowCarriesTheGradientFromATenthOfItsAlphaToNothing() {
        RecordingCanvas canvas = new RecordingCanvas();

        ZioPaint.buttonRow(canvas, 10, 20, 92, 13, 0x73C794F4, 1);

        RecordingCanvas.Fill fill = canvas.lastFill();

        assertEquals(11, fill.x1());
        assertEquals(20, fill.y1());
        assertEquals(101, fill.x2(), "one pixel short of the row's right edge");
        assertEquals(32, fill.y2(), "one pixel short of the row's bottom edge");
        assertEquals(0x0CC794F4, fill.argb(), "a tenth of the button colour's alpha");
        assertEquals(0x00C794F4, fill.gradientTo());
    }

    @Test
    void theBlurredRectangleIsExactlyTheOneTheTooltipFills() {
        RecordingCanvas canvas = new RecordingCanvas();
        String[] lines = {"leaves a trail", "behind you"};

        // The screen decides this rectangle once a frame, hands it to the blur, then draws in it.
        ZioPaint.Bounds bounds = ZioPaint.tooltipBounds(canvas, 320, 100, 100, lines);

        ZioPaint.tooltip(canvas, bounds, lines, 0x50101010, 0xFFFFFFFF);

        RecordingCanvas.Fill fill = canvas.fills().get(0);

        assertEquals(bounds, new ZioPaint.Bounds(fill.x1(), fill.y1(), fill.x2(), fill.y2()));
        assertEquals(0x50101010, fill.argb());

        // And the lines start one pixel inside it, one line height apart.
        assertEquals(bounds.left() + 1f, canvas.lines().get(0).x());
        assertEquals(bounds.top() + 1f, canvas.lines().get(0).y());
        assertEquals(bounds.top() + 1f + canvas.lineHeight() + 1, canvas.lines().get(1).y());
    }

    @Test
    void aPointerThatMovedBetweenFramesMovesTheBlurAndTheTooltipTogether() {
        RecordingCanvas canvas = new RecordingCanvas();
        String[] lines = {"one line"};

        ZioPaint.Bounds first = ZioPaint.tooltipBounds(canvas, 320, 100, 100, lines);
        ZioPaint.Bounds second = ZioPaint.tooltipBounds(canvas, 320, 140, 120, lines);

        assertTrue(second.left() > first.left(), "the rectangle follows the pointer sideways");
        assertTrue(second.top() > first.top(), "and downwards");

        ZioPaint.tooltip(canvas, second, lines, 0x50101010, 0xFFFFFFFF);

        RecordingCanvas.Fill fill = canvas.lastFill();

        assertEquals(second, new ZioPaint.Bounds(fill.x1(), fill.y1(), fill.x2(), fill.y2()));
    }

    @Test
    void atTheRightEdgeBothFlipToTheSameSideOfThePointer() {
        RecordingCanvas canvas = new RecordingCanvas();
        String[] lines = {"a tooltip line long enough to run past the edge"};

        ZioPaint.Bounds bounds = ZioPaint.tooltipBounds(canvas, 320, 315, 40, lines);

        assertTrue(bounds.right() < 315, "it flips to the left of the pointer");
        assertEquals(314, bounds.right(), "one pixel short of the pointer");

        ZioPaint.tooltip(canvas, bounds, lines, 0x50101010, 0xFFFFFFFF);

        RecordingCanvas.Fill fill = canvas.fills().get(0);

        assertEquals(bounds, new ZioPaint.Bounds(fill.x1(), fill.y1(), fill.x2(), fill.y2()));
    }
}
