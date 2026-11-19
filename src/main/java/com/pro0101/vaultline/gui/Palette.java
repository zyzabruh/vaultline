package com.pro0101.vaultline.gui;

import meteordevelopment.meteorclient.utils.render.color.SettingColor;

/**
 * The colours a theme is built from. Everything else (hovered and pressed states, title text,
 * separators, scrollbars) is derived here, so a theme is defined by six colours and a
 * light/dark flag instead of twenty settings that would drift apart.
 */
public record Palette(boolean light, SettingColor accent, SettingColor accentAlt, SettingColor surface,
                      SettingColor panel, SettingColor outline, SettingColor text, SettingColor textSecondary) {
}
