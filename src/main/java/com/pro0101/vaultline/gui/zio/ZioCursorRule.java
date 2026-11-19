package com.pro0101.vaultline.gui.zio;

/**
 * Which pointer shape a part of the copied screen asks for.
 *
 * <p>The rule is the one its components follow, read from them one by one: a text field or a key being
 * captured asks for the I-beam ({@code NumericSettingComponent}: {@code ClientScreen.cursor =
 * CursorType.INPUT;}, {@code StringSettingComponent} and {@code KeybindComponent} the same), every other
 * component the pointer can act on asks for the hand ({@code AbstractComponent}: {@code
 * ClientScreen.cursor = CursorType.POINTER;}, and the colour chips do the same), and anything else
 * leaves the arrow that the frame starts from.
 */
public final class ZioCursorRule {
    /** What the pointer is over, in the screen's own terms. */
    public enum Zone {
        NONE,
        PANEL_HEADER,
        MODULE_ROW,
        /** A setting's line, chip, slider or list choice. */
        SETTING_ROW,
        BUTTON,
        /** A field taking typed text, or a key being bound. */
        FIELD
    }

    private ZioCursorRule() {
    }

    public static ZioCursor cursorFor(Zone zone) {
        if (zone == null || zone == Zone.NONE) return ZioCursor.STANDARD;
        if (zone == Zone.FIELD) return ZioCursor.INPUT;

        return ZioCursor.POINTER;
    }
}
