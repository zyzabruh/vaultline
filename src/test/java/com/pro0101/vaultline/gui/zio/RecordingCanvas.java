package com.pro0101.vaultline.gui.zio;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link ZioCanvas} that writes nothing and remembers everything.
 *
 * <p>This is how the paint routines are checked: the test drives them with fixed metrics and reads
 * back exactly the calls that would have reached the renderer.
 */
final class RecordingCanvas implements ZioCanvas {
    /** A filled rectangle as it was submitted. */
    record Fill(int x1, int y1, int x2, int y2, int argb, int gradientTo) {
    }

    /** A line of text as it was submitted. */
    record Line(String value, float x, float y, int argb, float scale) {
    }

    private final List<Fill> fills = new ArrayList<>();
    private final List<Line> lines = new ArrayList<>();

    private final float characterWidth;
    private final int lineHeight;

    RecordingCanvas() {
        this(5f, 8);
    }

    RecordingCanvas(float characterWidth, int lineHeight) {
        this.characterWidth = characterWidth;
        this.lineHeight = lineHeight;
    }

    List<Fill> fills() {
        return fills;
    }

    List<Line> lines() {
        return lines;
    }

    Fill lastFill() {
        return fills.get(fills.size() - 1);
    }

    Line lastLine() {
        return lines.get(lines.size() - 1);
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int argb) {
        fills.add(new Fill(x1, y1, x2, y2, argb, argb));
    }

    @Override
    public void fillGradient(int x1, int y1, int x2, int y2, int topArgb, int bottomArgb) {
        fills.add(new Fill(x1, y1, x2, y2, topArgb, bottomArgb));
    }

    @Override
    public void text(String value, float x, float y, int argb) {
        lines.add(new Line(value, x, y, argb, 1f));
    }

    @Override
    public void textScaled(String value, float x, float y, int argb, float scale) {
        lines.add(new Line(value, x, y, argb, scale));
    }

    @Override
    public float textWidth(String value) {
        return value.length() * characterWidth;
    }

    @Override
    public int lineHeight() {
        return lineHeight;
    }
}
