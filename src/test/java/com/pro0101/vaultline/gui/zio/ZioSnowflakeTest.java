package com.pro0101.vaultline.gui.zio;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The drawn snowflake: six one-pixel spokes that grow with the flake's scale. */
class ZioSnowflakeTest {
    @Test
    void theStarHasSixArmsAroundItsCentre() {
        RecordingCanvas canvas = new RecordingCanvas();

        ZioPaint.snowflake(canvas, 40, 40, 3, 0xFFFFFFFF);

        List<RecordingCanvas.Fill> fills = canvas.fills();

        // Every stamp has its opposite across the centre: the star is symmetric by construction.
        for (RecordingCanvas.Fill fill : fills) {
            assertTrue(has(fills, 2 * 40 - fill.x1(), 2 * 40 - fill.y1()),
                "no arm opposite " + fill.x1() + "," + fill.y1());
        }

        assertEquals(6, spokes(fills, 40, 40, 3));
    }

    @Test
    void theArmsEndAtTheRadiusAlongEachSpoke() {
        RecordingCanvas canvas = new RecordingCanvas();

        ZioPaint.snowflake(canvas, 20, 20, 4, 0xFFFFFFFF);

        List<RecordingCanvas.Fill> fills = canvas.fills();

        // Six spokes at sixty degrees: the horizontal pair reaches the full radius, the four
        // diagonal ones two out and three up or down.
        assertTrue(has(fills, 16, 20));
        assertTrue(has(fills, 24, 20));
        assertTrue(has(fills, 22, 23));
        assertTrue(has(fills, 18, 17));
        assertTrue(has(fills, 22, 17));
        assertTrue(has(fills, 18, 23));

        for (RecordingCanvas.Fill fill : fills) {
            assertTrue(Math.abs(fill.x1() - 20) <= 4);
            assertTrue(Math.abs(fill.y1() - 20) <= 4);
        }
    }

    @Test
    void everyStampIsOnePixelWide() {
        RecordingCanvas canvas = new RecordingCanvas();

        ZioPaint.snowflake(canvas, 12, 8, 5, 0x80FFFFFF);

        assertFalse(canvas.fills().isEmpty());

        for (RecordingCanvas.Fill fill : canvas.fills()) {
            assertEquals(1, fill.x2() - fill.x1());
            assertEquals(1, fill.y2() - fill.y1());
            assertEquals(0x80FFFFFF, fill.argb());
        }
    }

    @Test
    void aLargerRadiusReachesFurtherOut() {
        assertEquals(2, ZioPaint.flakeRadius(0.5f));
        assertEquals(4, ZioPaint.flakeRadius(1f));
        assertEquals(5, ZioPaint.flakeRadius(1.15f));

        RecordingCanvas small = new RecordingCanvas();
        RecordingCanvas large = new RecordingCanvas();

        ZioPaint.snowflake(small, 30, 30, ZioPaint.flakeRadius(0.5f), 0xFFFFFFFF);
        ZioPaint.snowflake(large, 30, 30, ZioPaint.flakeRadius(1.15f), 0xFFFFFFFF);

        assertEquals(2, reach(small.fills()));
        assertEquals(5, reach(large.fills()));
    }

    @Test
    void theFlakesOnScreenAreDrawnNotTyped() {
        RecordingCanvas canvas = new RecordingCanvas();

        ZioPaint.snow(canvas, 320, 240, List.of(new ZioSnow.Flake(0.5, 0.5, 0.1f, 1f, 0)), 1);

        assertTrue(canvas.lines().isEmpty());
        assertFalse(canvas.fills().isEmpty());

        int centreX = 160;
        int centreY = Math.round(240 * 0.5f * 0.5f - 10 + ZioPaint.flakeRadius(1f));

        assertTrue(has(canvas.fills(), centreX, centreY));
        assertEquals(6, spokes(canvas.fills(), centreX, centreY, ZioPaint.flakeRadius(1f)));
    }

    @Test
    void aFlakeStillAboveTheTopEdgeDrawsNothing() {
        RecordingCanvas canvas = new RecordingCanvas();

        ZioPaint.snow(canvas, 320, 240, List.of(new ZioSnow.Flake(0.5, -0.4, 0.1f, 1f, 0)), 1);

        assertTrue(canvas.fills().isEmpty());
        assertTrue(canvas.lines().isEmpty());
    }

    private static boolean has(List<RecordingCanvas.Fill> fills, int x, int y) {
        for (RecordingCanvas.Fill fill : fills) {
            if (fill.x1() == x && fill.y1() == y) return true;
        }

        return false;
    }

    /** How many of the six spokes carry a stamp reaching at least a pixel short of the radius. */
    private static int spokes(List<RecordingCanvas.Fill> fills, int centreX, int centreY, int radius) {
        int spokes = 0;

        for (int spoke = 0; spoke < 6; spoke++) {
            double angle = Math.PI * spoke / 3;
            double furthest = 0;

            for (RecordingCanvas.Fill fill : fills) {
                double dx = fill.x1() - centreX;
                double dy = fill.y1() - centreY;

                furthest = Math.max(furthest, dx * Math.cos(angle) + dy * Math.sin(angle));
            }

            if (furthest >= radius - 1) spokes++;
        }

        return spokes;
    }

    /** The furthest a stamp sits horizontally or vertically from a centre of 30,30. */
    private static int reach(List<RecordingCanvas.Fill> fills) {
        int furthest = 0;

        for (RecordingCanvas.Fill fill : fills) {
            furthest = Math.max(furthest, Math.max(Math.abs(fill.x1() - 30), Math.abs(fill.y1() - 30)));
        }

        return furthest;
    }
}
