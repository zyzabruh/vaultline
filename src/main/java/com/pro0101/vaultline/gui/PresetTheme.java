package com.pro0101.vaultline.gui;

import com.pro0101.vaultline.gui.widgets.GuiSounds.GuiSound;
import com.pro0101.vaultline.gui.widgets.PresetButton;
import com.pro0101.vaultline.gui.widgets.PresetWidgets;
import com.pro0101.vaultline.gui.widgets.PresetWindow;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.DefaultSettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.WAccount;
import meteordevelopment.meteorclient.gui.widgets.WHorizontalSeparator;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WQuad;
import meteordevelopment.meteorclient.gui.widgets.WTopBar;
import meteordevelopment.meteorclient.gui.widgets.WTooltip;
import meteordevelopment.meteorclient.gui.widgets.WVerticalSeparator;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WConfirmedButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WConfirmedMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WFavorite;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WTriangle;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Blur;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.nbt.NbtCompound;

/**
 * Shared base for the addon's GUI themes.
 *
 * <p>Meteor's own theme owns the widget classes: every widget asks its theme for the colours it
 * paints with. That makes a theme from an addon a two part job, and this class does the second
 * part once for all of them. A {@code PresetTheme} is a real {@link GuiTheme} with its own name,
 * so it appears in Meteor's theme dropdown and is saved under its own file, but it hands every
 * widget to an internal {@link MeteorGuiTheme} — the skin — whose palette is rewritten from this
 * theme's settings before each frame. Windows and buttons are the two widgets replaced with
 * addon ones, so the themes also differ in shape and not only in colour.
 *
 * <p>Nothing of Meteor's source is copied: the skin is a normal instance of its theme, driven
 * through its public settings.
 */
public abstract class PresetTheme extends GuiTheme {
    /**
     * What the themes differ in beyond colour: the shape of a window, and the effects around it.
     *
     * @param cornerRadius   corner radius of windows and buttons
     * @param panelGlow      alpha of the halo drawn around windows
     * @param bodyShade      alpha of the dark glass under a window body
     * @param headerHeight   title bar height, zero to leave Meteor's own
     * @param minWidth       minimum window width, zero for the width of the content
     * @param gradientHeader whether the title bar fades between the two accents
     * @param sounds         whether the GUI plays hover and click sounds
     * @param blur           whether the background behind the screens is blurred
     * @param portedSounds   whether the GUI plays the copied client's own sound files instead of
     *                       the chosen vanilla sounds
     */
    public record Style(int cornerRadius, int panelGlow, int bodyShade, double headerHeight,
                        double minWidth, boolean gradientHeader, boolean sounds, boolean blur,
                        boolean portedSounds) {
    }

    private static final java.util.List<PresetTheme> THEMES = new java.util.ArrayList<>();

    private final MeteorGuiTheme skin = new MeteorGuiTheme();
    private final boolean light;
    private final Style style;

    private final SettingGroup sgStyle = settings.createGroup("Style");
    private final SettingGroup sgSounds = settings.createGroup("Sounds");
    private final SettingGroup sgColors = settings.createGroup("Colors");

    private final Setting<Double> scale;
    private final Setting<AlignmentX> moduleAlignment;
    private final Setting<Boolean> categoryIcons;
    private final Setting<Boolean> hideHud;
    private final Setting<Double> cornerRadius;
    private final Setting<Integer> bodyShade;
    private final Setting<Integer> panelGlow;
    private final Setting<Boolean> guiBlur;

    private final Setting<Boolean> guiSounds;
    private final Setting<Integer> soundVolume;
    private final Setting<GuiSound> hoverSound;
    private final Setting<GuiSound> clickSound;
    private final Setting<GuiSound> rightClickSound;

    private boolean blurEnabledByTheme;

    private final Setting<SettingColor> accent;
    private final Setting<SettingColor> accentAlt;
    private final Setting<SettingColor> surface;
    private final Setting<SettingColor> panel;
    private final Setting<SettingColor> outline;
    private final Setting<SettingColor> text;
    private final Setting<SettingColor> textSecondary;

    protected PresetTheme(String name, Palette palette, Style style) {
        super(name);

        this.light = palette.light();
        this.style = style;
        THEMES.add(this);
        this.settingsFactory = new DefaultSettingsWidgetFactory(this);

        this.scale = sgStyle.add(new DoubleSetting.Builder()
            .name("scale")
            .description("Scale of the GUI.")
            .defaultValue(1)
            .min(0.75)
            .sliderRange(0.75, 4)
            .onChanged(value -> {
                skin.scale.set(value);
                if (MeteorClient.mc.currentScreen instanceof WidgetScreen screen) screen.invalidate();
            })
            .build()
        );

        this.moduleAlignment = sgStyle.add(new EnumSetting.Builder<AlignmentX>()
            .name("module-alignment")
            .description("How module titles are aligned.")
            .defaultValue(AlignmentX.Center)
            .onChanged(skin.moduleAlignment::set)
            .build()
        );

        this.categoryIcons = sgStyle.add(new BoolSetting.Builder()
            .name("category-icons")
            .description("Adds item icons to module categories.")
            .defaultValue(false)
            .onChanged(skin.categoryIcons::set)
            .build()
        );

        this.hideHud = sgStyle.add(new BoolSetting.Builder()
            .name("hide-HUD")
            .description("Hide HUD when in GUI.")
            .defaultValue(false)
            .onChanged(skin.hideHUD::set)
            .build()
        );

        this.cornerRadius = sgStyle.add(new DoubleSetting.Builder()
            .name("corner-radius")
            .description("Radius of the rounded corners on windows and buttons. Zero keeps square corners.")
            .defaultValue((double) style.cornerRadius())
            .min(0)
            .max(12)
            .sliderRange(0, 12)
            .build()
        );

        this.bodyShade = sgStyle.add(new IntSetting.Builder()
            .name("body-shade")
            .description("Alpha of the dark glass drawn under the window body. The body itself stays at the theme's opacity; this only stops a bright world showing through the settings. Zero removes it.")
            .defaultValue(style.bodyShade())
            .range(0, 240)
            .sliderRange(0, 240)
            .build()
        );

        this.panelGlow = sgStyle.add(new IntSetting.Builder()
            .name("panel-glow")
            .description("Alpha of the halo drawn around windows. Zero removes it.")
            .defaultValue(style.panelGlow())
            .range(0, 120)
            .sliderRange(0, 120)
            .build()
        );

        this.guiBlur = sgStyle.add(new BoolSetting.Builder()
            .name("gui-blur")
            .description("Blurs the world behind Meteor screens, the way the copied client blurs behind its panels. Runs Meteor's own blur module.")
            .defaultValue(style.blur())
            .onChanged(value -> syncBlur())
            .build()
        );

        this.guiSounds = sgSounds.add(new BoolSetting.Builder()
            .name("gui-sounds")
            .description("Plays a sound when the pointer enters or presses a control.")
            .defaultValue(style.sounds())
            .build()
        );

        this.soundVolume = sgSounds.add(new IntSetting.Builder()
            .name("volume")
            .description("Volume of the GUI sounds, in percent of the game's sound volume.")
            .defaultValue(35)
            .range(0, 100)
            .sliderRange(0, 100)
            .visible(guiSounds::get)
            .build()
        );

        this.hoverSound = sgSounds.add(new EnumSetting.Builder<GuiSound>()
            .name("hover-sound")
            .description("Sound played while the pointer enters a control.")
            .defaultValue(GuiSound.HAT)
            .visible(guiSounds::get)
            .build()
        );

        this.clickSound = sgSounds.add(new EnumSetting.Builder<GuiSound>()
            .name("click-sound")
            .description("Sound played when a control is clicked.")
            .defaultValue(GuiSound.CLICK)
            .visible(guiSounds::get)
            .build()
        );

        this.rightClickSound = sgSounds.add(new EnumSetting.Builder<GuiSound>()
            .name("right-click-sound")
            .description("Sound played when a control is right clicked.")
            .defaultValue(GuiSound.PLING)
            .visible(guiSounds::get)
            .build()
        );

        this.accent = color(sgColors, "accent", "Main colour of the GUI.", palette.accent());
        this.accentAlt = color(sgColors, "accent-alt", "Second accent, used for pressed controls.", palette.accentAlt());
        this.surface = color(sgColors, "surface", "Background of windows and controls.", palette.surface());
        this.panel = color(sgColors, "panel", "Background of an active module.", palette.panel());
        this.outline = color(sgColors, "outline", "Thin border around every control.", palette.outline());
        this.text = color(sgColors, "text", "Colour of normal text.", palette.text());
        this.textSecondary = color(sgColors, "text-secondary", "Colour of secondary text.", palette.textSecondary());

        apply();
    }

    private Setting<SettingColor> color(SettingGroup group, String name, String description, SettingColor value) {
        return group.add(new ColorSetting.Builder()
            .name(name + "-color")
            .description(description)
            .defaultValue(value)
            .build()
        );
    }

    // Palette

    /**
     * Copies this theme's settings into the skin. Called before every frame rather than on
     * setting changes only, so "Reset Colors" and a theme file loaded from disk both take effect
     * without extra plumbing. Colour writes are plain field assignments, not setting writes, so
     * this does not fire change handlers or trigger saves.
     */
    private void apply() {
        if (!skin.scale.get().equals(scale.get())) skin.scale.set(scale.get());
        if (skin.moduleAlignment.get() != moduleAlignment.get()) skin.moduleAlignment.set(moduleAlignment.get());
        if (skin.categoryIcons.get() != categoryIcons.get()) skin.categoryIcons.set(categoryIcons.get());
        if (skin.hideHUD.get() != hideHud.get()) skin.hideHUD.set(hideHud.get());

        SettingColor accentColor = accent.get();
        SettingColor surfaceColor = surface.get();
        SettingColor panelColor = panel.get();
        SettingColor outlineColor = outline.get();
        SettingColor textColor = text.get();

        int step = light ? -1 : 1;
        SettingColor control = controlColor();
        SettingColor surfaceHover = shade(control, step * 30);
        SettingColor surfacePressed = shade(control, step * -24);

        skin.accentColor.set(accentColor);
        skin.checkboxColor.set(accentColor);
        skin.favoriteColor.set(accentAlt.get());
        skin.textColor.set(textColor);
        skin.titleTextColor.set(contrast(accentColor, textColor));
        skin.textSecondaryColor.set(textSecondary.get());
        skin.textHighlightColor.set(withAlpha(accentColor, 90));
        skin.placeholderColor.set(withAlpha(textColor, 120));
        skin.moduleBackground.set(panelColor);
        skin.separatorText.set(textColor);
        skin.separatorCenter.set(withAlpha(accentColor, 170));
        skin.separatorEdges.set(withAlpha(outlineColor, 210));
        skin.sliderLeft.set(accentColor);
        skin.sliderRight.set(surfacePressed);

        states(skin.sliderHandle, accentColor, shade(accentColor, step * 12), shade(accentColor, step * -14));
        states(skin.backgroundColor, control, surfaceHover, surfacePressed);
        states(skin.outlineColor, outlineColor, shade(outlineColor, step * 40), shade(outlineColor, step * 70));
        states(skin.scrollbarColor, withAlpha(accentColor, 200), withAlpha(accentColor, 255), withAlpha(accentColor, 160));
    }

    private static void states(MeteorGuiTheme.ThreeStateColorSetting setting, Color normal, Color hovered, Color pressed) {
        setting.get(false, false).set(normal);
        setting.get(false, true).set(hovered);
        setting.get(true, false).set(pressed);
    }

    private static SettingColor shade(SettingColor color, int delta) {
        return new SettingColor(
            clamp(color.r + delta), clamp(color.g + delta), clamp(color.b + delta), color.a
        );
    }

    private static SettingColor withAlpha(SettingColor color, int alpha) {
        return new SettingColor(color.r, color.g, color.b, alpha);
    }

    /** Picks dark or light title text from the accent, so a pale accent keeps its header readable. */
    private static SettingColor contrast(SettingColor background, SettingColor fallback) {
        double luminance = 0.299 * background.r + 0.587 * background.g + 0.114 * background.b;
        // Only a nearly white accent gets dark text; the lavender the theme is built on keeps the
        // white title the other client draws on it.
        if (luminance > 200) return new SettingColor(18, 18, 24, 255);
        return new SettingColor(245, 247, 250, Math.max(fallback.a, 255));
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    @Override
    public void beforeRender() {
        syncBlur();
        apply();
        skin.beforeRender();
    }

    /** Turns Meteor's blur on for the theme, and back off if the theme enabled it. */
    private void syncBlur() {
        Blur blur = Modules.get().get(Blur.class);
        if (blur == null) return;

        if (guiBlur.get()) {
            if (!blur.isActive()) {
                blur.enable();
                blurEnabledByTheme = true;
            }
        } else if (blurEnabledByTheme && blur.isActive()) {
            blur.toggle();
            blurEnabledByTheme = false;
        }
    }

    /**
     * Returns the theme behind a skin, or null when the widget was not built by one of these
     * themes. Widgets carry the skin they were drawn with, so this is how the sound hooks find the
     * settings of the theme a control belongs to.
     */
    public static PresetTheme owning(GuiTheme theme) {
        if (theme == null) return null;

        // A theme's own screens hand out the theme itself, while its widgets carry the skin.
        if (theme instanceof PresetTheme preset) return preset;

        for (PresetTheme preset : THEMES) {
            if (preset.skin == theme) return preset;
        }

        return null;
    }

    /** Returns the title bar height this theme wants, or zero to keep Meteor's own. */
    public double headerHeight() {
        return style.headerHeight();
    }

    /** Returns the minimum width of a window, or zero for the width of the content. */
    public double minWindowWidth() {
        return style.minWidth();
    }

    /** Returns whether the title bar fades from the accent to the second accent. */
    public boolean gradientHeader() {
        return style.gradientHeader();
    }

    /** Returns whether the GUI plays hover and click sounds. */
    public boolean soundsEnabled() {
        return guiSounds.get();
    }

    /** Whether the GUI plays the copied client's own sound files rather than the vanilla picks. */
    public boolean portedSounds() {
        return style.portedSounds();
    }


    /** Returns the volume of the GUI sounds, in percent. */
    public double soundVolume() {
        return soundVolume.get();
    }

    public GuiSound hoverSound() {
        return hoverSound.get();
    }

    public GuiSound clickSound() {
        return clickSound.get();
    }

    public GuiSound rightClickSound() {
        return rightClickSound.get();
    }

    /** Returns the accent colour, for the widgets that draw their own chrome. */
    public SettingColor accentColor() {
        return accent.get();
    }

    /** Returns the second accent, used at the bottom of title bars. */
    public SettingColor accentAltColor() {
        return accentAlt.get();
    }

    /** Returns the fill of a window body, at the low opacity the theme copies. */
    public SettingColor bodyColor() {
        return surface.get();
    }

    /** Returns the fill at the bottom edge of a window body: the surface, a little darker. */
    public SettingColor bodyBottomColor() {
        return shade(surface.get(), -16);
    }

    /**
     * Returns the background of the controls inside a window.
     *
     * <p>The body of a window is a tenth of the accent, which is fine behind a title and useless
     * behind a text field: whatever the world is showing through would be the background a value
     * has to be read on. Controls therefore get a dark violet that stays dark whatever is behind
     * it, while the window still reads as a thin sheet of colour.
     */
    public SettingColor controlColor() {
        SettingColor base = surface.get();
        double luminance = 0.299 * base.r + 0.587 * base.g + 0.114 * base.b;

        // A theme whose body is already dark uses that body at nearly full opacity; the pale ones
        // get a dark violet instead, so the controls never turn into the colour of the world.
        if (luminance < 90) return new SettingColor(base.r, base.g, base.b, 225);
        return new SettingColor(44, 32, 54, 225);
    }

    /** Returns the colour of a module that is switched on. */
    public SettingColor panelColor() {
        return panel.get();
    }

    /** Returns the fill of a window's title bar: the outline colour, which is the accent at half alpha. */
    public SettingColor headerColor() {
        return outline.get();
    }

    /** Returns the fill of a button: the panel colour with the opacity that client gives buttons. */
    public SettingColor buttonColor() {
        SettingColor color = panel.get();
        return new SettingColor(color.r, color.g, color.b, 115);
    }

    public double cornerRadius() {
        return cornerRadius.get();
    }

    /** Returns the alpha of the dark glass under the window body. */
    public int bodyShade() {
        return bodyShade.get();
    }

    /** Returns the alpha of the halo drawn around windows. */
    public int panelGlow() {
        return panelGlow.get();
    }

    // Widgets

    @Override
    protected <T extends WWidget> T w(T widget) {
        widget.theme = skin;
        return widget;
    }

    @Override
    public WWindow window(WWidget icon, String title) {
        return new PresetWindow(icon, title, this, skin);
    }

    @Override
    public WLabel label(String text, boolean title, double maxWidth) {
        if (maxWidth == 0 && !text.contains("\n")) return new PresetWidgets.Label(text, title, skin);
        return new PresetWidgets.MultiLabel(text, title, maxWidth, skin);
    }

    @Override
    public WHorizontalSeparator horizontalSeparator(String text) {
        return new PresetWidgets.HorizontalSeparator(text, skin);
    }

    @Override
    public WVerticalSeparator verticalSeparator() {
        return new PresetWidgets.VerticalSeparator(skin);
    }

    @Override
    protected WButton button(String text, GuiTexture texture) {
        return new PresetButton(text, texture, this, skin);
    }

    @Override
    protected WConfirmedButton confirmedButton(String text, String confirmText, GuiTexture texture) {
        return new PresetWidgets.ConfirmedButton(text, confirmText, texture, skin);
    }

    @Override
    public WMinus minus() {
        return new PresetWidgets.Minus(skin);
    }

    @Override
    public WConfirmedMinus confirmedMinus() {
        return new PresetWidgets.ConfirmedMinus(skin);
    }

    @Override
    public WPlus plus() {
        return new PresetWidgets.Plus(skin);
    }

    @Override
    public WCheckbox checkbox(boolean checked) {
        return new PresetWidgets.Checkbox(checked, skin);
    }

    @Override
    public WSlider slider(double value, double min, double max) {
        return new PresetWidgets.Slider(value, min, max, skin);
    }

    @Override
    public WTextBox textBox(String text, String placeholder, CharFilter filter, Class<? extends WTextBox.Renderer> renderer) {
        return new PresetWidgets.TextBox(text, placeholder, filter, renderer, skin);
    }

    @Override
    public <T> WDropdown<T> dropdown(T[] values, T value) {
        return new PresetWidgets.Dropdown<>(values, value, skin);
    }

    @Override
    public WTriangle triangle() {
        return new PresetWidgets.Triangle(skin);
    }

    @Override
    public WTooltip tooltip(String text) {
        return new PresetWidgets.Tooltip(text, skin);
    }

    @Override
    public WView view() {
        return new PresetWidgets.View(skin);
    }

    @Override
    public WSection section(String title, boolean expanded, WWidget headerWidget) {
        return new PresetWidgets.Section(title, expanded, headerWidget, skin);
    }

    @Override
    public WAccount account(WidgetScreen screen, Account<?> account) {
        return new PresetWidgets.AccountWidget(screen, account, skin);
    }

    @Override
    public WWidget module(Module module, String title) {
        return new PresetWidgets.ModuleWidget(module, title, skin);
    }

    @Override
    public WQuad quad(Color color) {
        return skin.quad(color);
    }

    @Override
    public WTopBar topBar() {
        return new PresetWidgets.TopBar(skin);
    }

    @Override
    public WFavorite favorite(boolean checked) {
        return new PresetWidgets.Favorite(checked, skin);
    }

    // Colours

    @Override
    public Color textColor() {
        return text.get();
    }

    @Override
    public Color textSecondaryColor() {
        return textSecondary.get();
    }

    @Override
    public Color starscriptTextColor() {
        return skin.starscriptTextColor();
    }

    @Override
    public Color starscriptBraceColor() {
        return skin.starscriptBraceColor();
    }

    @Override
    public Color starscriptParenthesisColor() {
        return skin.starscriptParenthesisColor();
    }

    @Override
    public Color starscriptDotColor() {
        return skin.starscriptDotColor();
    }

    @Override
    public Color starscriptCommaColor() {
        return skin.starscriptCommaColor();
    }

    @Override
    public Color starscriptOperatorColor() {
        return skin.starscriptOperatorColor();
    }

    @Override
    public Color starscriptStringColor() {
        return skin.starscriptStringColor();
    }

    @Override
    public Color starscriptNumberColor() {
        return skin.starscriptNumberColor();
    }

    @Override
    public Color starscriptKeywordColor() {
        return skin.starscriptKeywordColor();
    }

    @Override
    public Color starscriptAccessedObjectColor() {
        return skin.starscriptAccessedObjectColor();
    }

    // Other

    @Override
    public TextRenderer textRenderer() {
        return skin.textRenderer();
    }

    @Override
    public double scale(double value) {
        return skin.scale(value);
    }

    @Override
    public boolean categoryIcons() {
        return categoryIcons.get();
    }

    @Override
    public boolean hideHUD() {
        return hideHud.get();
    }

    @Override
    public meteordevelopment.meteorclient.gui.utils.WindowConfig getWindowConfig(String id) {
        return skin.getWindowConfig(id);
    }

    @Override
    public void clearWindowConfigs() {
        skin.clearWindowConfigs();
    }

    // Saving

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = super.toTag();
        skin.toTag().getCompound("windowConfigs").ifPresent(configs -> tag.put("windowConfigs", configs));
        return tag;
    }

    @Override
    public GuiTheme fromTag(NbtCompound tag) {
        super.fromTag(tag);
        tag.getCompound("windowConfigs").ifPresent(configs -> {
            NbtCompound wrapper = new NbtCompound();
            wrapper.put("windowConfigs", configs);
            skin.fromTag(wrapper);
        });
        apply();
        return this;
    }
}
