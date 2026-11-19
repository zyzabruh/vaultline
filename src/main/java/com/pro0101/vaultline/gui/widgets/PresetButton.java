package com.pro0101.vaultline.gui.widgets;

import com.pro0101.vaultline.gui.PresetTheme;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.WMeteorButton;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

/**
 * A flat button, in the shape the theme copies: one solid block of the theme colour with no border,
 * square by default, that gains a little opacity on hover and loses a little while pressed.
 *
 * <p>The text is drawn with its shadow, which is what makes small text readable on a translucent
 * block over the world.
 */
public class PresetButton extends WMeteorButton {
    private final PresetTheme preset;
    private final MeteorGuiTheme skin;

    public PresetButton(String text, GuiTexture texture, PresetTheme preset, MeteorGuiTheme skin) {
        super(text, texture);
        this.preset = preset;
        this.skin = skin;
        this.theme = skin;
    }

    @Override
    public GuiTheme getTheme() {
        return skin;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double radius = preset.cornerRadius();
        double pad = pad();

        SettingColor base = preset.buttonColor();
        int alpha = pressed ? 175 : mouseOver ? 150 : base.a;

        Color fill = new Color(base.r, base.g, base.b, alpha);

        // A hovered button gains a thin edge of the accent, so the pointer is visible on it even
        // when the fill under it is translucent.
        if (mouseOver || pressed) {
            SettingColor accent = preset.accentColor();
            Color edge = new Color(accent.r, accent.g, accent.b, pressed ? 230 : 160);
            Shapes.bordered(renderer, x, y, width, height, radius, skin.scale(1), edge, fill, fill);
        } else {
            Shapes.fill(renderer, x, y, width, height, radius, fill);
        }

        if (text != null) {
            renderer.text(text, x + width / 2 - textWidth / 2, y + pad, skin.textColor.get(), false);
        } else if (texture != null) {
            double ts = skin.textHeight();
            renderer.quad(x + width / 2 - ts / 2, y + pad, ts, ts, texture, skin.textColor.get());
        }
    }
}
