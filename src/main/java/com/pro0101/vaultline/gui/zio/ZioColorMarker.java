package com.pro0101.vaultline.gui.zio;

/**
 * The colour picker's markers and bars, as the copied client draws and animates them.
 *
 * <p>{@code ColorSettingComponent} never jumps its markers: each one eases to the value at
 * {@code new InterpolatedFloat(0.35F, true)}, a third of the speed every other animation runs at. The
 * square's marker grows by a pixel while the square is held ({@code 3.0F + markerExpansion}), its border
 * turns black over a light colour ({@code markerContrast}, the same speed and uneased), and its inside is
 * the chosen colour darkened. The two sliders keep the accent border until they are held, then turn
 * white. The alpha bar is opaque on the left and clear on the right, over a light checkerboard.
 */
public final class ZioColorMarker {
    /** Half the square's marker at rest: {@code 3.0F}. */
    public static final int HALF = 3;

    /** The markers' own easing speed, {@code InterpolatedFloat(0.35F, true)}. */
    public static final float SPEED = 0.35f;

    /** The accent a resting slider marker keeps: {@code colorSetting} at full alpha. */
    public static final int ACCENT = 0xFFC794F4;

    public static final int WHITE = 0xFFFFFFFF;

    public static final int BLACK = 0xFF000000;

    private ZioColorMarker() {
    }

    /** How far the square's marker reaches: three pixels at rest, four while it is held. */
    public static int half(double expansion) {
        return HALF + (int) Math.round(expansion);
    }

    /** One frame of a marker gliding to its value, at the client's slow speed. */
    public static double slide(double current, double target, int fps) {
        return ZioAnim.approach(current, target, SPEED, fps);
    }

    /**
     * One frame of the border's own animation: {@code ClampedInterpolatedFloat(0.35F, false)}, the same
     * slow speed as the markers but without the ease, so the black border is chosen the moment the
     * colour crosses the halfway mark rather than sliding into it.
     */
    public static double contrast(double current, double target, int fps) {
        return ZioAnim.linear(current, target, SPEED, fps);
    }

    /** Black once a light colour has faded in, white otherwise. */
    public static int outline(double luminance, double contrast) {
        return contrast > 0.5 && luminance > 0.5 ? BLACK : WHITE;
    }

    /** The chosen colour, a tenth darker, inside the border. */
    public static int inner(int rgb) {
        java.awt.Color darker = new java.awt.Color(rgb & 0xFFFFFF).darker();

        return BLACK | (darker.getRGB() & 0xFFFFFF);
    }

    /** A slider's marker is white while it is held and the accent otherwise. */
    public static int sliderOutline(boolean dragging) {
        return dragging ? WHITE : ACCENT;
    }

    /** The alpha bar's marker: at its left end for a full alpha, at its right for a clear one. */
    public static double alphaMarker(int width, int alpha) {
        return Math.max(0, width - 1) * (1 - alpha / 255.0);
    }

    /** The alpha a column of the bar carries: opaque at the left, clear at the right. */
    public static int alphaAt(int width, int column) {
        if (width <= 1) return 255;

        double share = Math.min(1, Math.max(0, column / (double) (width - 1)));

        return (int) Math.round(255 * (1 - share));
    }

    /** The preview's transparency checkerboard: white or light grey, alternating. */
    public static int checker(boolean light) {
        return light ? 0xFFFFFFFF : 0xFFAAAAAA;
    }

    /** The alpha bar's own checkerboard, one shade darker than the preview's. */
    public static int alphaChecker(boolean light) {
        return light ? 0xFFFFFFFF : 0xFF999999;
    }

    /** The luminance the border is chosen from, on the client's own weights. */
    public static double luminance(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        return (0.2126 * red + 0.7152 * green + 0.0722 * blue) / 255.0;
    }
}
