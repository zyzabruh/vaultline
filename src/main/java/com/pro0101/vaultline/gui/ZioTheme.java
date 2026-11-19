package com.pro0101.vaultline.gui;

import meteordevelopment.meteorclient.utils.render.color.SettingColor;

/**
 * The zio client's ClickGUI, colour for colour.
 *
 * <p>Everything is one lavender, {@code #C794F4}, at a different opacity: a window's title bar is
 * that colour at half alpha, its body at a tenth, a button at forty five percent and a module that
 * is switched on at thirty five. Text is near-white, {@code #E1E1E1}, and turns pure white on an
 * active module. Corners are square and there is no halo; windows are 99 pixels wide with a
 * fourteen pixel title bar.
 */
public class ZioTheme extends PresetTheme {
    public ZioTheme() {
        super("Zio", new Palette(
            false,
            new SettingColor(199, 148, 244),       // accent, #C794F4
            new SettingColor(199, 148, 244),       // accent-alt: zio has no second colour
            new SettingColor(199, 148, 244, 30),   // surface, the body of a window
            new SettingColor(199, 148, 244, 90),   // panel, a module that is switched on
            new SettingColor(199, 148, 244, 130),  // outline and title bar
            new SettingColor(225, 225, 225),       // text, #E1E1E1
            new SettingColor(225, 225, 225, 170)   // text-secondary
        ), new Style(
            0,    // corner-radius
            0,    // panel-glow
            0,    // body-shade: the blurred background does the work
            14,   // header-height
            99,   // window width
            false,// gradient header
            true, // sounds
            true, // background blur
            true  // ported sounds: the copied client's own files
        ));
    }
}
