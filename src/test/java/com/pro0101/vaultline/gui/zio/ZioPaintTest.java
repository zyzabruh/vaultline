package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the screen submits to the renderer for the shapes pulled out of the copied client: the
 * tooltip, the search and its hints, the row flash, the bind status and the game button.
 */
class ZioPaintTest {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREY = 0xFF808080;
    private static final int TOOLTIP = 0x500A0A0A;
    private static final int TEXT = 0xFFE1E1E1;

    @Test
    void theTooltipPadsItsFirstLineOnePixelsInAndStartsAboveThePointer() {
        RecordingCanvas canvas = new RecordingCanvas();

        ZioPaint.Bounds bounds = ZioPaint.tooltip(canvas, 320, 100, 100, new String[]{"First", "Second line"}, TOOLTIP,
            WHITE);

        // Nine to the right of the pointer, as wide as the widest line plus the one pixel of padding.
        assertEquals(109, bounds.left());
        assertEquals(109 + 11 * 5 + 1, bounds.right());
        // One pixel above the pointer, and a line height plus one per line.
        assertEquals(99, bounds.top());
        assertEquals(99 + 9 * 2, bounds.bottom());

        RecordingCanvas.Fill background = canvas.lastFill();
        assertEquals(TOOLTIP, background.argb());
        assertEquals(bounds.left(), background.x1());
        assertEquals(bounds.top(), background.y1());

        List<RecordingCanvas.Line> lines = canvas.lines();
        assertEquals(2, lines.size());
        // The padding is on the inside: the text starts one pixel in and one pixel down.
        assertEquals(bounds.left() + 1, lines.get(0).x());
        assertEquals(bounds.top() + 1, lines.get(0).y());
        assertEquals(bounds.left() + 1, lines.get(1).x());
        assertEquals(bounds.top() + 1 + 9, lines.get(1).y());
        assertEquals("First", lines.get(0).value());
        assertEquals("Second line", lines.get(1).value());
        assertEquals(WHITE, lines.get(0).argb());
    }

    @Test
    void theTooltipFlipsToTheLeftOfThePointerAtTheScreenEdge() {
        RecordingCanvas canvas = new RecordingCanvas();

        ZioPaint.Bounds bounds = ZioPaint.tooltip(canvas, 320, 315, 40, new String[]{"Long tooltip line"}, WHITE, WHITE);

        // Pointer minus the text and two, as ClientScreen.render lays it out.
        assertEquals(315 - 17 * 5 - 2, bounds.left());
        assertEquals(315 - 1, bounds.right());
        assertEquals(bounds.right(), canvas.lastFill().x2());
    }

    @Test
    void theSearchDimsTheWholeScreenByAFifth() {
        RecordingCanvas canvas = new RecordingCanvas();

        ZioPaint.searchDimmer(canvas, 320, 240, 1, 1);

        RecordingCanvas.Fill dim = canvas.lastFill();
        assertEquals(0, dim.x1());
        assertEquals(0, dim.y1());
        assertEquals(320, dim.x2());
        assertEquals(240, dim.y2());
        assertEquals(Math.round(0.2 * 255) << 24, dim.argb());
        // Nothing is drawn while the search has faded out.
        canvas.fills().clear();
        ZioPaint.searchDimmer(canvas, 320, 240, 0.005, 1);
        assertTrue(canvas.fills().isEmpty());
    }

    @Test
    void theSearchIsCentredOnTheScreenWithItsCompletionInGrey() {
        RecordingCanvas canvas = new RecordingCanvas();

        ZioPaint.searchQuery(canvas, 320, 240, "kil", "lAura", true, WHITE, GREY);

        // The whole "killAura" plus the caret is centred, so the query starts half of it left of centre.
        float total = "killAura_".length() * 5f;
        float expectedX = 320 / 2f - total / 2f;

        List<RecordingCanvas.Line> lines = canvas.lines();
        assertEquals(3, lines.size());

        assertEquals("kil", lines.get(0).value());
        assertEquals(expectedX, lines.get(0).x());
        assertEquals(240 / 2f - 8 / 2f, lines.get(0).y());
        assertEquals(WHITE, lines.get(0).argb());

        assertEquals("lAura", lines.get(1).value());
        assertEquals(expectedX + 15, lines.get(1).x());
        assertEquals(GREY, lines.get(1).argb());

        assertEquals("_", lines.get(2).value());
        assertEquals(expectedX + 15 + 25, lines.get(2).x());
        assertEquals(WHITE, lines.get(2).argb());
    }

    @Test
    void theCaretAndCompletionOnlyAppearWhenThereIsOne() {
        RecordingCanvas canvas = new RecordingCanvas();

        ZioPaint.searchQuery(canvas, 320, 240, "", "", false, WHITE, GREY);

        assertEquals(1, canvas.lines().size());
        assertEquals("", canvas.lines().get(0).value());
    }

    @Test
    void theHintSitsInTheCornerClearOfTheHudMargin() {
        RecordingCanvas canvas = new RecordingCanvas();
        String hint = "Ctrl + F to activate search";

        ZioPaint.searchHint(canvas, 320, 240, hint, 3, WHITE);

        RecordingCanvas.Line line = canvas.lastLine();
        assertEquals(hint, line.value());
        assertEquals(320 - hint.length() * 5f - 3, line.x());
        assertEquals(240 - 8 - 3, line.y());
        assertTrue(canvas.fills().isEmpty());
    }

    @Test
    void theRowFlashIsRedAndFadesWithTheBlink() {
        RecordingCanvas canvas = new RecordingCanvas();

        ZioPaint.highlight(canvas, 11, 20, 98, 32, 1, 1);
        assertEquals(0xFFFF0000, canvas.lastFill().argb());
        assertEquals(11, canvas.lastFill().x1());
        assertEquals(98, canvas.lastFill().x2());

        ZioPaint.highlight(canvas, 11, 20, 98, 32, 0.5, 1);
        assertEquals(0x80FF0000, canvas.lastFill().argb());

        // A blink at its lowest is fully transparent but still the same red.
        ZioPaint.highlight(canvas, 11, 20, 98, 32, 0, 1);
        assertEquals(0x00FF0000, canvas.lastFill().argb());
    }

    @Test
    void theGameButtonIsItsLabelThreePixelsIn() {
        RecordingCanvas canvas = new RecordingCanvas();

        ZioPaint.gameButton(canvas, 210, 63.5f, "Tetris", WHITE);

        RecordingCanvas.Line line = canvas.lastLine();
        assertEquals("Tetris", line.value());
        assertEquals(213, line.x());
        assertEquals(63.5f, line.y());
        // A plain line of text: the copied client's button paints no background of its own.
        assertTrue(canvas.fills().isEmpty());
    }

    @Test
    void anEnumerationIsBackedByTheAccentBarTwoPixelsIn() {
        RecordingCanvas canvas = new RecordingCanvas();

        ZioPaint.enumBar(canvas, 10, 40, 13, 99, 1, 0x82C794F4);

        RecordingCanvas.Fill bar = canvas.lastFill();
        assertEquals(12, bar.x1());
        assertEquals(10 + 98, bar.x2());
        assertEquals(40, bar.y1());
        assertEquals(40 + 12, bar.y2());
        assertEquals(0x82C794F4, bar.argb());

        // While it comes up, the bar is as wide as its share, never narrower than two pixels.
        ZioPaint.enumBar(canvas, 10, 40, 13, 99, 0.5, 0x82C794F4);
        assertEquals(10 + 49, canvas.lastFill().x2());

        ZioPaint.enumBar(canvas, 10, 40, 13, 99, 0, 0x82C794F4);
        assertEquals(12, canvas.lastFill().x2());
    }

    @Test
    void theBindRowIsDrawnFourPixelsInWithItsMode() {
        RecordingCanvas canvas = new RecordingCanvas();

        // The default mode, no pointer on it: the copied client writes "Key" and the key.
        ZioPaint.bindRow(canvas, 10, 21.5f, ZioBindMode.TOGGLE, "B", false, false, TEXT);
        assertEquals("Key B", canvas.lastLine().value());
        assertEquals(14, canvas.lastLine().x());
        assertEquals(21.5f, canvas.lastLine().y());
        assertEquals(TEXT, canvas.lastLine().argb());
        assertEquals(1f, canvas.lastLine().scale());

        // Hovered, the mode names itself.
        ZioPaint.bindRow(canvas, 10, 21.5f, ZioBindMode.TOGGLE, "B", false, true, TEXT);
        assertEquals("Toggle B", canvas.lastLine().value());

        // Hold always says so, hovered or not.
        ZioPaint.bindRow(canvas, 10, 21.5f, ZioBindMode.HOLD, "B", false, false, TEXT);
        assertEquals("Hold B", canvas.lastLine().value());

        // Waiting for a key, the key itself becomes an ellipsis.
        ZioPaint.bindRow(canvas, 10, 21.5f, ZioBindMode.TOGGLE, "B", true, false, TEXT);
        assertEquals("Key ...", canvas.lastLine().value());
    }

    @Test
    void theSettingBindStatusIsBracketedHalfSizedAfterTheName() {
        RecordingCanvas canvas = new RecordingCanvas();

        ZioPaint.bindStatus(canvas, 27, 12, ZioBindMode.HOLD, "B", false, TEXT);

        RecordingCanvas.Line status = canvas.lastLine();
        assertEquals("[HOLD B]", status.value());
        assertEquals(27, status.x());
        assertEquals(12, status.y());
        assertEquals(TEXT, status.argb());
        assertEquals(0.5f, status.scale());

        // While the setting waits for its key, the ellipsis is what it reads.
        ZioPaint.bindStatus(canvas, 27, 12, ZioBindMode.TOGGLE, "B", true, TEXT);
        assertEquals("[TOGGLE ...]", canvas.lastLine().value());
        assertEquals("[HOLD B]", ZioPaint.bindStatusLabel(ZioBindMode.HOLD, "B", false));
    }

    @Test
    void theModuleStatusIsBracketedAndHalfSized() {
        RecordingCanvas canvas = new RecordingCanvas();

        ZioPaint.status(canvas, 40, 7, "shift", TEXT);

        RecordingCanvas.Line status = canvas.lastLine();
        assertEquals("[SHIFT]", status.value());
        assertEquals(40, status.x());
        assertEquals(7, status.y());
        assertEquals(TEXT, status.argb());
        assertEquals(0.5f, status.scale());
        assertEquals("[SHIFT]", ZioPaint.statusLabel("shift"));
    }

    @Test
    void aSettingDescriptionWinsOverItsModules() {
        assertEquals("leaves a trail", ZioPaint.descriptionToShow("leaves a trail", "trails behind you"));
        assertEquals("trails behind you", ZioPaint.descriptionToShow("", "trails behind you"));
        assertEquals("trails behind you", ZioPaint.descriptionToShow(null, "trails behind you"));
        assertEquals(null, ZioPaint.descriptionToShow(null, null));
        assertEquals(null, ZioPaint.descriptionToShow("", ""));
    }

    @Test
    void aSnowflakeIsDrawnHalfTheScreenDeepAndFadesAsItFalls() {
        RecordingCanvas canvas = new RecordingCanvas();

        // One flake just inside the top edge, one halfway down, one still above the edge.
        List<ZioSnow.Flake> flakes = List.of(
            new ZioSnow.Flake(0.5, 0.1, 0.1f, 1f, 0),
            new ZioSnow.Flake(0.25, 0.5, 0.1f, 1.15f, 0),
            new ZioSnow.Flake(0.75, -0.4, 0.1f, 0.5f, 0));

        ZioPaint.snow(canvas, 320, 240, flakes, 1);

        // Each flake is a drawn six-armed star, not a typed glyph.
        assertTrue(canvas.lines().isEmpty());
        assertTrue(canvas.fills().size() > 0);

        // A tenth down the screen's depth, at its start across the width, fading from full: its
        // centre sits ten pixels above that, so its stamps stay between two and ten.
        for (RecordingCanvas.Fill fill : canvas.fills()) {
            if (fill.argb() == 0xE6FFFFFF) {
                assertTrue(fill.x1() >= 156 && fill.x1() <= 164);
                assertTrue(fill.y1() >= 2 && fill.y1() <= 10);
            } else {
                // Halfway down, at half opacity, larger: five pixels around its centre at 55.
                assertEquals(0x80FFFFFF, fill.argb());
                assertTrue(fill.x1() >= 75 && fill.x1() <= 85);
                assertTrue(fill.y1() >= 50 && fill.y1() <= 60);
            }
        }
    }

    @Test
    void aFadeScalesTheAlphaAndKeepsTheColour() {
        assertEquals(0x80ABCDEF, ZioPaint.fade(0xFFABCDEF, 0.502));
        assertEquals(0x00ABCDEF, ZioPaint.fade(0xFFABCDEF, 0));
        assertEquals(0xFFABCDEF, ZioPaint.fade(0xFFABCDEF, 4));
    }
}
