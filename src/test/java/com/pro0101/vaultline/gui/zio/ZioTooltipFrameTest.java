package com.pro0101.vaultline.gui.zio;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The order a frame decides its tooltip in: the panels are stepped first, and only then is the row
 * under the pointer read. The copied client never reads a row before its panels have updated, and
 * reading it the other way round made the ported text lag one frame behind its blurred rectangle.
 */
class ZioTooltipFrameTest {
    /**
     * A panel that opens or closes over its frames, and has nothing to hover while its body is under
     * half open — the copied client's own gate, as {@code Panel.rowAt} applies it.
     */
    private static final class Panel implements ZioTooltipFrame.Panels {
        private final List<String> order = new ArrayList<>();
        private final boolean opening;
        private double expansion;

        Panel(double expansion, boolean opening) {
            this.expansion = expansion;
            this.opening = opening;
        }

        @Override
        public void step() {
            order.add("step");
            expansion = ZioAnim.approach(expansion, opening ? 1 : 0, 2.0, 20);
        }

        @Override
        public String descriptionAt(double x, double y) {
            order.add("read");

            return expansion >= 0.5 ? "the row under the pointer" : null;
        }
    }

    @Test
    void theLineIsReadOnlyAfterThePanelsHaveStepped() {
        Panel panel = new Panel(1, true);

        ZioTooltipFrame.resolve(panel, new RecordingCanvas(), 320, 10, 10, 10, 10);

        assertEquals(List.of("step", "read"), panel.order);
    }

    @Test
    void anOpeningPanelHasItsTooltipOnTheFrameThatOpensIt() {
        Panel panel = new Panel(0.49, true);

        // The order the screen used before: the row was read first, the body was still under half
        // open, so this frame had no tooltip and no blurred rectangle — while the next frame had both,
        // which is exactly the one-image lag.
        assertNull(panel.descriptionAt(0, 0), "read before the step: nothing to show yet");

        panel.order.clear();

        // The screen's order: step, then read. The same frame now has its row.
        ZioTooltipFrame.Resolved resolved = ZioTooltipFrame.resolve(panel, new RecordingCanvas(), 320, 10, 10, 10, 10);

        assertNotNull(resolved, "stepped first, the frame that opens the panel already has its line");
        assertEquals("the row under the pointer", resolved.lines()[0]);
        assertEquals(List.of("step", "read"), panel.order);
    }

    @Test
    void aClosingPanelDropsItsTooltipOnTheFrameItCloses() {
        Panel panel = new Panel(0.52, false);

        // Read first, as the old order did: the body was still over half open, so a tooltip was drawn
        // and its rectangle blurred for one frame after the panel had already started closing.
        assertNotNull(panel.descriptionAt(0, 0), "read before the step: the old order still showed a row");

        panel.order.clear();

        ZioTooltipFrame.Resolved resolved = ZioTooltipFrame.resolve(panel, new RecordingCanvas(), 320, 10, 10, 10, 10);

        assertNull(resolved, "stepped first, the body is under half open and there is nothing to blur or draw");
        assertEquals(List.of("step", "read"), panel.order);
    }

    @Test
    void theRectangleIsTheOneTheLinesOfThatSameFrameFill() {
        RecordingCanvas canvas = new RecordingCanvas();
        Panel panel = new Panel(1, true);

        ZioTooltipFrame.Resolved resolved = ZioTooltipFrame.resolve(panel, canvas, 320, 40, 60, 40, 60);

        assertNotNull(resolved);

        ZioPaint.tooltip(canvas, resolved.bounds(), resolved.lines(), 0x50101010, 0xFFFFFFFF);

        RecordingCanvas.Fill fill = canvas.fills().get(0);

        assertEquals(resolved.bounds(), new ZioPaint.Bounds(fill.x1(), fill.y1(), fill.x2(), fill.y2()));
        assertEquals(1, resolved.lines().length);
    }

    @Test
    void aPointerOverNothingHasNeitherLineNorRectangle() {
        RecordingCanvas canvas = new RecordingCanvas();

        assertNull(ZioTooltipFrame.resolve(new Panel(0, false), canvas, 320, 10, 10, 10, 10));
        assertTrue(canvas.fills().isEmpty(), "nothing is drawn, and the blur pass gets no rectangle");
    }
}
