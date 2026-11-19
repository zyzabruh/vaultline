package com.pro0101.vaultline.gui.widgets;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import org.lwjgl.glfw.GLFW;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorAccount;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorHorizontalSeparator;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorLabel;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorMultiLabel;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorSection;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorTooltip;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorTopBar;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorVerticalSeparator;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorView;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.input.WMeteorDropdown;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.input.WMeteorSlider;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.input.WMeteorTextBox;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.WMeteorCheckbox;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.WMeteorConfirmedButton;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.WMeteorConfirmedMinus;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.WMeteorFavorite;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.WMeteorMinus;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.WMeteorPlus;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.WMeteorTriangle;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.modules.Module;

/**
 * The widgets a {@link com.pro0101.vaultline.gui.PresetTheme} hands out.
 *
 * <p>Every one of them extends Meteor's own widget class so the drawing code is Meteor's, but
 * reports the theme's skin (a real {@link MeteorGuiTheme}) from {@code getTheme()}. That cast is
 * what Meteor's widgets do internally, and the GUI container assigns its own theme to each child
 * it adds — which is the preset theme, not the skin — so {@code getTheme()} has to answer with the
 * skin rather than with the field. {@code init()} puts the skin back on the field too, because
 * windows, sections, dropdowns and text boxes build their inner widgets from that field.
 */
public final class PresetWidgets {
    private PresetWidgets() {
    }

    public static class Label extends WMeteorLabel {
        private final MeteorGuiTheme skin;

        public Label(String text, boolean title, MeteorGuiTheme skin) {
            super(text, title);
            this.skin = skin;
            this.theme = skin;
        }

        @Override
        public GuiTheme getTheme() {
            return skin;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            if (text.isEmpty()) return;

            // The flag is title size, not shadow: Meteor's GUI text has no shadow at all, and a
            // title is measured a quarter wider than a normal line, so it is drawn as one only
            // when it is one.
            renderer.text(text, x, y,
                color != null ? color : (title ? skin.titleTextColor.get() : skin.textColor.get()),
                title);
        }
    }

    public static class MultiLabel extends WMeteorMultiLabel {
        private final MeteorGuiTheme skin;

        public MultiLabel(String text, boolean title, double maxWidth, MeteorGuiTheme skin) {
            super(text, title, maxWidth);
            this.skin = skin;
            this.theme = skin;
        }

        @Override
        public GuiTheme getTheme() {
            return skin;
        }
    }

    public static class HorizontalSeparator extends WMeteorHorizontalSeparator {
        private final MeteorGuiTheme skin;

        public HorizontalSeparator(String text, MeteorGuiTheme skin) {
            super(text);
            this.skin = skin;
            this.theme = skin;
        }

        @Override
        public GuiTheme getTheme() {
            return skin;
        }
    }

    public static class VerticalSeparator extends WMeteorVerticalSeparator {
        private final MeteorGuiTheme skin;

        public VerticalSeparator(MeteorGuiTheme skin) {
            this.skin = skin;
            this.theme = skin;
        }

        @Override
        public GuiTheme getTheme() {
            return skin;
        }
    }

    public static class Minus extends WMeteorMinus {
        private final MeteorGuiTheme skin;

        public Minus(MeteorGuiTheme skin) {
            this.skin = skin;
            this.theme = skin;
        }

        @Override
        public GuiTheme getTheme() {
            return skin;
        }
    }

    public static class ConfirmedMinus extends WMeteorConfirmedMinus {
        private final MeteorGuiTheme skin;

        public ConfirmedMinus(MeteorGuiTheme skin) {
            this.skin = skin;
            this.theme = skin;
        }

        @Override
        public GuiTheme getTheme() {
            return skin;
        }
    }

    public static class Plus extends WMeteorPlus {
        private final MeteorGuiTheme skin;

        public Plus(MeteorGuiTheme skin) {
            this.skin = skin;
            this.theme = skin;
        }

        @Override
        public GuiTheme getTheme() {
            return skin;
        }
    }

    public static class Checkbox extends WMeteorCheckbox {
        private final MeteorGuiTheme skin;

        public Checkbox(boolean checked, MeteorGuiTheme skin) {
            super(checked);
            this.skin = skin;
            this.theme = skin;
        }

        @Override
        public GuiTheme getTheme() {
            return skin;
        }
    }

    public static class Favorite extends WMeteorFavorite {
        private final MeteorGuiTheme skin;

        public Favorite(boolean checked, MeteorGuiTheme skin) {
            super(checked);
            this.skin = skin;
            this.theme = skin;
        }

        @Override
        public GuiTheme getTheme() {
            return skin;
        }
    }

    public static class ConfirmedButton extends WMeteorConfirmedButton {
        private final MeteorGuiTheme skin;

        public ConfirmedButton(String text, String confirmText, GuiTexture texture, MeteorGuiTheme skin) {
            super(text, confirmText, texture);
            this.skin = skin;
            this.theme = skin;
        }

        @Override
        public GuiTheme getTheme() {
            return skin;
        }
    }

    public static class Slider extends WMeteorSlider {
        private final MeteorGuiTheme skin;

        public Slider(double value, double min, double max, MeteorGuiTheme skin) {
            super(value, min, max);
            this.skin = skin;
            this.theme = skin;
        }

        @Override
        public GuiTheme getTheme() {
            return skin;
        }
    }

    public static class TextBox extends WMeteorTextBox {
        private final MeteorGuiTheme skin;

        public TextBox(String text, String placeholder, CharFilter filter, Class<? extends WTextBox.Renderer> renderer, MeteorGuiTheme skin) {
            super(text, placeholder, filter, renderer);
            this.skin = skin;
            this.theme = skin;
        }

        @Override
        public GuiTheme getTheme() {
            return skin;
        }

        @Override
        public void init() {
            theme = skin;
            super.init();
        }
    }

    public static class Dropdown<T> extends WMeteorDropdown<T> {
        private final MeteorGuiTheme skin;

        public Dropdown(T[] values, T value, MeteorGuiTheme skin) {
            super(values, value);
            this.skin = skin;
            this.theme = skin;
        }

        @Override
        public GuiTheme getTheme() {
            return skin;
        }

        @Override
        public void init() {
            theme = skin;
            super.init();
        }
    }

    public static class Triangle extends WMeteorTriangle {
        private final MeteorGuiTheme skin;

        public Triangle(MeteorGuiTheme skin) {
            this.skin = skin;
            this.theme = skin;
        }

        @Override
        public GuiTheme getTheme() {
            return skin;
        }
    }

    public static class Tooltip extends WMeteorTooltip {
        private final MeteorGuiTheme skin;

        public Tooltip(String text, MeteorGuiTheme skin) {
            super(text);
            this.skin = skin;
            this.theme = skin;
        }

        @Override
        public GuiTheme getTheme() {
            return skin;
        }
    }

    public static class View extends WMeteorView {
        private final MeteorGuiTheme skin;

        public View(MeteorGuiTheme skin) {
            this.skin = skin;
            this.theme = skin;
        }

        @Override
        public GuiTheme getTheme() {
            return skin;
        }
    }

    public static class Section extends WMeteorSection {
        private final MeteorGuiTheme skin;

        public Section(String title, boolean expanded, WWidget headerWidget, MeteorGuiTheme skin) {
            super(title, expanded, headerWidget);
            this.skin = skin;
            this.theme = skin;
        }

        @Override
        public GuiTheme getTheme() {
            return skin;
        }

        @Override
        public void init() {
            theme = skin;
            super.init();
        }
    }

    public static class AccountWidget extends WMeteorAccount {
        private final MeteorGuiTheme skin;

        public AccountWidget(WidgetScreen screen, Account<?> account, MeteorGuiTheme skin) {
            super(screen, account);
            this.skin = skin;
            this.theme = skin;
        }

        @Override
        public GuiTheme getTheme() {
            return skin;
        }
    }

    /**
     * A module row: a fill that sweeps across the width while the module switches on, a tint that
     * follows the pointer, and the title on top.
     *
     * <p>The sweep is the bar the copied interface draws: as wide as the row times how far the
     * switch has animated, so a row being turned on fills up and one being turned off empties. A
     * row under the pointer is tinted with the accent whether it is on or off, which is the only
     * hover feedback a flat theme has room for. The title is white while the module runs and the
     * theme's text colour otherwise.
     */
    public static class ModuleWidget extends WPressable {
        private final Module module;
        private final String title;
        private final MeteorGuiTheme skin;
        private double titleWidth;
        private double fill;
        private double hover;

        public ModuleWidget(Module module, String title, MeteorGuiTheme skin) {
            this.module = module;
            this.title = title;
            this.skin = skin;
            this.theme = skin;
            this.tooltip = module.description;
            this.fill = module.isActive() ? 1 : 0;
        }

        @Override
        public GuiTheme getTheme() {
            return skin;
        }

        @Override
        public double pad() {
            return skin.scale(4);
        }

        @Override
        protected void onCalculateSize() {
            double pad = pad();

            if (titleWidth == 0) titleWidth = skin.textWidth(title);

            width = pad + titleWidth + pad;
            height = pad + skin.textHeight() + pad;
        }

        @Override
        protected void onPressed(int button) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                module.toggle();
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                MeteorClient.mc.setScreen(skin.moduleScreen(module));
            }
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            fill += delta * 6 * (module.isActive() ? 1 : -1);
            fill = Math.max(0, Math.min(1, fill));

            hover += delta * 10 * (mouseOver ? 1 : -1);
            hover = Math.max(0, Math.min(1, hover));

            if (fill > 0) {
                renderer.quad(x, y, width * fill, height, skin.moduleBackground.get());
            }

            SettingColor accent = skin.accentColor.get();

            // Hover tint over the whole row, then a two pixel edge of the accent, which is the
            // indicator the copied client draws on the row the pointer is on.
            if (hover > 0) {
                renderer.quad(x, y, width, height,
                    new Color(accent.r, accent.g, accent.b, (int) Math.round(55 * hover)));
                renderer.quad(x, y, skin.scale(2) * hover, height,
                    new Color(accent.r, accent.g, accent.b, (int) Math.round(220 * hover)));
            }

            double pad = pad();
            double textX = x + pad;
            double inner = width - pad * 2;
            AlignmentX alignment = skin.moduleAlignment.get();

            if (alignment == AlignmentX.Center) textX += inner / 2 - titleWidth / 2;
            else if (alignment == AlignmentX.Right) textX += inner - titleWidth;

            renderer.text(title, textX, y + pad,
                module.isActive() ? ACTIVE_TEXT : skin.textColor.get(), false);
        }

        private static final Color ACTIVE_TEXT = new Color(255, 255, 255);
    }

    public static class TopBar extends WMeteorTopBar {
        private final MeteorGuiTheme skin;

        public TopBar(MeteorGuiTheme skin) {
            this.skin = skin;
            this.theme = skin;
        }

        @Override
        public GuiTheme getTheme() {
            return skin;
        }
    }
}
