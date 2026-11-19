package com.pro0101.vaultline.gui.zio;

/**
 * What pressing a bound key does.
 *
 * <p>The copied client's {@code Keybind.Mode} has two values — {@code TOGGLE} and {@code HOLD} — and
 * cycles between them on a right click. Meteor's own {@code Keybind} has neither, so the mode lives
 * beside it in {@link ZioBindModes}. The default is {@code TOGGLE}, which is the behaviour this addon
 * has always had: one press switches the module on, the next switches it off.
 */
public enum ZioBindMode {
    TOGGLE("Toggle"),
    HOLD("Hold");

    private final String displayName;

    ZioBindMode(String displayName) {
        this.displayName = displayName;
    }

    /** How the copied client writes the mode: {@code Toggle}, {@code Hold}. */
    public String displayName() {
        return displayName;
    }

    /** The next mode, in the copied client's own order. */
    public ZioBindMode next() {
        ZioBindMode[] modes = values();

        return modes[(ordinal() + 1) % modes.length];
    }

    /** A stored name back to its mode, and the default for anything that is not one. */
    public static ZioBindMode parse(String name) {
        for (ZioBindMode mode : values()) {
            if (mode.name().equalsIgnoreCase(name)) return mode;
        }

        return ZioBindMode.TOGGLE;
    }
}
