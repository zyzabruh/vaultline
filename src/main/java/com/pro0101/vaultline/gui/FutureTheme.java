package com.pro0101.vaultline.gui;

import meteordevelopment.meteorclient.utils.render.color.SettingColor;

/**
 * The addon's own theme: rounded, glowing panels in cyan and violet on a dark sheet.
 *
 * <p>Where {@link ZioTheme} copies another client's flat lavender interface, this one keeps the
 * shape the addon started with — a rounded window with a halo, a title bar that fades from the
 * accent to the second accent, and a body dark enough to read over any world. Every colour below is
 * a setting, so the palette can be changed without touching the code.
 */
public class FutureTheme extends PresetTheme {
    public FutureTheme() {
        super("Future", new Palette(
            false,
            new SettingColor(58, 208, 255),        // accent, #3AD0FF
            new SettingColor(122, 107, 255),       // accent-alt, #7A6BFF
            new SettingColor(16, 22, 38, 190),     // surface, the body of a window
            new SettingColor(58, 208, 255, 45),    // panel, a module that is switched on
            new SettingColor(58, 208, 255, 120),   // outline
            new SettingColor(232, 240, 255),       // text
            new SettingColor(150, 172, 200)        // text-secondary
        ), new Style(
            0,    // corner-radius
            34,   // panel-glow
            0,    // body-shade: the surface is already dark
            0,    // header-height: Meteor's own bar
            0,    // window width: as wide as the content
            true, // gradient header
            true, // sounds
            true, // background blur
            false // ported sounds: Meteor's own picks here
        ));
    }
}
