package com.pro0101.vaultline.gui.widgets;

import com.pro0101.vaultline.gui.PresetTheme;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorWindow;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

/**
 * A Meteor window drawn as a rounded, glowing panel: a soft halo outside, a one pixel border, a
 * vertical gradient body and a gradient title bar with rounded top corners.
 */
public class PresetWindow extends WMeteorWindow {
    private static final int BODY_SHADE_R = 12;
    private static final int BODY_SHADE_G = 10;
    private static final int BODY_SHADE_B = 18;

    private final PresetTheme preset;
    private final MeteorGuiTheme skin;

    public PresetWindow(WWidget icon, String title, PresetTheme preset, MeteorGuiTheme skin) {
        super(icon, title);
        this.preset = preset;
        this.skin = skin;
        this.theme = skin;
    }

    @Override
    public GuiTheme getTheme() {
        return skin;
    }

    @Override
    public void init() {
        // The container hands its own theme to every child it adds; the title bar and the view
        // built below must be themed with the skin instead, so that their casts keep working.
        theme = skin;
        super.init();

        // The first cell is the header, and the header is what sets the width of the window: a
        // minimum here widens every window of a theme that asks for a fixed width.
        double minWidth = preset.minWindowWidth();
        if (minWidth > 0 && !cells.isEmpty()) cells.get(0).minWidth(skin.scale(minWidth));
    }

    @Override
    protected WHeader header(WWidget icon) {
        return new Header(icon);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (!expanded && animProgress <= 0) return;

        double radius = preset.cornerRadius();
        double line = skin.scale(1);

        Color surface = preset.bodyColor();
        Color deep = preset.bodyBottomColor();

        // A dark halo under the panel, so it sits above the world instead of floating on it.
        Shapes.glow(renderer, x, y + skin.scale(2), width, height, radius,
            new Color(0, 0, 0, 110), 3);

        int glow = preset.panelGlow();
        if (glow > 0) {
            SettingColor accent = preset.accentColor();
            Shapes.glow(renderer, x, y, width, height, radius,
                new Color(accent.r, accent.g, accent.b, glow), 3);
        }

        // The title bar covers the top of this, so only the sides and the bottom edge of the
        // border need to be drawn here.
        double top = y + header.height - radius;
        double bodyHeight = y + height - top;
        if (bodyHeight <= 0) return;

        // The body is a tenth of the accent as in the copied theme, which over a bright world is
        // close to white: a dark glass is drawn under it so the labels and the fields keep a
        // background of their own. Setting the shade to zero gives the plain translucent sheet.
        int shade = preset.bodyShade();

        if (shade > 0) {
            Shapes.fill(renderer, x, top, width, bodyHeight, radius,
                new Color(BODY_SHADE_R, BODY_SHADE_G, BODY_SHADE_B, shade));
        }

        Shapes.bordered(renderer, x, top, width, bodyHeight, radius, line,
            skin.outlineColor.get(), surface, deep);
    }

    /** Title bar: one solid block of the theme colour, square by default. */
    private class Header extends WHeader {
        Header(WWidget icon) {
            super(icon);
        }

        @Override
        public void init() {
            super.init();

            // Meteor pads the title by four pixels above and below, which makes a seventeen pixel
            // bar. A theme with a fixed bar trims the padding to two and holds the height there.
            double bar = preset.headerHeight();
            if (bar <= 0) return;

            for (var cell : cells) cell.padVertical(skin.scale(2));
            height = skin.scale(bar);
        }

        @Override
        protected void onCalculateSize() {
            super.onCalculateSize();

            // Meteor derives the bar from the text it holds, which lands a pixel or two off the
            // height a fixed bar wants; such a bar is a constant whatever the title is.
            double bar = preset.headerHeight();
            if (bar > 0) height = skin.scale(bar);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            double radius = preset.cornerRadius();
            double line = skin.scale(1);

            // A gradient theme fades the bar from the accent into the second accent; the flat one
            // uses a single block of colour.
            SettingColor bar = preset.headerColor();
            Color top = preset.gradientHeader() ? preset.accentColor() : bar;
            Color bottom = preset.gradientHeader() ? preset.accentAltColor() : bar;

            Shapes.fill(renderer, x, y, width, height + radius, radius, top, bottom);

            // Square off the bottom of the bar, which would otherwise be rounded by the shape above.
            if (radius > 0) {
                renderer.quad(x, y + radius, width, height - radius, bar);
            }
        }
    }
}
