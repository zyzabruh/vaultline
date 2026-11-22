package com.pro0101.vaultline.gui.zio;

import com.pro0101.vaultline.gui.widgets.GuiSounds;
import java.time.LocalDate;
import com.pro0101.vaultline.render.GuiFx;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BlockPosSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StorageBlockListSetting;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.profiles.Profile;
import meteordevelopment.meteorclient.systems.profiles.Profiles;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * The copied client's ClickGUI, rebuilt from its own source.
 *
 * <p>Every number here is taken from {@code open-zio-main}: a panel is 92 pixels plus 7 of width
 * padding, its title bar is 14, a row is 4 of height padding plus the 9 pixel font, the text sits at
 * 2.5 pixels from the top of its row, the name is drawn 3 pixels from the left edge of the panel and
 * the setting name 4, the expand indicator sits 4 pixels from the right edge, panels start at
 * {@code x = 10, y = 5} and are placed 3 pixels apart, and a panel is at most the screen height minus
 * 50.
 *
 * <p>The colours are the defaults of that client's UI module, unmodified: the accent at half alpha
 * for the title bar and the outline, a tenth for the body, the enabled fill at 90, a gradient from
 * the button colour at a tenth of its alpha, text at {@code #E1E1E1} and pure white when a module is
 * on. The drawing order is theirs too: body, then each row's enabled fill, then its gradient, then
 * the title bar and finally the one pixel outline.
 *
 * <p>A left click toggles a module, a right click opens its settings inside the panel — booleans
 * toggle, numbers drag, enum values cycle — and the title bar drags the panel or folds it on a right
 * click. Only the parts that need a client-side service the addon does not have (its sound files,
 * its font atlas, searching) are different.
 */
public class ZioScreen extends TabScreen {
    // Geometry, from Panel and GuiElement.
    private static final int PANEL_WIDTH = 92 + 7;

    /** Fourteen, the copied client's header for every style but SLIM (see ZioPanelPaint). */
    private static final int HEADER_HEIGHT = ZioPanelPaint.headerHeight(false);
    private static final int ROW_HEIGHT = ZioListLayout.ROW_HEIGHT;
    private static final int PANEL_X = 10;

    /**
     * Top of the panels, in layout units.
     *
     * <p>The copied client starts its panels five pixels down, but the client this addon lives in
     * draws its tab bar along the top of every screen it owns. That bar is a padding above and below
     * nine pixels of text, seventeen GUI pixels in all: the panels start just under it instead of
     * covering it.
     */
    private static final int PANEL_Y = 19;
    private static final int PANEL_GAP = 3;
    private static final double TEXT_Y = (ROW_HEIGHT - 9 + 1) / 2.0;
    private static final int HEIGHT_MARGIN = ZioChrome.HEIGHT_MARGIN;

    /**
     * The search, as the copied client draws it: no field and no box, a fifth of black over the screen
     * while it is open, the query in the middle of it, and a hint pulsing in the bottom right corner.
     */
    private static final long SEARCH_FOCUS_MILLIS = 300L;
    private static final long SEARCH_CARET_MILLIS = 500L;
    private static final long SEARCH_HINT_MILLIS = 2000L;
    private static final long SEARCH_HINT_OFFSET = 250L;
    private static final int SEARCH_HINT_GREY = 0x80;

    /** The completion is {@code ChatFormatting.GRAY}, which is a lighter grey than the hint's. */
    private static final int SEARCH_COMPLETION = 0xFFAAAAAA;

    /** How many rows may stand open at once; a fourth closes the oldest, as the copied client does. */
    private static final int MAX_OPEN_ROWS = 3;

    /** One blink of the search's row flash, and how many of them there are. */
    private static final long HIGHLIGHT_MILLIS = 150L;
    private static final int HIGHLIGHT_BLINKS = 4;

    // The preset panel, built from the same line heights as the rest of the screen.
    private static final int PRESET_WIDTH = 124;
    private static final int PRESET_HEADER = 14;
    private static final int PRESET_ROWS = ZioListLayout.LIST_ROWS;
    private static final int PRESET_HEIGHT = PRESET_HEADER + ROW_HEIGHT + 3 + PRESET_ROWS * ROW_HEIGHT + 3
        + ROW_HEIGHT + 4;

    // The colour picker, in the same shape as the copied client's: a square for saturation and
    // brightness, then a hue bar and an alpha bar, each six pixels tall.
    private static final int COLOUR_PADDING = 3;
    private static final int COLOUR_SLIDER = 6;
    private static final int COLOUR_SQUARE = PANEL_WIDTH - COLOUR_PADDING * 2;
    private static final int COLOUR_PICKER_HEIGHT = ZioListLayout.COLOUR_PICKER_HEIGHT;

    // An open list: a search field and six checked entries, scrolled inside that box.
    private static final int LIST_ROWS = ZioListLayout.LIST_ROWS;
    private static final int LIST_SEARCH_HEIGHT = 13;
    private static final int LIST_HEIGHT = ZioListLayout.LIST_HEIGHT;

    // The game panel: a header line above the board, one tile of a 128x32 sheet.
    private static final Identifier GAME_TEXTURE = Identifier.of("vaultline", "textures/gui/game.png");
    private static final int GAME_TILE = 16;
    private static final int GAME_TEXTURE_WIDTH = 128;
    private static final int GAME_TEXTURE_HEIGHT = 32;

    /** One choice of a list setting: a value, what it reads as, and its registry name. */
    private record ListEntry(Object value, String label, String id) {}

    /**
     * The copied client lays its screen out in the game's own scaled pixels, one for one, so a panel
     * is 99 units wide at whatever GUI scale the player runs. Drawing at half that made everything
     * tiny at scale 2 and 3; this keeps the size the client has.
     */
    /** How much of the game's GUI units one layout unit takes; the zoom buttons change it. */
    private static float uiScale() {
        return ZioScaleStore.current();
    }

    /** Stores a new zoom: clamped, written to the mod's config and used from the next frame on. */
    private static void setUiScale(float value) {
        ZioScaleStore.set(value);
    }

    /** The frosted sheet the panels lay under their content; see {@code MakeAero}. */
    private static final Identifier AERO_TEXTURE = Identifier.of("vaultline", "textures/gui/aero.png");



    /** Bloom colour, and the reach of its halo in pixels. */
    private static final int BLOOM_COLOUR = 0xB0C794F4;

    /** The copied client's blur radius ({@code BlurModule.radiusSetting}, default 6). */
    private static final float BLUR_RADIUS = 6f;
    private static final float BLOOM_STRENGTH = 0.4f;

    /** Panel rectangles for the effect shaders, reused every frame so the pass never allocates. */
    private static final float[] EFFECT_RECTS = new float[12 * 4];

    /** The screen on show, so the effects pass can read its panels before the GUI is drawn. */
    private static ZioScreen active;

    // Colours, from UIModule's defaults.
    private static final int COLOR = 0x82C794F4;
    private static final int BACKGROUND = 0x1EC794F4;
    private static final int BUTTON = 0x73C794F4;
    private static final int ENABLED = 0x5AC794F4;
    private static final int TEXT = 0xFFE1E1E1;
    private static final int TEXT_ENABLED = 0xFFFFFFFF;
    /** The copied client's tooltip panel: ten, ten, ten at eighty. */
    private static final int TOOLTIP = 0x500A0A0A;

    private final List<Panel> panels = new ArrayList<>();

    private double mouseX;
    private double mouseY;
    private double alpha;

    /** When this screen was first drawn, for its opening fade. */
    private long openedAt;

    /** The scissor regions open this frame, outermost first. */
    private final ZioScissor scissors = new ZioScissor();

    private Panel draggingPanel;
    private double dragOffsetX;
    private double dragOffsetY;
    private SettingRow draggingSetting;

    /**
     * The number being typed into, and what has been typed so far.
     *
     * <p>A slider is only as exact as it is wide: the copied client lets a right click turn it into
     * a text field, and so does this one.
     */
    private SettingRow editingSetting;
    private boolean editingString;

    /** The key that is being bound: the next key or button pressed becomes its value. */
    private SettingRow bindingSetting;

    /** The open list whose search field has the keyboard. */
    private SettingRow listSearchRow;
    private String editText = "";
    private long editStart;

    /** The search of the copied client: Ctrl + F opens it and it filters the modules. */
    private boolean searchFocused;
    private String searchQuery = "";
    private String searchCompletion = "";
    private long searchStart;

    /** Zero to one over 300 ms: whether the search has the keyboard, which dims the screen. */
    private final ZioAnim.Timed searchFocus = new ZioAnim.Timed(0f);

    /** The rows standing open right now, oldest first, so a fourth can close the first. */
    private final List<Row> openRows = new ArrayList<>();

    /** The open module whose bind line is listening for its next key, or null. */
    private Row bindingRow;

    /** The winter flakes the copied client falls over this screen, December to February. */
    private final ZioSnow snow = new ZioSnow();

    /**
     * The tooltip drawn last frame, as {@code left, top, right, bottom}, so the blur pass of this
     * frame can reach behind it.
     *
     * <p>The copied client blurs its tooltip as it draws it, in the middle of its GUI. Here the GUI is
     * recorded and executed only after the effect pass has run, so the rectangle has to come from the
     * frame before: the pointer moves a pixel or two between two frames, which is the only difference
     * it makes.
     */
    private ZioPaint.Bounds tooltipBounds;

    /** The lines drawn inside {@link #tooltipBounds}, decided with it. */
    private String[] tooltipLines;

    /** Whether the pointer's chase, the panels and the tooltip have been stepped for this frame. */
    private boolean framed;

    /** The frame's partial tick, kept so a frame stepped before drawing uses the usual value. */
    private double frameDelta = 1;

    /** The panel list as {@link ZioTooltipFrame} sees it: stepped first, then read. */
    private final ZioTooltipFrame.Panels panelsSource = new ZioTooltipFrame.Panels() {
        @Override
        public void step() {
            stepPanels();
        }

        @Override
        public String descriptionAt(double x, double y) {
            Panel hovered = panelAt(x, y);
            Module module = hovered == null ? null : hovered.moduleAt(x, y);

            // A setting's own description wins over its module's, as the copied client's components
            // render after the row they sit in and overwrite its tooltip.
            return ZioPaint.descriptionToShow(settingDescription(hovered),
                module == null ? null : module.description);
        }
    };

    /** Where the tooltip is drawn from: the pointer, caught up to at the copied client's own speed. */
    private double hoverX;
    private double hoverY;
    private boolean hoverFollowing;

    /** What the paint routines draw through; its context is the frame being drawn. */
    private final Canvas canvas = new Canvas();

    /**
     * The preset panel: a list of the client's profiles with apply, save and delete.
     *
     * <p>Nothing is kept here but the panel's own state; the presets themselves are the profiles
     * Meteor already stores, so the panel only ever reads and writes those.
     */
    private boolean presetsOpen;
    private boolean presetNameFocused;

    /** The three games the Game panel holds, and the one it is showing. */
    private final ZioMinesweeper minesweeper = new ZioMinesweeper(ZioMinesweeper.Level.EASY);
    private final ZioSnake snake = new ZioSnake();
    private final ZioTetris tetris = new ZioTetris();
    private ZioGameType gameType = ZioGameType.TETRIS;
    private boolean gameOpen;

    /** How much of the copied client's secret sequence has been typed in. */
    private int secretProgress;

    /** The panel blur and bloom, which the copied client keeps behind its own Blur module. */
    private boolean effects = true;
    private String presetName = "";
    private int presetSelected = -1;
    private double presetScroll;

    public ZioScreen(GuiTheme theme, Tab tab) {
        super(theme, tab);

        buildPanels();
        active = this;
    }

    /**
     * Runs the panel blur and bloom for the frame about to be drawn, and decides that frame's tooltip.
     *
     * <p>Called after the world and before the GUI: the panels' rectangles — and the tooltip's, decided
     * here from this frame's pointer — are handed to the shaders, which blur the frame inside them and
     * add the halo around their edges. The screen then draws its panels and that same tooltip over the
     * result, so the glass look comes from the frame itself rather than a tint.
     */
    public static void applyEffects() {
        ZioScreen screen = active;
        if (screen == null || mc.currentScreen != screen) return;

        // This frame's pointer, panel animations, hovered line and rectangle are all advanced here,
        // before the shaders run, so the blur and the tooltip drawn afterwards cover the same pixels
        // of the same frame — during an opening or closing transition too. The copied client does the
        // same: it lets its panels update and render, then blurs the rectangle it has just laid out.
        screen.stepFrame();

        if (!screen.effects) return;

        int count = screen.fillEffectRects();
        if (count == 0) return;

        // The copied client scales its blur radius with the screen's own fade, so the effect arrives
        // with the panels instead of before them.
        GuiFx.render(EFFECT_RECTS, count, BLOOM_COLOUR, BLUR_RADIUS * (float) screen.alpha, BLOOM_STRENGTH);
    }

    /**
     * The rectangles the shaders blur and halo: the category panels, then the game panel and the
     * presets, which the copied client blurs as ordinary panels of its own.
     *
     * @return how many rectangles were written into the shared buffer
     */
    private int fillEffectRects() {
        int limit = viewHeight() - HEIGHT_MARGIN;
        int count = 0;

        for (Panel panel : panels) {
            int drawn = HEADER_HEIGHT
                + (int) Math.round((panel.height(limit) - HEADER_HEIGHT) * panel.expansion);

            if (drawn <= 0) continue;

            count = addEffectRect(count, panel.x, panel.y, PANEL_WIDTH, drawn);
        }

        if (gameOpen) count = addEffectRect(count, gameX(), gameY(), gameWidth(), gameHeight());
        if (presetsOpen) count = addEffectRect(count, presetX(), presetY(), PRESET_WIDTH, PRESET_HEIGHT);

        // The tooltip is blurred behind as well, with the same radius, as the copied client does: the
        // rectangle is the one the tooltip will be drawn in during this same frame.
        if (tooltipBounds != null) {
            count = addEffectRect(count, tooltipBounds.left(), tooltipBounds.top(),
                tooltipBounds.right() - tooltipBounds.left(), tooltipBounds.bottom() - tooltipBounds.top());
        }

        return count;
    }

    /**
     * Writes one layout rectangle as screen UVs, bottom-left origin, as the shaders expect them.
     *
     * @return the count that follows, unchanged when the buffer is full
     */
    private int addEffectRect(int count, int x, int y, int width, int height) {
        if (count * 4 + 4 > EFFECT_RECTS.length) return count;

        float screenWidth = Math.max(1, viewWidth());
        float screenHeight = Math.max(1, viewHeight());
        int offset = count * 4;

        EFFECT_RECTS[offset] = x / screenWidth;
        EFFECT_RECTS[offset + 1] = 1f - (y + height) / screenHeight;
        EFFECT_RECTS[offset + 2] = (x + width) / screenWidth;
        EFFECT_RECTS[offset + 3] = 1f - y / screenHeight;

        return count + 1;
    }

    private void buildPanels() {
        Map<Category, List<Module>> byCategory = new LinkedHashMap<>();

        for (Module module : Modules.get().getAll()) {
            byCategory.computeIfAbsent(module.category, key -> new ArrayList<>()).add(module);
        }

        int index = 0;

        for (Map.Entry<Category, List<Module>> entry : byCategory.entrySet()) {
            List<Module> modules = entry.getValue();
            modules.sort(Comparator.comparing(module -> module.title));

            panels.add(new Panel(entry.getKey().name, modules, PANEL_X + index * (PANEL_WIDTH + PANEL_GAP)));
            index++;
        }
    }

    @Override
    public void initWidgets() {
        // Nothing: this screen draws itself instead of a tree of widgets.
    }

    // Painting helpers

    private static int fade(int argb, double factor) {
        return ZioPaint.fade(argb, factor);
    }

    /**
     * The seam between this screen and the paint routines: the one implementation of {@link ZioCanvas}
     * that writes into the frame.
     */
    private final class Canvas implements ZioCanvas {
        private DrawContext context;

        @Override
        public void fill(int x1, int y1, int x2, int y2, int argb) {
            context.fill(x1, y1, x2, y2, argb);
        }

        @Override
        public void fillGradient(int x1, int y1, int x2, int y2, int topArgb, int bottomArgb) {
            context.fillGradient(x1, y1, x2, y2, topArgb, bottomArgb);
        }

        @Override
        public void text(String value, float x, float y, int argb) {
            ZioFontRenderer.draw(context, value, x, y, argb);
        }

        @Override
        public void textScaled(String value, float x, float y, int argb, float scale) {
            ZioFontRenderer.drawScaled(context, value, x, y, argb, scale);
        }

        @Override
        public float textWidth(String value) {
            return ZioFontRenderer.widthF(value);
        }

        @Override
        public int lineHeight() {
            return ZioFontRenderer.lineHeight();
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Every label on this screen goes through the copied client's own text renderer, shadow included.
     */
    private static void text(DrawContext context, String value, float x, float y, int color) {
        ZioFontRenderer.draw(context, value, x, y, color);
    }

    private static int textWidth(String value) {
        return ZioFontRenderer.width(value);
    }

    // 1.21 transforms the scissor rectangle by the matrices itself, so these stay in layout units.

    /**
     * Scissors a region for the fills, the text and the game, clipped to the region already open — the
     * copied client's {@code ScissorStack.pushIntersection} (see {@link ZioScissor}).
     */
    private void scissorOn(DrawContext context, int x1, int y1, int x2, int y2) {
        int[] region = scissors.push(x1, y1, x2, y2);

        context.enableScissor(region[0], region[1], region[2], region[3]);
        ZioFontRenderer.scissor(region[0], region[1], region[2], region[3]);
    }

    private void scissorOff(DrawContext context) {
        context.disableScissor();

        int[] parent = scissors.pop();

        if (parent == null) {
            ZioFontRenderer.noScissor();
        } else {
            ZioFontRenderer.scissor(parent[0], parent[1], parent[2], parent[3]);
        }
    }

    private static int scaleX(double value) {
        return (int) Math.round(value);
    }

    private static int scaleY(double value) {
        return (int) Math.round(value);
    }

    /** Width the layout works in: the game's scaled width at the half scale drawn below. */
    private int viewWidth() {
        return Math.round(mc.getWindow().getScaledWidth() / uiScale());
    }

    /** Height the layout works in, for the panel height limit and the pointer. */
    private int viewHeight() {
        return Math.round(mc.getWindow().getScaledHeight() / uiScale());
    }

    // Frame

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // The pointer arrives in the game's scaled units; the layout works in its own.
        this.mouseX = mouseX / uiScale();
        this.mouseY = mouseY / uiScale();

        // Last frame's shape goes on the window, then this one starts from the arrow again.
        ZioCursor.chosen().apply();
        ZioCursor.reset();

        // The screen appears over the fixed 150 ms the copied client's transition uses, straight and
        // not eased.
        long now = System.currentTimeMillis();
        if (openedAt == 0) openedAt = now;
        alpha = Math.min(1, (now - openedAt) / (double) ZioAnim.OPEN_MILLIS);

        // The games' own clocks: space hurries the snake, and a finished board resets itself.
        snake.setFast(GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS);
        activeGame().tick(System.currentTimeMillis());

        int limit = viewHeight() - HEIGHT_MARGIN;

        // The panels are stepped with this frame's tick, then read by the tooltip — unless the effect
        // pass already did both, which is the normal path.
        frameDelta = delta;
        if (!framed) stepFrame();

        // Everything below is in layout units, so the whole screen is scaled down once.
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(uiScale(), uiScale());

        // The frame's text goes into one batch: a run of quads per scissor region instead of one
        // render state per glyph.
        scissors.clear();
        ZioFontRenderer.begin(context, uiScale());

        try {
            drawContents(context, limit);
        } finally {
            // Hands the last run over even if a panel threw, so the next screen starts clean.
            ZioFontRenderer.end();
            context.getMatrices().popMatrix();
        }
    }

    private void drawContents(DrawContext context, int limit) {
        canvas.context = context;

        // Both normally happened before the blur pass; a frame where that hook did not run does it
        // here, so the tooltip is still laid out on this frame's pointer and drawn where it was blurred.
        if (!framed) stepFrame();

        drawBackgroundOverlay(context);
        drawSnow(context);

        for (Panel panel : panels) drawPanel(context, panel, limit);

        if (tooltipBounds != null) {
            ZioFontRenderer.flush();
            ZioPaint.tooltip(canvas, tooltipBounds, tooltipLines, fade(TOOLTIP, alpha), fade(0xFFFFFFFF, alpha));
        }

        // The next frame's step belongs to its own blur pass again.
        framed = false;

        ZioFontRenderer.flush();
        drawSearch(context);
        drawPresetButton(context);
        drawZoom(context);
        drawGameButton(context);
        drawFxButton(context);

        if (gameOpen) {
            ZioFontRenderer.flush();
            drawGame(context);
        }

        if (presetsOpen) {
            ZioFontRenderer.flush();
            drawPresets(context);
        }

        // What this frame found under the pointer is what the next frame installs.
        ZioCursor.set(hoverCursor());
    }

    /**
     * The shape the pointer should have wherever it is now, decided from the same hit tests the mouse
     * handlers use: a field that takes typing is an I-beam, everything else the pointer can act on is a
     * hand, and anything else leaves the arrow.
     */
    private ZioCursor hoverCursor() {
        double x = mouseX;
        double y = mouseY;

        if (presetsOpen) {
            int presetX = presetX();
            int presetY = presetY();

            if (x >= presetX && x <= presetX + PRESET_WIDTH && y >= presetY && y <= presetY + PRESET_HEIGHT) {
                boolean overNameField = y >= presetFieldTop() && y <= presetFieldTop() + ROW_HEIGHT;

                return ZioCursorRule.cursorFor(overNameField ? ZioCursorRule.Zone.FIELD
                    : ZioCursorRule.Zone.BUTTON);
            }
        }

        boolean overChrome = presetsButton().contains(x, y) || zoomMinusButton().contains(x, y)
            || zoomPlusButton().contains(x, y) || (ZioGames.unlocked() && gameButton().contains(x, y))
            || fxButton().contains(x, y);

        if (overChrome) return ZioCursorRule.cursorFor(ZioCursorRule.Zone.BUTTON);

        if (gameOpen) {
            int panelX = gameX();
            int panelY = gameY();

            if (x >= panelX && x <= panelX + gameWidth() && y >= panelY && y <= panelY + gameHeight()) {
                return ZioCursorRule.cursorFor(ZioCursorRule.Zone.BUTTON);
            }
        }

        Panel panel = panelAt(x, y);

        if (panel == null) return ZioCursorRule.cursorFor(ZioCursorRule.Zone.NONE);
        if (panel.isOverHeader(x, y)) return ZioCursorRule.cursorFor(ZioCursorRule.Zone.PANEL_HEADER);

        int limit = viewHeight() - HEIGHT_MARGIN;

        if (panel.rowAt(x, y, limit) != null) return ZioCursorRule.cursorFor(ZioCursorRule.Zone.MODULE_ROW);

        SettingRow setting = panel.settingAt(x, y, limit);

        if (setting != null && (setting == editingSetting || setting == bindingSetting)) {
            return ZioCursorRule.cursorFor(ZioCursorRule.Zone.FIELD);
        }

        // An unfolded list carries its own search field on its first line.
        if (setting != null && setting == listSearchRow) {
            int searchTop = listSearchTop(setting);

            if (y >= searchTop && y < searchTop + ROW_HEIGHT) {
                return ZioCursorRule.cursorFor(ZioCursorRule.Zone.FIELD);
            }
        }

        if (setting != null) return ZioCursorRule.cursorFor(ZioCursorRule.Zone.SETTING_ROW);

        return ZioCursorRule.cursorFor(ZioCursorRule.Zone.NONE);
    }

    // Presets

    private int presetX() {
        return viewWidth() - PRESET_WIDTH - 8;
    }

    private int presetY() {
        return 8;
    }

    private int presetFieldTop() {
        return presetY() + PRESET_HEADER;
    }

    private int presetListTop() {
        return presetFieldTop() + ROW_HEIGHT + 3;
    }

    private int presetActionsTop() {
        return presetListTop() + PRESET_ROWS * ROW_HEIGHT + 3;
    }

    private ZioChrome.Rect presetsButton() {
        return ZioChrome.presetsButton(viewWidth(), viewHeight());
    }

    /**
     * The presets to show: the profiles the client already stores, in display order.
     *
     * <p>Profiles are the client's own named saves, so the panel never keeps a second list of its
     * own — what is shown here is what is on disk.
     */
    private List<String> profileNames() {
        Profiles profiles = Profiles.get();
        if (profiles == null) return List.of();

        List<String> names = new ArrayList<>();

        for (Profile profile : profiles.getAll()) names.add(profile.name.get());

        return ZioPresets.sorted(names);
    }

    private ZioChrome.Rect zoomMinusButton() {
        return ZioChrome.zoomMinus(viewWidth(), viewHeight());
    }

    private ZioChrome.Rect zoomLabelRect() {
        return ZioChrome.zoomLabel(viewWidth(), viewHeight());
    }

    private ZioChrome.Rect zoomPlusButton() {
        return ZioChrome.zoomPlus(viewWidth(), viewHeight());
    }

    // Game panel

    private ZioChrome.Rect gameButton() {
        return ZioChrome.gameButton(viewWidth(), viewHeight());
    }

    private ZioGridGame activeGame() {
        return switch (gameType) {
            case TETRIS -> tetris;
            case SNAKE -> snake;
            case MINESWEEPER -> minesweeper;
        };
    }

    private int gameWidth() {
        return ZioGameLayout.panelWidth(gameType, minesweeper.columns());
    }

    private int gameHeight() {
        return ZioGameLayout.panelHeight(gameType, minesweeper.columns(), minesweeper.rows());
    }

    private int gameBoardHeight() {
        return ZioGameLayout.boardHeight(gameType, minesweeper.columns(), minesweeper.rows());
    }

    private int gameCell() {
        return ZioGameLayout.cellSize(gameType, activeGame().columns(), minesweeper.columns());
    }

    private int gameX() {
        return ZioGameLayout.panelX(viewWidth(), gameWidth());
    }

    private int gameY() {
        return ZioGameLayout.panelY(viewHeight(), gameHeight(), HEIGHT_MARGIN);
    }

    private void drawGameButton(DrawContext context) {
        // The button only exists once the sequence has been typed; before that the strip has a gap
        // where it will appear.
        if (!ZioGames.unlocked()) return;

        ZioChrome.Rect button = gameButton();

        context.fill(button.x(), button.y(), button.right(), button.bottom(), fade(BUTTON, alpha));
        text(context, "Game", button.x() + 4, button.y() + 2, fade(TEXT_ENABLED, alpha));
    }

    /**
     * The Game panel: the copied client's {@code GamePanel}, with the three games it holds — Tetris,
     * Snake and Minesweeper — on its title bar, which is the button that cycles them there.
     */
    private void drawGame(DrawContext context) {
        int x = gameX();
        int y = gameY();
        int right = x + gameWidth();
        int bottom = y + gameHeight();
        int boardTop = y + HEADER_HEIGHT;

        context.fill(x, y, right, bottom, fade(BACKGROUND, alpha));

        switch (gameType) {
            case MINESWEEPER -> drawMinesweeper(context, x, boardTop);
            case SNAKE -> drawSnake(context, x, boardTop);
            case TETRIS -> drawTetris(context, x, boardTop);
        }

        // Under the board, the copied panel's own button. It is a plain line of text in that client —
        // the button class draws no background of its own — one row down from the board.
        int buttonTop = boardTop + gameBoardHeight();

        ZioPaint.gameButton(canvas, x, (float) (buttonTop + TEXT_Y + 1), gameType.gameName(),
            fade(TEXT_ENABLED, alpha));

        // The title bar carries the game's name, then the one pixel outline, like the module panels.
        int outline = fade(COLOR, alpha);

        context.fill(x, y, right, y + HEADER_HEIGHT, fade(COLOR, alpha));
        text(context, gameType.gameName(), x + 2, y + HEADER_HEIGHT / 2 - ZioFontRenderer.lineHeight() / 2,
            fade(0xFFFFFFFF, alpha));

        context.fill(x - 1, y - 1, right, y, outline);
        context.fill(x - 1, bottom - 1, right, bottom, outline);
        context.fill(x - 1, y - 1, x, bottom, outline);
        context.fill(right, y - 1, right + 1, bottom, outline);
    }

    /** The minefield: its own header line, then the cells of the client's sprite sheet. */
    private void drawMinesweeper(DrawContext context, int x, int boardTop) {
        int cell = gameCell();
        String score = String.valueOf(minesweeper.score());
        String difficulty = minesweeper.level().levelName();
        String remaining = String.valueOf(minesweeper.remainingMines());
        int width = minesweeper.columns() * cell;
        float difficultyWidth = textWidth(difficulty) * 0.8f;
        float remainingWidth = textWidth(remaining) * 0.8f;

        ZioFontRenderer.drawScaled(context, score, x + 1, boardTop + 1.5f, fade(TEXT_ENABLED, alpha), 0.8f);
        ZioFontRenderer.drawScaled(context, difficulty, x + (width - difficultyWidth) / 2f,
            boardTop + 1.5f, fade(TEXT_ENABLED, alpha), 0.8f);
        ZioFontRenderer.drawScaled(context, remaining, x + width - remainingWidth - cell,
            boardTop + 1.5f, fade(TEXT_ENABLED, alpha), 0.8f);

        for (int row = 0; row < minesweeper.rows(); row++) {
            for (int column = 0; column < minesweeper.columns(); column++) {
                ZioMinesweeper.Cell cellContent = minesweeper.cell(row, column);
                ZioGameSprite sprite = ZioGameSprite.of(cellContent.state(), cellContent.content());

                int opacity = 255;
                if (minesweeper.won()) opacity = 150;
                else if (isOverMinesweeperCell(column, row)) opacity = 210;

                context.drawTexture(RenderPipelines.GUI_TEXTURED, GAME_TEXTURE,
                    x + column * cell, boardTop + (row + 1) * cell,
                    sprite.u(), sprite.v(), cell, cell, GAME_TILE, GAME_TILE,
                    GAME_TEXTURE_WIDTH, GAME_TEXTURE_HEIGHT, fade(0xFFFFFFFF, alpha, opacity));
            }
        }

        if (minesweeper.won()) {
            String title = "You done it!";
            String finalScore = "Score " + minesweeper.score();
            int centre = x + width / 2;
            int textY = boardTop + gameBoardHeight() / 2;

            text(context, title, centre - textWidth(title) / 2, textY, fade(TEXT_ENABLED, alpha));
            text(context, finalScore, centre - textWidth(finalScore) / 2,
                textY + ZioFontRenderer.lineHeight() + 1, fade(TEXT_ENABLED, alpha));
        }
    }

    /** Snake: an apple, the snake's head and its body, in the copied client's colours. */
    private void drawSnake(DrawContext context, int x, int boardTop) {
        int cell = gameCell();

        context.fill(x + snake.food().x() * cell, boardTop + snake.food().y() * cell,
            x + (snake.food().x() + 1) * cell, boardTop + (snake.food().y() + 1) * cell,
            fade(ZioGameColors.APPLE, alpha));

        boolean head = true;

        for (ZioSnake.Segment segment : snake.segments()) {
            int colour;

            if (head) {
                colour = snake.gameOver() ? ZioGameColors.SNAKE_HEAD_OVER : ZioGameColors.SNAKE_HEAD;
                head = false;
            } else {
                colour = snake.gameOver() ? ZioGameColors.SNAKE_BODY_OVER : ZioGameColors.SNAKE_BODY;
            }

            context.fill(x + segment.x() * cell, boardTop + segment.y() * cell,
                x + (segment.x() + 1) * cell, boardTop + (segment.y() + 1) * cell,
                fade(colour, alpha));
        }

        drawGameScore(context, x, boardTop, snake.score(), snake.gameOver());
    }

    /** Tetris: the settled board, the falling piece and its ghost, then the cleared lines. */
    private void drawTetris(DrawContext context, int x, int boardTop) {
        int cell = gameCell();
        int rows = tetris.rows();
        int columns = tetris.columns();

        for (int renderRow = 0; renderRow < rows; renderRow++) {
            for (int column = 0; column < columns; column++) {
                ZioTetrisShape shape = tetris.shapeAt(column, rows - renderRow - 1);

                if (!shape.isEmpty()) {
                    drawGameCell(context, x + column * cell, boardTop + renderRow * cell, cell, shape.argb());
                }
            }
        }

        ZioTetrisPiece piece = tetris.piece();

        if (piece.shape() != ZioTetrisShape.NoShape) {
            for (int block = 0; block < ZioTetrisPiece.BLOCKS; block++) {
                int blockX = tetris.pieceX() + piece.x(block);
                int blockY = tetris.pieceY() - piece.y(block);

                drawGameCell(context, x + blockX * cell, boardTop + (rows - blockY - 1) * cell, cell,
                    piece.shape().argb());
            }

            // The ghost: the same blocks dropped as far as they go, at the client's fifth-alpha tint.
            int ghostY = tetris.pieceY();

            while (ghostY > 0 && tetris.canPlaceAt(tetris.pieceX(), ghostY - 1)) {
                ghostY--;
            }

            for (int block = 0; block < ZioTetrisPiece.BLOCKS; block++) {
                int blockX = tetris.pieceX() + piece.x(block);
                int blockY = ghostY - piece.y(block);

                drawGameCell(context, x + blockX * cell, boardTop + (rows - blockY - 1) * cell, cell,
                    ZioGameColors.withAlpha(piece.shape().argb(), ZioGameColors.GHOST_ALPHA));
            }
        }

        drawGameScore(context, x, boardTop, tetris.clearedLines(), tetris.gameOver());
    }

    private void drawGameCell(DrawContext context, int x, int y, int size, int argb) {
        context.fill(x, y, x + size, y + size, fade(argb, alpha));
    }

    /**
     * Snake's and Tetris' score: small in the board's corner while the game runs, centred with the
     * restart hint once it is over. Both games showed the same pair of branches.
     */
    private void drawGameScore(DrawContext context, int x, int boardTop, int score, boolean over) {
        if (over) {
            String message = "Score " + score + ". Press R.";

            text(context, message, x + gameWidth() / 2 - textWidth(message) / 2,
                boardTop + gameBoardHeight() / 2, fade(TEXT_ENABLED, alpha));
        } else {
            ZioFontRenderer.drawScaled(context, String.valueOf(score), x + 1.5f, boardTop + 1,
                fade(TEXT_ENABLED, alpha), 0.75f);
        }
    }

    /** Handles a click meant for the game button or the game panel. Returns whether it was one. */
    private boolean clickGame(double x, double y, int button) {
        if (!ZioGames.unlocked()) return false;

        if (gameButton().contains(x, y)) {
            if (button == 0) gameOpen = !gameOpen;

            return true;
        }

        if (!gameOpen) return false;

        int panelX = gameX();
        int panelY = gameY();

        if (x < panelX || x > panelX + gameWidth() || y < panelY || y > panelY + gameHeight()) return false;

        // The title bar is only a title bar; the button under the board is what cycles the games,
        // exactly as the copied panel's own button does.
        if (y < panelY + HEADER_HEIGHT) return true;

        int buttonTop = panelY + HEADER_HEIGHT + gameBoardHeight();

        if (y >= buttonTop) {
            if (button == 0) {
                gameType = gameType.next();
                GuiSounds.click(theme, button);
            }

            return true;
        }

        if (gameType != ZioGameType.MINESWEEPER) return true;

        int cell = gameCell();
        int gridTop = panelY + HEADER_HEIGHT;

        // The minefield keeps its own header row, and clicking it cycles the difficulty.
        if (y < gridTop + cell) {
            if (button == 0) minesweeper.setLevel(minesweeper.level().next());

            return true;
        }

        int column = (int) ((x - panelX) / cell);
        int row = (int) ((y - gridTop) / cell) - 1;

        if (row >= 0 && row < minesweeper.rows() && column >= 0 && column < minesweeper.columns()) {
            if (button == 0) minesweeper.reveal(row, column);
            else if (button == 1) minesweeper.flag(row, column);

            GuiSounds.click(theme, button);
        }

        return true;
    }

    /**
     * The keys the games read, while their panel is open and nothing is being typed: the arrows for
     * Snake, the arrows and D and space for Tetris, and R for both.
     */
    private boolean clickGameKey(int keyCode) {
        if (!gameOpen || gameType == ZioGameType.MINESWEEPER) return false;
        if (searchFocused || presetNameFocused || editingSetting != null || bindingSetting != null) return false;
        if (listSearchRow != null) return false;

        return gameType == ZioGameType.SNAKE ? snake.keyPressed(keyCode) : tetris.keyPressed(keyCode);
    }

    private boolean isOverMinesweeperCell(int column, int row) {
        int cell = gameCell();
        int cellX = gameX() + column * cell;
        int cellY = gameY() + HEADER_HEIGHT + (row + 1) * cell;

        return mouseX >= cellX && mouseX < cellX + cell && mouseY >= cellY && mouseY < cellY + cell;
    }

    /** Helper for a tint that also carries the screen's fade and the board's own opacity. */
    private static int fade(int argb, double factor, int opacity) {
        int faded = fade(argb, factor);
        int alpha = (int) Math.round(((faded >>> 24) & 0xFF) * (opacity / 255.0));

        return (alpha << 24) | (faded & 0xFFFFFF);
    }

    private void drawZoom(DrawContext context) {
        ZioChrome.Rect minus = zoomMinusButton();
        ZioChrome.Rect label = zoomLabelRect();
        ZioChrome.Rect plus = zoomPlusButton();

        context.fill(minus.x(), minus.y(), minus.right(), minus.bottom(), fade(BUTTON, alpha));
        text(context, "-", minus.x() + 5, minus.y() + 2, fade(TEXT_ENABLED, alpha));

        text(context, ZioScale.label(uiScale()), label.x() + 2, label.y() + 2, fade(TEXT, alpha));

        context.fill(plus.x(), plus.y(), plus.right(), plus.bottom(), fade(BUTTON, alpha));
        text(context, "+", plus.x() + 4, plus.y() + 2, fade(TEXT_ENABLED, alpha));
    }

    /** Handles a click on the zoom controls. Returns whether it was one. */
    private boolean clickZoom(double x, double y) {
        if (zoomMinusButton().contains(x, y)) {
            setUiScale(ZioScale.stepped(uiScale(), -1));
            return true;
        }

        if (zoomPlusButton().contains(x, y)) {
            setUiScale(ZioScale.stepped(uiScale(), 1));
            return true;
        }

        return false;
    }

    private void drawPresetButton(DrawContext context) {
        ZioChrome.Rect button = presetsButton();

        context.fill(button.x(), button.y(), button.right(), button.bottom(),
            fade(presetsOpen ? ENABLED : BUTTON, alpha));
        text(context, "Presets", button.x() + 4, button.y() + 2, fade(TEXT_ENABLED, alpha));
    }

    /** The panel: a name field, the presets, then apply, save and delete. */
    private void drawPresets(DrawContext context) {
        int x = presetX();
        int y = presetY();
        int right = x + PRESET_WIDTH;
        int bottom = y + PRESET_HEIGHT;

        context.fill(x, y, right, bottom, fade(BACKGROUND, alpha));
        context.fill(x, y, right, y + 1, fade(COLOR, alpha));
        context.fill(x, y + PRESET_HEIGHT - 1, right, bottom, fade(COLOR, alpha));
        context.fill(x, y, x + 1, bottom, fade(COLOR, alpha));
        context.fill(right - 1, y, right, bottom, fade(COLOR, alpha));

        context.fill(x + 1, y + 1, right - 1, y + PRESET_HEADER - 1, fade(BUTTON, alpha * 0.5));
        text(context, "Presets", x + 5, y + 3, fade(TEXT_ENABLED, alpha));
        text(context, "x", right - 10, y + 3, fade(TEXT, alpha));

        int fieldTop = presetFieldTop();
        int edge = fade(COLOR, alpha * (presetNameFocused ? 1 : 0.5));

        context.fill(x + 4, fieldTop, right - 4, fieldTop + ROW_HEIGHT, fade(BACKGROUND, alpha));
        context.fill(x + 4, fieldTop, right - 4, fieldTop + 1, edge);
        context.fill(x + 4, fieldTop + ROW_HEIGHT - 1, right - 4, fieldTop + ROW_HEIGHT, edge);
        context.fill(x + 4, fieldTop, x + 5, fieldTop + ROW_HEIGHT, edge);
        context.fill(right - 5, fieldTop, right - 4, fieldTop + ROW_HEIGHT, edge);

        boolean cursor = presetNameFocused && ((System.currentTimeMillis() - editStart) / 500) % 2 == 0;
        boolean empty = presetName.isEmpty();

        text(context, (empty ? "New preset" : presetName) + (cursor ? "_" : ""), x + 7, fieldTop + 2,
            fade(empty ? TEXT : TEXT_ENABLED, alpha * (empty ? 0.5 : 1)));

        List<String> names = profileNames();
        int listTop = presetListTop();
        int listBottom = listTop + PRESET_ROWS * ROW_HEIGHT;

        presetScroll = ZioListLayout.clampScroll(presetScroll, names.size());

        scissorOn(context, scaleX(x + 1), scaleY(listTop), scaleX(right - 1), scaleY(listBottom));

        int first = (int) (presetScroll / ROW_HEIGHT);

        for (int slot = 0; slot < PRESET_ROWS; slot++) {
            int index = first + slot;
            if (index >= names.size()) break;

            int rowY = listTop + slot * ROW_HEIGHT - (int) Math.round(presetScroll % ROW_HEIGHT);
            boolean selected = index == presetSelected;

            if (selected) context.fill(x + 1, rowY, right - 1, rowY + ROW_HEIGHT - 1, fade(ENABLED, alpha * 0.8));

            // A preset row is a button as well, so it carries the same gradient.
            ZioPaint.buttonRow(canvas, x, rowY, right - x, ROW_HEIGHT, BUTTON, alpha);

            text(context, trim(names.get(index), PRESET_WIDTH - 12), x + 5, rowY + 2,
                fade(selected ? TEXT_ENABLED : TEXT, alpha));
        }

        scissorOff(context);

        if (names.isEmpty()) text(context, "No presets", x + 5, listTop + 2, fade(TEXT, alpha * 0.6));

        int actions = presetActionsTop();
        int width = presetActionWidth();
        String[] labels = {"Apply", "Save", "Delete"};

        for (int i = 0; i < labels.length; i++) {
            int buttonX = x + 4 + i * (width + 3);

            context.fill(buttonX, actions, buttonX + width, actions + ROW_HEIGHT, fade(BUTTON, alpha));
            text(context, trim(labels[i], width - 6), buttonX + 3, actions + 2, fade(TEXT, alpha));
        }
    }

    private static int presetActionWidth() {
        return (PRESET_WIDTH - 8 - 6) / 3;
    }

    /** Handles a click meant for the preset panel or its button. Returns whether it was one. */
    private boolean clickPresets(double x, double y, int button) {
        if (!presetsOpen) {
            boolean onButton = button == 0 && presetsButton().contains(x, y);

            if (!onButton) return false;

            presetsOpen = true;
            presetNameFocused = false;

            return true;
        }

        int panelX = presetX();
        int right = panelX + PRESET_WIDTH;
        int bottom = presetY() + PRESET_HEIGHT;

        // A click outside the panel closes it and stops there.
        if (x < panelX || x > right || y < presetY() || y > bottom) {
            presetsOpen = false;
            presetNameFocused = false;

            return true;
        }

        presetNameFocused = false;

        if (y < presetY() + PRESET_HEADER) {
            if (x >= right - 14) presetsOpen = false;

            return true;
        }

        int fieldTop = presetFieldTop();

        if (y >= fieldTop && y < fieldTop + ROW_HEIGHT) {
            presetNameFocused = true;
            editStart = System.currentTimeMillis();

            return true;
        }

        List<String> names = profileNames();
        int listTop = presetListTop();

        if (y >= listTop && y < listTop + PRESET_ROWS * ROW_HEIGHT) {
            int index = (int) ((y - listTop + presetScroll) / ROW_HEIGHT);
            if (index >= 0 && index < names.size()) presetSelected = index;

            return true;
        }

        int actions = presetActionsTop();

        if (y >= actions && y < actions + ROW_HEIGHT) {
            int width = presetActionWidth();
            int column = (int) Math.floor((x - (panelX + 4)) / (width + 3));

            switch (column) {
                case 0 -> applyPreset();
                case 1 -> savePreset();
                case 2 -> deletePreset();
                default -> { }
            }

            return true;
        }

        return true;
    }

    private String selectedPresetName() {
        List<String> names = profileNames();

        return presetSelected >= 0 && presetSelected < names.size() ? names.get(presetSelected) : null;
    }

    /** Applies the selected preset through the client's own profile loading. */
    private void applyPreset() {
        Profiles profiles = Profiles.get();
        String name = selectedPresetName();

        if (profiles == null || name == null) return;

        Profile profile = profiles.get(name);
        if (profile != null) profile.load();
    }

    /** Saves the current state as a preset, under the typed name or the next free one. */
    private void savePreset() {
        Profiles profiles = Profiles.get();
        if (profiles == null) return;

        String typed = ZioPresets.normalise(presetName);
        if (!ZioPresets.isValid(typed)) return;

        List<String> taken = new ArrayList<>();
        for (Profile profile : profiles.getAll()) taken.add(profile.name.get());

        String name = ZioPresets.uniqueName(typed, taken);
        Profile profile = profiles.get(name);

        if (profile == null) {
            profile = new Profile();

            // A preset of this GUI is the modules and their settings, plus the HUD those own.
            profile.modules.set(true);
            profile.hud.set(true);
        }

        profile.name.set(name);

        // Adding writes the profile folder and the profile list, so nothing else has to.
        profiles.add(profile);

        presetName = "";
        presetSelected = profileNames().indexOf(name);
        presetScroll = 0;
    }

    /** Deletes the selected preset, folder and all, through the client's own profile removal. */
    private void deletePreset() {
        Profiles profiles = Profiles.get();
        String name = selectedPresetName();

        if (profiles == null || name == null) return;

        Profile profile = profiles.get(name);
        if (profile != null) profiles.remove(profile);

        presetSelected = -1;
        presetScroll = 0;
    }

    // Panels

    private void drawPanel(DrawContext context, Panel panel, int heightLimit) {
        int right = panel.x + PANEL_WIDTH;
        int height = panel.height(heightLimit);
        int bodyHeight = (int) Math.round((height - HEADER_HEIGHT) * panel.expansion);

        // The frosted sheet: the copied client's own 600x450 image, drawn at its own size, clipped to
        // the panel and shifted with the panel's place on screen. Stretching it into the panel — as an
        // ordinary blit would — is what made it read as a smeared gradient instead of a sheet.
        int sheetHeight = HEADER_HEIGHT + bodyHeight;

        if (sheetHeight > 0) {
            scissorOn(context, scaleX(panel.x - 1), scaleY(panel.y - 1),
                scaleX(right + 1), scaleY(panel.y + sheetHeight + 1));

            context.drawTexture(RenderPipelines.GUI_TEXTURED, AERO_TEXTURE,
                ZioPanelPaint.aeroLeft(panel.x), panel.y, 0, 0,
                ZioPanelPaint.AERO_WIDTH, ZioPanelPaint.AERO_HEIGHT,
                ZioPanelPaint.AERO_WIDTH, ZioPanelPaint.AERO_HEIGHT,
                (ZioPanelPaint.aeroAlpha(alpha) << 24) | 0xFFFFFF);

            scissorOff(context);
        }

        panel.bodyTop = panel.y + HEADER_HEIGHT;
        panel.bodyBottom = panel.y + HEADER_HEIGHT + Math.max(0, bodyHeight);

        if (bodyHeight > 0) {
            int top = panel.y + HEADER_HEIGHT;

            context.fill(panel.x, top, right, top + bodyHeight, fade(BACKGROUND, alpha));

            scissorOn(context, scaleX(panel.x - 1), scaleY(top), scaleX(right + 1), scaleY(top + bodyHeight));

            for (Row row : panel.rows) {
                if (!row.matches()) continue;

                int rowY = (int) Math.round(panel.y + row.offset);
                if (rowY + ROW_HEIGHT <= top || rowY >= top + bodyHeight) continue;

                drawRow(context, panel, row, rowY);
            }

            scissorOff(context);

            drawScrollFade(context, panel, top, bodyHeight);
        }

        // Title bar, then its title, exactly as Panel does it: the header's own vertical offset (half a
        // pixel when the copied client's line is off, zero here since the line is always drawn) and the
        // title two pixels in from the edge.
        float headerOffset = ZioPanelPaint.headerVerticalOffset(true);

        context.fill(panel.x, panel.y, right, Math.round(panel.y + HEADER_HEIGHT + headerOffset),
            fade(COLOR, panel.headerAlpha * alpha));

        float titleY = panel.y + HEADER_HEIGHT / 2f - ZioFontRenderer.lineHeight() / 2f + headerOffset;
        text(context, panel.title, panel.x + 2, titleY, fade(0xFFFFFFFF, panel.headerAlpha * alpha));

        // One pixel outline around the whole panel, up to where the body actually reaches and tinted
        // with the header's alpha, as the copied client's drawRectOutline is.
        int outline = fade(COLOR, panel.headerAlpha * alpha);
        int bottom = panel.y + ZioPanelPaint.outlineHeight(height, panel.expansion, HEADER_HEIGHT) + 1;

        context.fill(panel.x - 1, panel.y - 1, right, panel.y, outline);
        context.fill(panel.x - 1, bottom - 1, right, bottom, outline);
        context.fill(panel.x - 1, panel.y - 1, panel.x, bottom, outline);
        context.fill(right, panel.y - 1, right + 1, bottom, outline);
    }

    /**
     * The screen behind the panels: the copied client's tint over everything, then its vertical
     * gradient from the gradient colour at zero alpha down to the same colour at its own alpha. Without
     * it the panels float on a flat background and the aero sheet reads as a rectangle.
     */
    private void drawBackgroundOverlay(DrawContext context) {
        int width = viewWidth();
        int height = viewHeight();

        context.fill(0, 0, width, height, fade(ZioPanelPaint.TINT, alpha));
        context.fillGradient(0, 0, width, height,
            fade(ZioPanelPaint.GRADIENT & 0x00FFFFFF, alpha), fade(ZioPanelPaint.GRADIENT, alpha));
    }

    private ZioChrome.Rect fxButton() {
        return ZioChrome.fxButton(viewWidth(), viewHeight());
    }

    private void drawFxButton(DrawContext context) {
        ZioChrome.Rect button = fxButton();

        context.fill(button.x(), button.y(), button.right(), button.bottom(),
            fade(effects ? ENABLED : BUTTON, alpha));
        text(context, "Fx", button.x() + 4, button.y() + 2, fade(TEXT_ENABLED, alpha));
    }

    /** Turns the panel blur and bloom on and off. */
    private boolean clickFx(double x, double y, int button) {
        if (!fxButton().contains(x, y)) return false;

        if (button == 0) effects = !effects;

        return true;
    }

    private void drawRow(DrawContext context, Panel panel, Row row, int y) {
        boolean active = row.module.isActive();
        double progress = row.enabled;

        // Enabled fill: from one pixel in to the width times the animation.
        if (progress > 0.01) {
            double end = panel.x + (PANEL_WIDTH - 1) * progress;
            context.fill(panel.x + 1, y, (int) Math.round(end), y + ROW_HEIGHT - 1,
                fade(ENABLED, alpha));
        }

        // The button gradient every row carries while "Gradient" is on.
        ZioPaint.buttonRow(canvas, panel.x, y, PANEL_WIDTH, ROW_HEIGHT, BUTTON, alpha);

        // The search's flash goes over both, as the copied client draws it last.
        drawRowHighlight(context, panel, row, y);

        int textY = (int) Math.round(y + TEXT_Y - row.hover);
        String title = row.module.title;

        drawScrolledText(context, panel, y, title, panel.x + 3, textY,
            fade(active ? TEXT_ENABLED : TEXT, alpha), row.textScroll.offset(ZioFontRenderer.widthF(title), row.hovered));

        // The bind, in the copied client's half sized brackets right after the name.
        if (row.module.keybind.isSet()) {
            ZioPaint.status(canvas, panel.x + 3 + ZioFontRenderer.widthF(title), textY + 1,
                row.module.keybind.toString(), fade(TEXT, alpha));
        }

        // The + / - indicator on the right edge, when the module has settings.
        if (!row.settingsRows.isEmpty()) {
            String indicator = row.open ? "-" : "+";
            int x = panel.x + PANEL_WIDTH - 4 - textWidth(indicator);
            text(context, indicator, x, textY, fade(TEXT, alpha));
        }

        if (row.open || row.openAnim > 0.01) {
            int top = y + ROW_HEIGHT;
            int height = (int) Math.round(row.blockHeight() * row.openAnim);

            if (height > 0) {
                scissorOn(context, scaleX(panel.x), scaleY(top), scaleX(panel.x + PANEL_WIDTH), scaleY(top + height));

                for (SettingRow setting : row.settingsRows) {
                    int settingY = (int) Math.round(top + setting.offset);
                    if (settingY + setting.height() <= top || settingY >= top + height) continue;

                    drawSetting(context, panel, setting, settingY);

                    if (setting.pickerOpen) drawColourPicker(context, panel, setting, settingY + ROW_HEIGHT);
                    if (setting.listOpen) drawList(context, panel, setting, settingY + ROW_HEIGHT);
                }

                // The bind line the copied client keeps at the foot of an open module.
                int bindY = (int) Math.round(top + row.settingsHeight());
                row.bindTop = bindY;

                if (bindY >= top && bindY < top + height) drawBindRow(context, panel, row, bindY);

                scissorOff(context);
            }
        }
    }

    /**
     * One line of a list, clipped to its row when it is being scrolled sideways. The copied client scrolls
     * a name that does not fit rather than cutting it at the panel edge, and only scissors it while the
     * scroll is not at rest — a static list costs one text run, not two.
     */
    private void drawScrolledText(DrawContext context, Panel panel, int rowY, String text, float x, float textY,
                                  int colour, float scroll) {
        boolean clipped = scroll > 0.01f;

        if (clipped) {
            scissorOn(context, scaleX(panel.x + 1), scaleY(rowY), scaleX(panel.x + PANEL_WIDTH - 1),
                scaleY(rowY + ROW_HEIGHT));
        }

        text(context, text, x - scroll, textY, colour);

        if (clipped) scissorOff(context);
    }

    /**
     * The bind line at the foot of an open module, with the copied client's own hover lift: the
     * module's key on the left, {@code Listen} ellipsis while it waits for one, and a right click
     * clearing it where that client cycles its toggle/hold mode.
     */
    private void drawBindRow(DrawContext context, Panel panel, Row row, int y) {
        int textY = (int) Math.round(y + TEXT_Y - row.bindHover);

        ZioPaint.bindRow(canvas, panel.x, textY, ZioBindModes.of(moduleBindId(row.module)),
            row.module.keybind.toString(), row == bindingRow, row.bindHovered, fade(TEXT, alpha));
    }

    /** The name a module's own bind stores its mode under. */
    private static String moduleBindId(Module module) {
        return module.name;
    }

    /** The name a setting's bind stores its mode under: its module, then the setting. */
    private static String settingBindId(Module module, Setting<?> setting) {
        return module.name + "/" + setting.name;
    }

    private void drawSetting(DrawContext context, Panel panel, SettingRow row, int y) {
        int textY = (int) Math.round(y + TEXT_Y - row.hover);
        int colour = fade(TEXT, alpha);

        // One choice of an open list: centred, filled in when it is the one in use.
        if (row.option != null) {
            String label = row.optionLabel();

            text(context, label, panel.x + PANEL_WIDTH / 2 - textWidth(label) / 2, textY, colour);

            // The outline closes the box under the last choice, over the bar and the labels.
            if (row.optionIsLast()) {
                outlineRect(context, ZioEnumBox.outline(panel.x, row.enumRow.top, PANEL_WIDTH, ROW_HEIGHT,
                    row.optionCount()), fade(COLOR, alpha));
            }

            return;
        }

        // A group of settings: its name, and the indicator that folds it.
        if (row.group != null) {
            text(context, row.group.name, panel.x + 3, textY, colour);

            if (row.hasSettings) {
                String indicator = row.group.sectionExpanded ? "-" : "+";
                text(context, indicator, panel.x + PANEL_WIDTH - 4 - textWidth(indicator), textY, colour);
            }

            return;
        }

        Setting<?> setting = row.setting;
        String text;

        if (setting instanceof KeybindSetting keybindSetting) {
            Keybind bind = keybindSetting.get();

            drawScrolledText(context, panel, y, setting.title, panel.x + 4, textY, colour,
                row.textScroll.offset(ZioFontRenderer.widthF(setting.title), row.hovered));

            // The mode and the key after the name, as the copied client's keybind line writes them,
            // in its half sized brackets.
            ZioPaint.bindStatus(canvas, panel.x + 4 + ZioFontRenderer.widthF(setting.title) + 2, textY + 1,
                ZioBindModes.of(settingBindId(row.module, setting)), bind.toString(), row == bindingSetting,
                fade(TEXT, alpha));

            return;
        }

        if (isListSetting(setting)) {
            String indicator = row.listOpen ? "-" : "+";
            String label = setting.title + " (" + listSize(setting) + ")";

            text(context, indicator, panel.x + PANEL_WIDTH - 4 - textWidth(indicator), textY, colour);
            drawScrolledText(context, panel, y, label, panel.x + 4, textY, colour,
                row.textScroll.offset(ZioFontRenderer.widthF(label), row.hovered));
            return;
        }

        if (setting instanceof ColorSetting colourSetting) {
            SettingColor value = colourSetting.get();

            // The preview sits at the right edge of the row, eight by eight, as it does there, on the
            // client's own checkerboard when the colour is not opaque.
            int preview = panel.x + PANEL_WIDTH - 11;

            if (value.a < 255) {
                for (int column = 0; column < 2; column++) {
                    for (int line = 0; line < 2; line++) {
                        int cell = ZioColorMarker.checker((column + line) % 2 == 0);

                        context.fill(preview + column * 4, textY + line * 4, preview + column * 4 + 4,
                            textY + line * 4 + 4, fade(cell, alpha));
                    }
                }
            }

            context.fill(preview, textY, preview + 8, textY + 8,
                fade((value.a << 24) | (value.r << 16) | (value.g << 8) | value.b, alpha));

            String label = setting.title;

            drawScrolledText(context, panel, y, label, panel.x + 4, textY, colour,
                row.textScroll.offset(ZioFontRenderer.widthF(label), row.hovered));
            return;
        }

        if (setting instanceof BoolSetting bool) {
            if (bool.get() || row.fill > 0.01) {
                // Two pixels in on the left, one short of the right edge, and the whole line high: the
                // copied client insets its toggle by half a pixel, which is ten of the eleven rows.
                double end = panel.x + Math.max(PANEL_WIDTH * row.fill - 1, 2);

                context.fill(panel.x + 2, y, (int) Math.round(end), y + ROW_HEIGHT - 1, fade(COLOR, alpha));
            }

            text = setting.title;
        } else if (setting instanceof IntSetting number) {
            text = editedText(row, setting.title + ": " + number.get());

            if (row.fill > 0.01) {
                double end = panel.x + clamp(row.fill, 0, PANEL_WIDTH - 3) + 2;

                context.fill(panel.x + 2, y, (int) Math.round(end), y + ROW_HEIGHT - 1, fade(COLOR, alpha));
            }
        } else if (setting instanceof DoubleSetting number) {
            text = editedText(row, setting.title + ": " + String.format("%.2f", number.get()));

            if (row.fill > 0.01) {
                double end = panel.x + clamp(row.fill, 0, PANEL_WIDTH - 3) + 2;

                context.fill(panel.x + 2, y, (int) Math.round(end), y + ROW_HEIGHT - 1, fade(COLOR, alpha));
            }
        } else if (setting instanceof EnumSetting<?> option) {
            // The copied client keeps an accent bar behind an enumeration, full width while the
            // setting is available — the bar the value is read against.
            ZioPaint.enumBar(canvas, panel.x, y, ROW_HEIGHT, PANEL_WIDTH, row.fill, fade(COLOR, alpha));

            // Open, a second bar slides to the chosen choice and the box gets its outline.
            if (row.dropdownOpen) {
                ZioEnumBox.Rect bar = ZioEnumBox.selection(panel.x, y, PANEL_WIDTH, ROW_HEIGHT, row.slide);

                context.fill(bar.left(), bar.top(), bar.right(), bar.bottom(), fade(COLOR, alpha));
            }

            text = setting.title + ": " + option.get();
            if (row.dropdownOpen) text += " ^";
        } else if (setting instanceof StringSetting string) {
            text = editedText(row, setting.title + ": " + string.get());

            // A text field carries the full width bar behind it, which is how it reads as editable.
            context.fill(panel.x + 2, y, panel.x + PANEL_WIDTH - 1, y + ROW_HEIGHT - 1, fade(COLOR, alpha));
        } else if (setting instanceof BlockPosSetting position) {
            BlockPos value = position.get();

            text = editedText(row, setting.title + ": " + value.getX() + ", " + value.getY() + ", " + value.getZ());
        } else {
            text = setting.title;
        }

        drawScrolledText(context, panel, y, text, panel.x + 4, textY, colour,
            row.textScroll.offset(ZioFontRenderer.widthF(text), row.hovered));
    }

    /**
     * The opened colour picker: saturation and brightness, then hue, then alpha.
     *
     * <p>The square is drawn one column at a time, each a straight gradient from the hue mixed with
     * white down to black, which is the same surface the copied client fills in.
     */
    private void drawColourPicker(DrawContext context, Panel panel, SettingRow row, int y) {
        int x = panel.x + COLOUR_PADDING;
        int size = COLOUR_SQUARE;
        int squareTop = y + COLOUR_PADDING;
        int hueColour = 0xFF000000 | java.awt.Color.HSBtoRGB(row.hsb[0], 1f, 1f);

        for (int i = 0; i < size; i++) {
            float saturation = i / (float) (size - 1);
            context.fillGradient(x + i, squareTop, x + i + 1, squareTop + size,
                mix(0xFFFFFFFF, hueColour, saturation), 0xFF000000);
        }

        // The square's marker: three pixels at rest and four while the square is held, a black border
        // over a light colour, and the chosen colour darkened inside — markerExpansion, markerContrast.
        int markerX = x + (int) Math.round(row.markerSatX);
        int markerY = squareTop + (int) Math.round(row.markerBriY);
        int half = ZioColorMarker.half(row.markerExpansion);
        int chosen = 0xFF000000 | (java.awt.Color.HSBtoRGB(row.hsb[0], row.hsb[1], row.hsb[2]) & 0xFFFFFF);

        context.fill(markerX - half, markerY - half, markerX + half + 1, markerY + half + 1,
            ZioColorMarker.outline(ZioColorMarker.luminance(chosen), row.markerContrast));
        context.fill(markerX - half + 1, markerY - half + 1, markerX + half, markerY + half,
            ZioColorMarker.inner(chosen));

        int hueTop = squareTop + size + COLOUR_PADDING;

        for (int i = 0; i < size; i++) {
            int colour = 0xFF000000 | java.awt.Color.HSBtoRGB(i / (float) (size - 1), 1f, 1f);
            context.fill(x + i, hueTop, x + i + 1, hueTop + COLOUR_SLIDER, colour);
        }

        // A slider's marker keeps the accent until it is held, then turns white with a white core.
        int hueMarker = x + (int) Math.round(row.markerHueX);

        context.fill(hueMarker - 1, hueTop - 1, hueMarker + 2, hueTop + COLOUR_SLIDER + 1,
            ZioColorMarker.sliderOutline(row == draggingSetting && row.pickerZone == 2));
        context.fill(hueMarker, hueTop, hueMarker + 1, hueTop + COLOUR_SLIDER, 0xFFFFFFFF);

        // Alpha sits over a checkerboard, so a see through colour reads as one.
        int alphaTop = hueTop + COLOUR_SLIDER + COLOUR_PADDING;

        for (int cx = 0; cx < size; cx += 4) {
            for (int cy = 0; cy < COLOUR_SLIDER; cy += 4) {
                boolean light = ((cx / 4) + (cy / 4)) % 2 == 0;

                context.fill(x + cx, alphaTop + cy, Math.min(x + cx + 4, x + size),
                    Math.min(alphaTop + cy + 4, alphaTop + COLOUR_SLIDER), ZioColorMarker.alphaChecker(light));
            }
        }

        // The bar runs from the colour at full alpha on the left to the same colour clear on the right.
        int rgb = row.packedColour() & 0xFFFFFF;

        for (int i = 0; i < size; i++) {
            context.fill(x + i, alphaTop, x + i + 1, alphaTop + COLOUR_SLIDER,
                (ZioColorMarker.alphaAt(size, i) << 24) | rgb);
        }

        int alphaMarker = x + (int) Math.round(row.markerAlphaX);

        context.fill(alphaMarker - 1, alphaTop - 1, alphaMarker + 2, alphaTop + COLOUR_SLIDER + 1,
            ZioColorMarker.sliderOutline(row == draggingSetting && row.pickerZone == 3));
        context.fill(alphaMarker, alphaTop, alphaMarker + 1, alphaTop + COLOUR_SLIDER, 0xFFFFFFFF);
    }

    private static int mix(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 0xFF) * (1 - t) + ((to >> 16) & 0xFF) * t);
        int g = Math.round(((from >> 8) & 0xFF) * (1 - t) + ((to >> 8) & 0xFF) * t);
        int b = Math.round((from & 0xFF) * (1 - t) + (to & 0xFF) * t);

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** A one pixel square marker, three pixels half size. */
    /** A one pixel rectangle, as the copied client's {@code drawRectOutline} draws it. */
    private static void outlineRect(DrawContext context, ZioEnumBox.Rect rect, int colour) {
        context.fill(rect.left(), rect.top(), rect.right(), rect.top() + 1, colour);
        context.fill(rect.left(), rect.bottom() - 1, rect.right(), rect.bottom(), colour);
        context.fill(rect.left(), rect.top(), rect.left() + 1, rect.bottom(), colour);
        context.fill(rect.right() - 1, rect.top(), rect.right(), rect.bottom(), colour);
    }

    private static void outline(DrawContext context, int x, int y, int half, int colour) {
        context.fill(x - half, y - half, x + half + 1, y - half + 1, colour);
        context.fill(x - half, y + half, x + half + 1, y + half + 1, colour);
        context.fill(x - half, y - half, x - half + 1, y + half + 1, colour);
        context.fill(x + half, y - half, x + half + 1, y + half + 1, colour);
    }

    // List settings

    private static boolean isListSetting(Setting<?> setting) {
        return setting instanceof BlockListSetting || setting instanceof ItemListSetting
            || setting instanceof EntityTypeListSetting || setting instanceof StringListSetting
            || setting instanceof ModuleListSetting || setting instanceof StorageBlockListSetting;
    }

    private static int listSize(Setting<?> setting) {
        if (setting instanceof BlockListSetting blockSetting) return blockSetting.get().size();
        if (setting instanceof ItemListSetting itemSetting) return itemSetting.get().size();
        if (setting instanceof EntityTypeListSetting entitySetting) return entitySetting.get().size();
        if (setting instanceof StringListSetting stringSetting) return stringSetting.get().size();
        if (setting instanceof ModuleListSetting moduleSetting) return moduleSetting.get().size();
        if (setting instanceof StorageBlockListSetting storageSetting) return storageSetting.get().size();
        return 0;
    }

    /**
     * Every choice a list setting offers, sorted by what it reads as.
     *
     * <p>Blocks, items and entities are read from the game's registries and kept to what the setting
     * allows; modules come from the client; a free text list offers its own entries, which is why
     * Enter in its search field adds one.
     */
    private static List<ListEntry> candidates(Setting<?> setting) {
        List<ListEntry> entries = new ArrayList<>();

        if (setting instanceof BlockListSetting blockSetting) {
            for (Block block : Registries.BLOCK) {
                if (!blockSetting.filter.test(block)) continue;

                entries.add(new ListEntry(block, block.getName().getString(),
                    Registries.BLOCK.getId(block).toString()));
            }
        } else if (setting instanceof ItemListSetting itemSetting) {
            for (Item item : Registries.ITEM) {
                if (!itemSetting.filter.test(item)) continue;

                entries.add(new ListEntry(item, item.getName().getString(),
                    Registries.ITEM.getId(item).toString()));
            }
        } else if (setting instanceof EntityTypeListSetting entitySetting) {
            for (EntityType<?> type : Registries.ENTITY_TYPE) {
                if (!entitySetting.filter.test(type)) continue;

                entries.add(new ListEntry(type, type.getName().getString(),
                    Registries.ENTITY_TYPE.getId(type).toString()));
            }
        } else if (setting instanceof ModuleListSetting) {
            for (Module module : Modules.get().getAll()) {
                entries.add(new ListEntry(module, module.title, module.name));
            }
        } else if (setting instanceof StorageBlockListSetting) {
            // The list the setting was written around: the block entities a stash can hide in.
            for (BlockEntityType<?> type : StorageBlockListSetting.STORAGE_BLOCKS) {
                String id = BlockEntityType.getId(type).toString();
                entries.add(new ListEntry(type, ZioListLayout.readableId(id), id));
            }
        } else if (setting instanceof StringListSetting stringSetting) {
            for (String value : stringSetting.get()) {
                entries.add(new ListEntry(value, value, value));
            }
        }

        entries.sort(Comparator.comparing(entry -> entry.label().toLowerCase(Locale.ROOT)));

        return entries;
    }

    /** The choices left after the internal search, which also matches registry names. */
    private static List<ListEntry> visibleEntries(SettingRow row, List<ListEntry> all) {
        return ZioListLayout.filtered(all, ListEntry::label, ListEntry::id, row.listSearch);
    }

    private static boolean isSelected(Setting<?> setting, Object value) {
        if (setting instanceof BlockListSetting blockSetting) return ZioListLayout.selected(blockSetting.get(), value);
        if (setting instanceof ItemListSetting itemSetting) return ZioListLayout.selected(itemSetting.get(), value);
        if (setting instanceof EntityTypeListSetting entitySetting) return ZioListLayout.selected(entitySetting.get(), value);
        if (setting instanceof StringListSetting stringSetting) return ZioListLayout.selected(stringSetting.get(), value);
        if (setting instanceof ModuleListSetting moduleSetting) return ZioListLayout.selected(moduleSetting.get(), value);
        if (setting instanceof StorageBlockListSetting storageSetting) return ZioListLayout.selected(storageSetting.get(), value);
        return false;
    }

    /** Checks or unchecks one entry, writing the new value straight into the setting. */
    @SuppressWarnings("unchecked")
    private static void toggleEntry(Setting<?> setting, Object value) {
        if (setting instanceof BlockListSetting blockSetting) {
            blockSetting.set(ZioListLayout.toggled(blockSetting.get(), (Block) value));
        } else if (setting instanceof ItemListSetting itemSetting) {
            itemSetting.set(ZioListLayout.toggled(itemSetting.get(), (Item) value));
        } else if (setting instanceof EntityTypeListSetting entitySetting) {
            Set<EntityType<?>> set = new HashSet<>(entitySetting.get());
            if (!set.remove(value)) set.add((EntityType<?>) value);
            entitySetting.set(set);
        } else if (setting instanceof StringListSetting stringSetting) {
            stringSetting.set(ZioListLayout.toggled(stringSetting.get(), (String) value));
        } else if (setting instanceof ModuleListSetting moduleSetting) {
            moduleSetting.set(ZioListLayout.toggled(moduleSetting.get(), (Module) value));
        } else if (setting instanceof StorageBlockListSetting storageSetting) {
            storageSetting.set(ZioListLayout.toggled(storageSetting.get(), (BlockEntityType<?>) value));
        }
    }

    private static int listSearchTop(SettingRow row) {
        return ZioListLayout.listSearchTop(row.top);
    }

    private static int listEntriesTop(SettingRow row) {
        return ZioListLayout.listEntriesTop(row.top);
    }

    private static double listMaxScroll(List<ListEntry> entries) {
        return ZioListLayout.maxScroll(entries.size());
    }

    /** Folds a list back up and drops its search. */
    private void closeList(SettingRow row) {
        row.listOpen = false;
        row.listSearchFocused = false;
        row.listSearch = "";
        row.listScroll = 0;
        row.listCandidates = null;

        if (listSearchRow == row) listSearchRow = null;
    }

    /** The open list whose entries the pointer is over, if any. */
    private SettingRow openListAt(double mouseX, double mouseY) {
        Panel panel = panelAt(mouseX, mouseY);
        if (panel == null) return null;

        SettingRow row = panel.settingAt(mouseX, mouseY, viewHeight() - HEIGHT_MARGIN);
        return row != null && row.listOpen ? row : null;
    }

    /** A label cut to a width, with an ellipsis when it does not fit. */
    private static String trim(String value, int width) {
        if (textWidth(value) <= width) return value;

        StringBuilder builder = new StringBuilder();

        for (char character : value.toCharArray()) {
            if (textWidth(builder.toString() + character + "...") > width) break;
            builder.append(character);
        }

        return builder + "...";
    }

    /** The unfolded list: its search field, then the entries that survive it, scrolled in a box. */
    private void drawList(DrawContext context, Panel panel, SettingRow row, int y) {
        if (row.listCandidates == null) row.listCandidates = candidates(row.setting);

        List<ListEntry> entries = visibleEntries(row, row.listCandidates);
        row.listScroll = ZioListLayout.clampScroll(row.listScroll, entries.size());

        int x = panel.x + COLOUR_PADDING;
        int right = panel.x + PANEL_WIDTH - COLOUR_PADDING;
        int searchTop = listSearchTop(row);

        context.fill(x, searchTop, right, searchTop + LIST_SEARCH_HEIGHT, fade(BACKGROUND, alpha));

        int edge = fade(COLOR, alpha * (row.listSearchFocused ? 1 : 0.5));
        context.fill(x, searchTop, right, searchTop + 1, edge);
        context.fill(x, searchTop + LIST_SEARCH_HEIGHT - 1, right, searchTop + LIST_SEARCH_HEIGHT, edge);
        context.fill(x, searchTop, x + 1, searchTop + LIST_SEARCH_HEIGHT, edge);
        context.fill(right - 1, searchTop, right, searchTop + LIST_SEARCH_HEIGHT, edge);

        boolean cursor = row.listSearchFocused && ((System.currentTimeMillis() - editStart) / 500) % 2 == 0;
        boolean empty = row.listSearch.isEmpty();
        text(context, (empty ? "Search" : row.listSearch) + (cursor ? "_" : ""), x + 4, searchTop + 3,
            fade(empty ? TEXT : TEXT_ENABLED, alpha * (empty ? 0.5 : 1)));

        int top = listEntriesTop(row);
        int bottom = top + LIST_ROWS * ROW_HEIGHT;

        scissorOn(context, scaleX(x), scaleY(top), scaleX(right), scaleY(bottom));

        int first = (int) (row.listScroll / ROW_HEIGHT);

        for (int slot = 0; slot <= LIST_ROWS; slot++) {
            int index = first + slot;
            if (index >= entries.size()) break;

            int rowY = top + slot * ROW_HEIGHT - (int) Math.round(row.listScroll % ROW_HEIGHT);
            ListEntry entry = entries.get(index);
            boolean selected = isSelected(row.setting, entry.value());

            // A row that comes back into the list fades in rather than appearing whole.
            double fadeIn = ZioRowFade.next(row.listFade.getOrDefault(entry.value(), 0.0), true, ZioAnim.fps());

            row.listFade.put(entry.value(), fadeIn);

            if (selected) {
                context.fill(x, rowY, right, rowY + ROW_HEIGHT - 1, fade(ENABLED, alpha * 0.6 * fadeIn));
            }

            int boxTop = rowY + 3;
            int boxRight = x + 11;

            if (selected) context.fill(x + 4, boxTop, boxRight, boxTop + 7, fade(COLOR, alpha * fadeIn));
            context.fill(x + 4, boxTop, boxRight, boxTop + 1, fade(COLOR, alpha * fadeIn));
            context.fill(x + 4, boxTop + 6, boxRight, boxTop + 7, fade(COLOR, alpha * fadeIn));
            context.fill(x + 4, boxTop, x + 5, boxTop + 7, fade(COLOR, alpha * fadeIn));
            context.fill(boxRight - 1, boxTop, boxRight, boxTop + 7, fade(COLOR, alpha * fadeIn));

            text(context, trim(entry.label(), right - (x + 15)), x + 15, rowY + 2,
                fade(selected ? TEXT_ENABLED : TEXT, alpha * fadeIn));
        }

        scissorOff(context);

        if (row.listScroll > 0.5) {
            context.fillGradient(x, top, right, top + 6, fade(COLOR, 0.35 * alpha), fade(COLOR, 0));
        }

        if (row.listScroll < listMaxScroll(entries) - 0.5) {
            context.fillGradient(x, bottom - 6, right, bottom, fade(COLOR, 0), fade(COLOR, 0.35 * alpha));
        }
    }

    /**
     * The search of the copied client, drawn the way it draws it.
     *
     * <p>There is no field and no box: while the search has the keyboard the screen is dimmed by a
     * fifth and the query sits in the middle of it, its completion in grey and a caret after it;
     * while it does not, a hint pulses in the bottom right corner. Ctrl + F opens it.
     */
    private void drawSearch(DrawContext context) {
        long now = System.currentTimeMillis();
        searchFocus.setTarget(now, searchFocused, SEARCH_FOCUS_MILLIS);

        ZioPaint.searchDimmer(canvas, viewWidth(), viewHeight(), searchFocus.value(now), alpha);

        if (searchFocused) {
            boolean caret = (now - searchStart) / SEARCH_CARET_MILLIS % 2 == 0;

            ZioPaint.searchQuery(canvas, viewWidth(), viewHeight(), searchQuery, searchCompletion, caret,
                fade(TEXT_ENABLED, alpha), fade(SEARCH_COMPLETION, alpha));

            return;
        }

        ZioPaint.searchHint(canvas, viewWidth(), viewHeight(), "Ctrl + F to activate search", hintMargin(),
            fade(pulseColour(), alpha));
    }

    /**
     * The margin the hint keeps from the corner: the HUD's own safe margin, which is the value the
     * copied client reads from its HUD module.
     */
    private static int hintMargin() {
        return Math.max(1, ZioHudMarginStore.current()[0]);
    }

    /**
     * The description of the setting under the pointer, or null when it has none.
     *
     * <p>{@code SettingComponent.render} asks the copied client's translation table for
     * {@code descriptions.settings.<module>.<setting>}; this addon has no such table, so the
     * description the setting already carries is shown instead — the same text for the same purpose.
     */
    private String settingDescription(Panel panel) {
        if (panel == null) return null;

        SettingRow row = panel.settingAt(mouseX, mouseY, viewHeight() - HEIGHT_MARGIN);
        if (row == null || row.setting == null) return null;

        return row.setting.description;
    }

    /** The copied client's winter flakes: above the backdrop, under the panels, as they draw them. */
    private void drawSnow(DrawContext context) {
        if (!ZioSeason.isWinter(LocalDate.now().getMonthValue())) return;

        snow.advance(ZioAnim.frameFactor(ZioAnim.fps()));

        ZioPaint.snow(canvas, viewWidth(), viewHeight(), snow.flakes(), alpha);
    }

    /**
     * The hint's colour: the copied client pulses it from grey to white and back over two seconds,
     * with a quarter second of offset, so a screen opened at the same moment does not always start on
     * the same shade.
     */
    private static int pulseColour() {
        double phase = (System.currentTimeMillis() + SEARCH_HINT_OFFSET) % SEARCH_HINT_MILLIS
            / (double) SEARCH_HINT_MILLIS;
        double half = phase > 0.5 ? (phase - 0.5) * 2 : phase * 2;
        double grey = phase > 0.5 ? 1 - half : half;
        int channel = (int) Math.round(255 - (255 - SEARCH_HINT_GREY) * grey);

        return 0xFF000000 | channel << 16 | channel << 8 | channel;
    }

    /** The rest of the first module name the query starts, as the copied client completes it. */
    private void updateSearchCompletion() {
        searchCompletion = "";

        if (searchQuery.isEmpty()) return;

        for (Panel panel : panels) {
            for (Row row : panel.rows) {
                String title = row.module.title;

                if (title.length() > searchQuery.length()
                    && title.toLowerCase(Locale.ROOT).startsWith(searchQuery)) {
                    searchCompletion = title.substring(searchQuery.length());

                    return;
                }
            }
        }
    }

    /** The row of the module the query names exactly, or null when it names none. */
    private Row rowNamed(String name) {
        for (Panel panel : panels) {
            for (Row row : panel.rows) {
                if (row.module.title.equalsIgnoreCase(name)) return row;
            }
        }

        return null;
    }

    /**
     * Enter on a search: opens the module the query names and flashes its row, then puts the search
     * away, which is what the copied client's confirm does.
     */
    private void confirmSearch() {
        Row row = rowNamed(searchQuery);

        if (row != null) {
            openRow(row);
            row.highlightStartedAt = System.currentTimeMillis();
        }

        searchFocused = false;
        searchQuery = "";
        searchCompletion = "";
    }

    /**
     * Opens a row, or folds it, keeping at most three open: the copied client closes the oldest one
     * when a fourth is opened rather than refusing the fourth.
     */
    private void toggleRow(Row row) {
        if (row.open) {
            row.open = false;
            openRows.clear();
            openRows.addAll(ZioOpenRows.closed(openRows, row));

            if (bindingRow == row) bindingRow = null;

            return;
        }

        openRow(row);
    }

    private void openRow(Row row) {
        List<Row> previous = new ArrayList<>(openRows);

        openRows.clear();
        openRows.addAll(ZioOpenRows.opened(previous, row, MAX_OPEN_ROWS));

        // Whatever fell off the list is folded now, which is the fourth-opened rule in action.
        for (Row other : previous) {
            if (!openRows.contains(other)) other.open = false;
        }

        row.open = true;
    }

    /**
     * The red flash the copied client gives the row its search opened: up and down four times over
     * 150 ms each, then gone.
     */
    private void drawRowHighlight(DrawContext context, Panel panel, Row row, int y) {
        if (row.highlightStartedAt == 0) return;

        long elapsed = System.currentTimeMillis() - row.highlightStartedAt;
        long blink = elapsed / HIGHLIGHT_MILLIS;

        if (blink >= HIGHLIGHT_BLINKS) {
            row.highlightStartedAt = 0;

            return;
        }

        double phase = (elapsed % HIGHLIGHT_MILLIS) / (double) HIGHLIGHT_MILLIS;
        double flash = blink % 2 == 0 ? 1 - phase : phase;

        ZioPaint.highlight(canvas, panel.x + 1, y, panel.x + PANEL_WIDTH - 1, y + ROW_HEIGHT - 1, flash,
            alpha);
    }

    /**
     * A fade at each end, so a cut off list reads as scrolled rather than as broken: ten and a half
     * pixels of the accent colour at three tenths, each over the copied client's 200 ms.
     */
    private void drawScrollFade(DrawContext context, Panel panel, int top, int bodyHeight) {
        double expansion = Math.min(1, panel.expansion);
        double bottomFade = panel.bottomFade * expansion;
        double topFade = panel.topFade * expansion;

        if (bottomFade > 0.01) {
            context.fillGradient(panel.x, top + bodyHeight - 10, panel.x + PANEL_WIDTH, top + bodyHeight,
                fade(COLOR, 0), fade(COLOR, 0.3 * bottomFade * alpha));
        }

        if (topFade > 0.01) {
            context.fillGradient(panel.x, top, panel.x + PANEL_WIDTH, top + 10,
                fade(COLOR, 0.3 * topFade * alpha), fade(COLOR, 0));
        }
    }

    /**
     * Steps the point the tooltip hangs from: it does not sit on the pointer, it chases it at 2.5 and
     * slides in behind the mouse. The step belongs to the frame rather than to the drawing, so the
     * blur pass and the drawing below use the same point.
     */
    /** Steps every panel's animation, its scroll and the offset of every row: what the tooltip reads. */
    private void stepPanels() {
        int limit = viewHeight() - HEIGHT_MARGIN;

        for (Panel panel : panels) panel.update(frameDelta / 20.0, limit);
    }

    /**
     * One frame's state, advanced once and in the copied client's order: the pointer's chase, then the
     * panels — expansion, scroll, every row's offset — and only then the row under the pointer and the
     * rectangle that the blur pass and the drawing share. Resolving before stepping is what made the
     * tooltip's text lag one frame behind its blurred rectangle while a panel opened or closed.
     */
    private void stepFrame() {
        advanceHover();

        ZioTooltipFrame.Resolved resolved = ZioTooltipFrame.resolve(panelsSource, canvas, viewWidth(),
            hoverX, hoverY, mouseX, mouseY);

        tooltipLines = resolved == null ? null : resolved.lines();
        tooltipBounds = resolved == null ? null : resolved.bounds();
        framed = true;
    }

    private void advanceHover() {
        if (!hoverFollowing) {
            hoverX = mouseX;
            hoverY = mouseY;
            hoverFollowing = true;
        }

        hoverX = ZioPaint.follow(hoverX, mouseX, ZioAnim.fps());
        hoverY = ZioPaint.follow(hoverY, mouseY, ZioAnim.fps());
    }

    // Input

    /**
     * Keeps the pointer this screen works in step with the game's.
     *
     * <p>The effect pass runs before the frame is drawn and needs this frame's pointer, not the one the
     * previous frame was drawn with; {@code render} refreshes it again for the frames the game does not
     * report a move in.
     */
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.mouseX = mouseX / uiScale();
        this.mouseY = mouseY / uiScale();

        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double x = click.x() / uiScale();
        double y = click.y() / uiScale();
        int button = click.button();

        commitEdit();

        // A click anywhere but the open list's search field takes the keyboard back.
        if (listSearchRow != null) {
            Panel owner = panelAt(x, y);
            SettingRow hit = owner == null ? null : owner.settingAt(x, y, viewHeight() - HEIGHT_MARGIN);

            if (hit != listSearchRow) {
                listSearchRow.listSearchFocused = false;
                listSearchRow = null;
            }
        }

        // While a key is being bound, the next press becomes the binding.
        if (bindingSetting != null) {
            ((KeybindSetting) bindingSetting.setting).set(Keybind.fromButton(button));
            bindingSetting = null;
            return true;
        }

        // The zoom controls, then the preset panel, which is drawn over the screen.
        if (clickZoom(x, y)) return true;
        if (clickGame(x, y, button)) return true;
        if (clickFx(x, y, button)) return true;
        if (clickPresets(x, y, button)) return true;

        // A press anywhere puts the search away and drops what it filtered, which is what the copied
        // client's reset() does on every click.
        searchFocused = false;
        searchQuery = "";
        searchCompletion = "";

        Panel panel = panelAt(x, y);

        if (panel != null) {
            if (panel.isOverHeader(x, y)) {
                if (button == 0) {
                    // A dragged panel is carried to the end of the list, so it is drawn over the
                    // others while it moves, exactly as the copied client reorders its panels.
                    List<Panel> raised = ZioDrag.raised(panels, panel);

                    panels.clear();
                    panels.addAll(raised);

                    draggingPanel = panel;
                    dragOffsetX = x - panel.x;
                    dragOffsetY = y - panel.y;

                    GuiSounds.click(theme, button);
                    return true;
                }

                if (button == 1) {
                    panel.expanded = !panel.expanded;
                    GuiSounds.click(theme, button);
                    return true;
                }
            } else {
                Row row = panel.rowAt(x, y, viewHeight() - HEIGHT_MARGIN);

                if (row != null) {
                    if (button == 0) {
                        row.module.toggle();
                        GuiSounds.click(theme, button);
                        return true;
                    }

                    if (button == 1) {
                        toggleRow(row);
                        GuiSounds.click(theme, button);
                        return true;
                    }
                }

                // The bind line of an open module, before the settings under it.
                Row bindOwner = panel.bindRowAt(x, y, viewHeight() - HEIGHT_MARGIN);

                if (bindOwner != null) {
                    // A left click listens for the key, a right click steps the mode — the copied
                    // client's own two clicks on its bind line.
                    if (button == 0) bindingRow = bindingRow == bindOwner ? null : bindOwner;
                    else if (button == 1) ZioBindModes.cycle(moduleBindId(bindOwner.module));

                    GuiSounds.click(theme, button);
                    return true;
                }

                SettingRow setting = panel.settingAt(x, y, viewHeight() - HEIGHT_MARGIN);

                if (setting != null && (button == 0 || button == 1)) {
                    commitEdit();
                    clicked(setting, x, y, button);
                    GuiSounds.click(theme, button);
                    return true;
                }

                commitEdit();
            }
        }

        return super.mouseClicked(click, doubled);
    }

    private void clicked(SettingRow row, double mouseX, double mouseY, int button) {
        // A choice in an open list.
        if (row.option != null) {
            if (button == 0) row.selectOption();
            return;
        }

        if (row.group != null) {
            if (button == 0) row.group.sectionExpanded = !row.group.sectionExpanded;
            return;
        }

        Setting<?> setting = row.setting;

        if (setting instanceof BoolSetting bool) {
            if (button == 0) bool.set(!bool.get());
        } else if (setting instanceof IntSetting number) {
            if (button == 1) {
                beginEdit(row);
                return;
            }

            draggingSetting = row;
            drag(number, row, mouseX);
        } else if (setting instanceof DoubleSetting number) {
            if (button == 1) {
                beginEdit(row);
                return;
            }

            draggingSetting = row;
            drag(number, row, mouseX);
        } else if (setting instanceof EnumSetting<?> option) {
            // A left click steps through the values, a right click lists them to pick from.
            if (button == 1) row.dropdownOpen = !row.dropdownOpen;
            else cycle(option);
        } else if (isListSetting(setting)) {
            if (button == 1) {
                if (row.listOpen) closeList(row);
                else row.listOpen = true;
                return;
            }

            if (!row.listOpen) {
                row.listOpen = true;
                row.listCandidates = null;
                return;
            }

            switch (ZioListLayout.listZone(mouseY, row.top)) {
                case HEADER -> closeList(row);
                case SEARCH -> {
                    row.listSearchFocused = true;
                    listSearchRow = row;
                    editStart = System.currentTimeMillis();
                }
                case ENTRIES -> {
                    if (row.listCandidates == null) row.listCandidates = candidates(setting);

                    List<ListEntry> entries = visibleEntries(row, row.listCandidates);
                    int index = ZioListLayout.entryIndex(mouseY, row.top, row.listScroll);

                    if (index >= 0 && index < entries.size()) {
                        toggleEntry(setting, entries.get(index).value());

                        // A free text list is built from its own entries, so they have to be read again.
                        if (setting instanceof StringListSetting) row.listCandidates = null;
                    }
                }
                case NONE -> {
                    // The padding between the parts of the list does nothing.
                }
            }
        } else if (setting instanceof ColorSetting) {
            if (button == 1) {
                row.pickerOpen = !row.pickerOpen;
                if (row.pickerOpen) row.loadHsb();
                return;
            }

            if (!row.pickerOpen) {
                row.pickerOpen = true;
                row.loadHsb();
                return;
            }

            // A drag starts on whichever part of the picker was pressed.
            row.pickerZone = row.zoneAt(mouseY);
            if (row.pickerZone != 0) {
                draggingSetting = row;
                dragColour(row, mouseX, mouseY);
            }
        } else if (setting instanceof KeybindSetting) {
            // Left listens for the key, right steps the mode — the copied client's two clicks. The
            // bind itself is cleared with Escape while it listens, which is also what it does.
            if (button == 0) bindingSetting = row;
            else ZioBindModes.cycle(settingBindId(row.module, setting));
        } else if (setting instanceof StringSetting string && button == 0) {
            editingSetting = row;
            editingString = true;
            editStart = System.currentTimeMillis();
            editText = string.get();
        } else if (setting instanceof BlockPosSetting position && button == 0) {
            // The setting reads three comma separated numbers, so the field offers them the same way.
            BlockPos value = position.get();

            editingSetting = row;
            editingString = true;
            editStart = System.currentTimeMillis();
            editText = value.getX() + "," + value.getY() + "," + value.getZ();
        }
    }

    /** Turns a number into a text field holding its value. */
    private void beginEdit(SettingRow row) {
        editingSetting = row;
        editingString = false;
        editStart = System.currentTimeMillis();
        editText = row.setting instanceof IntSetting number
            ? String.valueOf(number.get())
            : String.format("%.2f", ((DoubleSetting) row.setting).get());
    }

    /** Applies what was typed, or drops it when it is not a number. */
    private void commitEdit() {
        if (editingSetting == null) return;

        SettingRow row = editingSetting;
        editingSetting = null;

        if (editingString) {
            editingString = false;

            // A position setting parses its own "x,y,z" text and keeps the old value when it cannot.
            if (row.setting instanceof BlockPosSetting position) position.parse(editText);
            else ((StringSetting) row.setting).set(editText);

            return;
        }

        String text = editText.trim().replace(',', '.');
        if (text.isEmpty()) return;

        try {
            double value = Double.parseDouble(text);

            if (row.setting instanceof IntSetting number) {
                number.set((int) Math.round(clamp(value, number.min, number.max)));
            } else if (row.setting instanceof DoubleSetting number) {
                number.set(clamp(value, number.min, number.max));
            }
        } catch (NumberFormatException ignored) {
            // Typing a dash or a lone dot is not a value yet; the old one stays.
        }
    }

    /** The text of a number row: what is being typed while it is edited, else its value. */
    private String editedText(SettingRow row, String text) {
        if (row != editingSetting) return text;

        boolean cursorVisible = ((System.currentTimeMillis() - editStart) / 500) % 2 == 0;
        int colon = text.indexOf(':');

        return text.substring(0, colon + 2) + editText + (cursorVisible ? "_" : "");
    }

    private void drag(Setting<?> setting, SettingRow row, double mouseX) {
        double min = setting instanceof IntSetting number ? number.min : ((DoubleSetting) setting).min;
        double max = setting instanceof IntSetting number ? number.max : ((DoubleSetting) setting).max;
        double fraction = clamp((mouseX - row.panel.x - 2) / (PANEL_WIDTH - 2), 0, 1);
        double value = valueAt(fraction, min, max);

        if (setting instanceof IntSetting number) {
            number.set((int) Math.round(clamp(Math.round(value), number.min, number.max)));
        } else {
            // Four decimals keep the drag smooth; a right click sets the exact value when needed.
            double rounded = Math.round(value * 10000.0) / 10000.0;
            ((DoubleSetting) setting).set(clamp(rounded, min, max));
        }
    }

    /** Dragging one of the picker's three parts sets its hue, saturation, brightness or alpha. */
    private void dragColour(SettingRow row, double mouseX, double mouseY) {
        int x = row.panel.x + COLOUR_PADDING;
        int size = COLOUR_SQUARE;
        double across = clamp((mouseX - x) / Math.max(1, size - 1), 0, 1);

        switch (row.pickerZone) {
            case 1 -> {
                row.hsb[1] = (float) across;
                row.hsb[2] = (float) (1 - clamp((mouseY - (row.top + ROW_HEIGHT + COLOUR_PADDING))
                    / Math.max(1, size - 1), 0, 1));
            }
            case 2 -> row.hsb[0] = (float) across;
            // The bar is opaque on the left, so a drag to the right clears the colour.
            case 3 -> row.alphaValue = (int) Math.round((1 - across) * 255);
            default -> {
                return;
            }
        }

        int rgb = java.awt.Color.HSBtoRGB(row.hsb[0], row.hsb[1], row.hsb[2]);
        ((ColorSetting) row.setting).set(new SettingColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF,
            row.alphaValue));
    }

    /**
     * Ranges this wide are scaled so a drag moves proportionally instead of by millions a pixel.
     *
     * <p>Some of the client's numbers are distances or delays bounded at two billion; a linear bar
     * ninety nine pixels wide cannot express those, and every setting of the kind was unusable here.
     */
    private static final double WIDE_RANGE = 10_000;

    /** The value a handle at {@code fraction} of the bar selects. */
    private static double valueAt(double fraction, double min, double max) {
        if (max <= min || max - min <= WIDE_RANGE) return min + (max - min) * fraction;

        if (min < 0) {
            // Two geometric halves meeting at zero, so negative bounds are reached evenly.
            if (fraction >= 0.5) return geometric((fraction - 0.5) * 2, 1, max);
            return -geometric((0.5 - fraction) * 2, 1, Math.abs(min));
        }

        return geometric(fraction, Math.max(1, min), max);
    }

    /** Where the handle sits for a value: the inverse of {@link #valueAt}. */
    private static double fractionOf(double value, double min, double max) {
        if (max <= min || max - min <= WIDE_RANGE) return (value - min) / (max - min);

        if (min < 0) {
            if (value >= 0) return 0.5 + inverseGeometric(value, 1, max) * 0.5;
            return 0.5 - inverseGeometric(-value, 1, Math.abs(min)) * 0.5;
        }

        return inverseGeometric(value, Math.max(1, min), max);
    }

    private static double geometric(double fraction, double start, double end) {
        if (end <= start) return start + (end - start) * fraction;
        return start * Math.pow(end / start, clamp(fraction, 0, 1));
    }

    private static double inverseGeometric(double value, double start, double end) {
        if (end <= start || value <= start) return 0;
        return Math.log(value / start) / Math.log(end / start);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Enum<?>> void cycle(EnumSetting<T> setting) {
        T[] values = (T[]) setting.getDefaultValue().getDeclaringClass().getEnumConstants();
        T current = setting.get();

        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                setting.set(values[(i + 1) % values.length]);
                return;
            }
        }
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingPanel = null;
        draggingSetting = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        double x = click.x() / uiScale();
        double y = click.y() / uiScale();

        if (draggingSetting != null) {
            if (draggingSetting.setting instanceof ColorSetting) dragColour(draggingSetting, x, y);
            else drag(draggingSetting.setting, draggingSetting, x);

            return true;
        }

        if (draggingPanel == null) return super.mouseDragged(click, offsetX, offsetY);

        draggingPanel.pinned = true;

        // The copied client keeps the whole panel on screen while it is dragged, not just its bar:
        // both edges are kept against the width and the height that are actually drawn.
        int limit = viewHeight() - HEIGHT_MARGIN;
        int drawn = HEADER_HEIGHT
            + (int) Math.round((draggingPanel.height(limit) - HEADER_HEIGHT) * draggingPanel.expansion);

        draggingPanel.x = ZioDrag.corner(x, dragOffsetX, viewWidth(), PANEL_WIDTH);
        draggingPanel.y = ZioDrag.corner(y, dragOffsetY, viewHeight(), drawn);

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        // Holding control makes the wheel a zoom, so the screen can be made to fit any window.
        long window = mc.getWindow().getHandle();

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS) {
            setUiScale(ZioScale.stepped(uiScale(), vertical));

            return true;
        }

        // The preset panel sits over everything, so it takes the wheel first.
        if (presetsOpen) {
            double x = mouseX / uiScale();
            double y = mouseY / uiScale();

            if (x >= presetX() && x <= presetX() + PRESET_WIDTH
                && y >= presetListTop() && y < presetListTop() + PRESET_ROWS * ROW_HEIGHT) {
                presetScroll = ZioListLayout.clampScroll(presetScroll - vertical * ROW_HEIGHT,
                    profileNames().size());

                return true;
            }
        }

        // The wheel goes to an open list under the pointer before the panel it sits in.
        SettingRow row = openListAt(mouseX / uiScale(), mouseY / uiScale());

        if (ZioInputRouter.scroll(row != null) == ZioInputRouter.ScrollDecision.LIST) {
            if (row.listCandidates == null) row.listCandidates = candidates(row.setting);

            row.listScroll = ZioInputRouter.scrollBy(row.listScroll, vertical,
                visibleEntries(row, row.listCandidates).size(), ROW_HEIGHT);

            return true;
        }

        Panel panel = panelAt(mouseX / uiScale(), mouseY / uiScale());
        if (panel == null) return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);

        panel.scrollTarget -= vertical * 20;
        return true;
    }

    private Panel panelAt(double x, double y) {
        int limit = viewHeight() - HEIGHT_MARGIN;

        for (Panel panel : panels) {
            // Only the part that is actually drawn counts, so a folded panel is just its bar.
            int height = HEADER_HEIGHT
                + (int) Math.round((panel.height(limit) - HEADER_HEIGHT) * panel.expansion);

            if (x >= panel.x - 1 && x <= panel.x + PANEL_WIDTH
                && y >= panel.y - 1 && y <= panel.y + height) {
                return panel;
            }
        }

        return null;
    }

    /**
     * Closes back to whatever opened this screen.
     *
     * <p>Escape is handled here rather than left to the widget tree: this screen paints itself and has
     * no widgets, so the base class never saw a key pressed. Falling back to the game when the screen
     * has no parent (opened from the pause menu or the tab bar) keeps it from reopening itself.
     */
    @Override
    public boolean keyPressed(KeyInput input) {
        boolean control = (input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;

        // The copied client's secret sequence — up, up, down, down, left, right, left, right, B, A —
        // unlocks the Game panel for good. It is fed every key the screen sees, before anything else
        // reads it, as the client does, and only while the panel is still locked.
        if (!ZioGames.unlocked()) {
            secretProgress = ZioSecretSequence.accept(secretProgress, input.key());

            if (ZioSecretSequence.complete(secretProgress)) ZioGames.unlock();
        }

        // A listening bind line takes the next key, and Escape clears the bind instead of leaving the
        // screen — the copied client's KeybindComponent does the same.
        if (bindingRow != null) {
            bindingRow.module.keybind.set(bindOf(input));

            bindingRow = null;

            return true;
        }

        // An open game panel reads its own keys first: the arrows, D, space and R.
        if (clickGameKey(input.key())) return true;

        // Ownership is decided by the router, which knows nothing of Minecraft; this switch only
        // carries the decision out. An open list comes first, so Escape folds it instead of leaving.
        switch (ZioInputRouter.key(input.key(), control, focus())) {
            case LIST_CLOSE -> closeList(listSearchRow);
            case LIST_BACKSPACE -> {
                String query = listSearchRow.listSearch;

                if (!query.isEmpty()) listSearchRow.listSearch = query.substring(0, query.length() - 1);
                listSearchRow.listScroll = 0;
            }
            case LIST_ENTER -> addTypedEntry(listSearchRow);
            case LIST_CLEAR -> {
                listSearchRow.listSearch = "";
                listSearchRow.listScroll = 0;
            }
            case LIST_PASTE -> {
                listSearchRow.listSearch = paste(listSearchRow.listSearch, 32);
                listSearchRow.listScroll = 0;
            }
            case EDIT_CANCEL -> {
                if (presetNameFocused) {
                    presetNameFocused = false;
                    presetName = "";
                } else {
                    editingSetting = null;
                    editingString = false;
                }
            }
            case EDIT_COMMIT -> {
                if (presetNameFocused) savePreset();
                else commitEdit();
            }
            case EDIT_BACKSPACE -> {
                String current = activeText();

                if (!current.isEmpty()) setActiveText(current.substring(0, current.length() - 1));
            }
            case EDIT_CLEAR -> setActiveText("");
            case EDIT_PASTE -> setActiveText(paste(activeText(), activeTextLimit()));
            case BIND_KEY -> {
                ((KeybindSetting) bindingSetting.setting).set(bindOf(input));
                bindingSetting = null;
            }
            case SCREEN_SEARCH_CLOSE -> {
                searchFocused = false;
                searchQuery = "";
                searchCompletion = "";
            }
            case SCREEN_SEARCH_BACKSPACE -> {
                if (!searchQuery.isEmpty()) searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                searchStart = System.currentTimeMillis();
                updateSearchCompletion();
            }
            case SCREEN_SEARCH_COMPLETE -> {
                searchQuery += searchCompletion;
                searchCompletion = "";
                searchStart = System.currentTimeMillis();
            }
            case SCREEN_SEARCH_CONFIRM -> confirmSearch();
            case OPEN_SCREEN_SEARCH -> {
                searchFocused = true;
                searchStart = System.currentTimeMillis();
            }
            case ESCAPE -> {
                // An open list is folded, then the preset panel is closed, and only then the screen
                // is left: narrowing comes before closing.
                if (closeAnyOpenList()) return true;

                if (presetsOpen) {
                    presetsOpen = false;
                    presetNameFocused = false;

                    return true;
                }

                exit();
            }
            case PASS -> {
                return super.keyPressed(input);
            }
        }

        return true;
    }

    /** The key a listening bind line takes: Escape clears it, anything else becomes the bind. */
    private static Keybind bindOf(KeyInput input) {
        return input.key() == GLFW.GLFW_KEY_ESCAPE
            ? Keybind.none()
            : Keybind.fromKeys(input.key(), input.modifiers());
    }

    /** What currently holds the keyboard, in the order the router asks about it. */
    private ZioInputRouter.Focus focus() {
        return new ZioInputRouter.Focus(listSearchRow != null, editingSetting != null,
            bindingSetting != null, searchFocused, presetNameFocused);
    }

    /** The text field the keyboard is currently typing into. */
    private String activeText() {
        return presetNameFocused ? presetName : editText;
    }

    private void setActiveText(String value) {
        if (presetNameFocused) presetName = value;
        else editText = value;
    }

    private int activeTextLimit() {
        if (presetNameFocused) return ZioPresets.MAX_NAME;

        return editingString ? 64 : 12;
    }

    @Override
    public boolean charTyped(CharInput input) {
        switch (ZioInputRouter.charTyped(input.codepoint(), focus())) {
            case LIST_SEARCH -> {
                if (listSearchRow.listSearch.length() < 32) {
                    listSearchRow.listSearch += (char) input.codepoint();
                    listSearchRow.listScroll = 0;
                }
            }
            case EDIT_FIELD -> {
                char typed = (char) input.codepoint();

                if (presetNameFocused) {
                    if (presetName.length() < ZioPresets.MAX_NAME) presetName += typed;
                } else if (editingString) {
                    if (editText.length() < 64) editText += typed;
                } else {
                    boolean number = (typed >= '0' && typed <= '9') || typed == '.' || typed == ','
                        || typed == '-';

                    if (number && editText.length() < 12) editText += typed;
                }
            }
            case SCREEN_SEARCH -> {
                if (searchQuery.length() < 24) {
                    searchQuery = (searchQuery + (char) input.codepoint()).toLowerCase(Locale.ROOT);
                    searchStart = System.currentTimeMillis();
                    updateSearchCompletion();
                }
            }
            case PASS -> {
                return super.charTyped(input);
            }
        }

        return true;
    }

    /** The clipboard, taken only as far as a field can hold it. */
    private static String paste(String current, int limit) {
        String clipboard = mc.keyboard.getClipboard();
        if (clipboard == null || clipboard.isEmpty()) return current;

        StringBuilder builder = new StringBuilder(current);

        for (char character : clipboard.toCharArray()) {
            if (builder.length() >= limit) break;
            if (character >= 32) builder.append(character);
        }

        return builder.toString();
    }

    /** Folds whichever list is unfolded: the one being searched, else the first one open. */
    private boolean closeAnyOpenList() {
        if (listSearchRow != null) {
            closeList(listSearchRow);
            return true;
        }

        for (Panel panel : panels) {
            for (Row row : panel.rows) {
                for (SettingRow setting : row.settingsRows) {
                    if (!setting.listOpen) continue;

                    closeList(setting);
                    return true;
                }
            }
        }

        return false;
    }

    /** Enter in a free text list's search adds what was typed; every other list ignores it. */
    private void addTypedEntry(SettingRow row) {
        if (!(row.setting instanceof StringListSetting strings)) return;

        String value = row.listSearch.trim();
        if (value.isEmpty()) return;

        List<String> list = new ArrayList<>(strings.get());
        if (!list.contains(value)) list.add(value);
        strings.set(list);

        row.listSearch = "";
        row.listCandidates = null;
        row.listScroll = 0;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void close() {
        exit();
    }

    /**
     * Leaves this screen at once.
     *
     * <p>The base class closes a Meteor screen through its own render pass, which fades it out and
     * only then swaps the screen; this screen paints itself and never runs that pass, so asking it to
     * close left the game on a screen that could not be dismissed. Going back to the screen that
     * opened this one, or to the game when there is none, is what Escape has to do here.
     */
    private void exit() {
        // The arrow comes back with the game's own pointer, as the copied client does when it closes.
        ZioCursor.STANDARD.apply();
        bindingRow = null;

        if (active == this) active = null;

        Screen target = parent;
        if (target == this) target = null;
        parent = null;

        mc.setScreen(target);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /**
     * Keeps the game from drawing Meteor's screen behind this one.
     *
     * <p>{@code WidgetScreen} draws the theme's backdrop and its tab bar here, and the copied client
     * has neither: its windows are the whole screen. Without this the tab bar sat on top of the panel
     * titles and the theme's background tinted everything.
     */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Nothing: this screen paints its own backdrop, which is the world, unblurred outside the
        // panels and blurred inside them by the effect pass.
    }

    // Model

    private final class Panel {
        private final String title;
        private final List<Row> rows = new ArrayList<>();

        private int x;
        private int y;

        private boolean expanded = true;
        private boolean pinned;
        /** Where the body was drawn last frame: nothing below it can be hovered or clicked. */
        private int bodyTop;
        private int bodyBottom;
        private double expansion = 1;
        private double headerAlpha = 1;
        /** The title bar's fade, half to full, at the copied client's 200 ms. */
        private final ZioAnim.Timed headerFade = new ZioAnim.Timed(1f);
        /** The two scroll fades, each over the same 200 ms. */
        private final ZioAnim.Timed topFadeAnim = new ZioAnim.Timed(0f);
        private final ZioAnim.Timed bottomFadeAnim = new ZioAnim.Timed(0f);
        private double topFade;
        private double bottomFade;
        private double scroll;
        private double scrollTarget;
        /** How many frames the scroll has spent past an end, for the spring back. */
        private int scrollFrames;

        private Panel(String title, List<Module> modules, int x) {
            this.title = title;
            this.x = x;
            this.y = PANEL_Y;

            for (Module module : modules) {
                rows.add(new Row(module, this));
            }
        }

        private void update(double delta, int heightLimit) {
            int fps = ZioAnim.fps();

            // Expansion, scroll and the enable sweep are eased approaches at the copied client's
            // speed of two; only the hover lift and the title bar fade run on the clock.
            expansion = ZioAnim.approach(expansion, expanded ? 1 : 0, 2.0, fps);

            // The wheel may run forty pixels past either end, and the offset is dragged back over the
            // frames that follow: the copied client's own recoverScrollOffset, at its own rate.
            double limit = Math.max(0, totalHeight() - height(heightLimit));
            ZioScroll.Recovery recovered = ZioScroll.recover(scrollTarget, limit, scrollFrames, fps);

            scrollTarget = recovered.offset();
            scrollFrames = recovered.frames();
            scroll = ZioAnim.approach(scroll, scrollTarget, 2.0, fps);
            if (Math.abs(scrollTarget - scroll) < 0.5) scroll = scrollTarget;

            long now = System.currentTimeMillis();
            // The title bar comes up with the body and follows the pointer on its own bar, at the
            // copied client's own threshold of a quarter open.
            headerFade.setTarget(now, expansion > 0.25 || isOverHeader(mouseX, mouseY), ZioAnim.FADE_MILLIS);
            headerAlpha = 0.5 + 0.5 * headerFade.value(now);

            // A fade at each end while the list runs on past it.
            topFadeAnim.setTarget(now, scroll > 0.5, ZioAnim.FADE_MILLIS);
            bottomFadeAnim.setTarget(now, scroll + height(heightLimit) < totalHeight() - 0.5,
                ZioAnim.FADE_MILLIS);
            topFade = topFadeAnim.value(now);
            bottomFade = bottomFadeAnim.value(now);

            double offset = HEADER_HEIGHT + 1 - scroll;

            for (Row row : rows) {
                if (!row.matches()) continue;

                row.offset = offset;
                offset += row.height();
                row.update(delta, mouseX, mouseY);
            }
        }

        /** Height of the whole list, without the scroll, for clamping that scroll. */
        private int totalHeight() {
            double height = HEADER_HEIGHT + 1;

            for (Row row : rows) {
                if (row.matches()) height += row.height();
            }

            return (int) Math.round(height);
        }

        private int height(int heightLimit) {
            return Math.min(totalHeight(), heightLimit);
        }

        private boolean isOverHeader(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + PANEL_WIDTH && mouseY >= y && mouseY <= y + HEADER_HEIGHT;
        }

        /** Whether a point falls in the part of the panel that is actually on screen. */
        private boolean withinBody(double mouseY) {
            return mouseY > bodyTop && mouseY < bodyBottom;
        }

        /** The open row whose bind line is under the pointer, or null when none is. */
        private Row bindRowAt(double mouseX, double mouseY, int heightLimit) {
            if (expansion < 0.5) return null;

            for (Row row : rows) {
                if (!row.open || !row.matches()) continue;
                if (row.isOverBindRow(mouseX, mouseY)) return row;
            }

            return null;
        }

        private Row rowAt(double mouseX, double mouseY, int heightLimit) {
            if (expansion < 0.5) return null;
            if (mouseX < x + 1 || mouseX > x + PANEL_WIDTH - 1) return null;

            int top = y + HEADER_HEIGHT;
            int bottom = y + height(heightLimit);
            if (mouseY < top || mouseY > bottom) return null;

            double position = mouseY - y;

            for (Row row : rows) {
                if (!row.matches()) continue;
                if (position >= row.offset && position < row.offset + ROW_HEIGHT) return row;
            }

            return null;
        }

        private SettingRow settingAt(double mouseX, double mouseY, int heightLimit) {
            if (expansion < 0.5) return null;
            if (mouseX < x + 1 || mouseX > x + PANEL_WIDTH - 1) return null;

            int top = y + HEADER_HEIGHT;
            int bottom = y + height(heightLimit);
            if (mouseY < top || mouseY > bottom) return null;

            double position = mouseY - y;

            for (Row row : rows) {
                if (!row.open || !row.matches()) continue;

                double settingTop = row.offset + ROW_HEIGHT;
                double settingPosition = position - settingTop;
                if (settingPosition < 0 || settingPosition >= row.settingsHeight()) continue;

                int[] heights = new int[row.settingsRows.size()];
                for (int i = 0; i < heights.length; i++) heights[i] = row.settingsRows.get(i).height();

                int index = ZioListLayout.indexAt(settingPosition, heights);

                return index < 0 ? null : row.settingsRows.get(index);
            }

            return null;
        }

        private Module moduleAt(double mouseX, double mouseY) {
            int limit = viewHeight() - HEIGHT_MARGIN;
            Row row = rowAt(mouseX, mouseY, limit);
            return row == null ? null : row.module;
        }
    }

    /**
     * A line too wide for its panel: nothing while it fits, then, after 750 ms under the pointer, a
     * 500 ms slide to the left edge of its overflow, and back when the pointer leaves.
     */
    private static final class TextScroll {
        private final ZioAnim.Timed anim = new ZioAnim.Timed(0f);
        private long hoveredAt;
        private boolean wasHovered;

        private float offset(float width, boolean hovered) {
            long now = System.currentTimeMillis();
            float available = PANEL_WIDTH - 4;

            if (width <= available) {
                anim.set(now, 0f);
                wasHovered = hovered;

                return 0f;
            }

            if (hovered) {
                if (!wasHovered) hoveredAt = now;
                if (now > hoveredAt + 750L) anim.setTarget(now, width - available, 500L);
            } else {
                anim.setTarget(now, 0f, 500L);
            }

            wasHovered = hovered;

            return anim.value(now);
        }
    }

    private final class Row {
        private final Module module;
        private final Panel panel;
        private final List<SettingGroup> groups = new ArrayList<>();
        private final List<SettingRow> settingsRows = new ArrayList<>();
        private final Map<Setting<?>, SettingRow> settingRowsByKey = new LinkedHashMap<>();
        private final Map<SettingGroup, SettingRow> groupRowsByKey = new LinkedHashMap<>();
        private final Map<Setting<?>, Map<Object, SettingRow>> optionRowsByKey = new LinkedHashMap<>();

        private boolean open;
        private boolean hovered;
        /** When the search's flash on this row started, or zero when it is not flashing. */
        private long highlightStartedAt;

        /** The bind line at the foot of an open module: where it was drawn, and its own hover lift. */
        private int bindTop;
        private boolean bindHovered;
        private double bindHover;
        private final ZioAnim.Timed bindHoverAnim = new ZioAnim.Timed(0f);
        private double enabled;
        private double hover;
        private double openAnim;
        /** The hover lift, one layout pixel up over 65 ms. */
        private final ZioAnim.Timed hoverAnim = new ZioAnim.Timed(0f);
        /** The sideways scroll of a name too long for the panel, under the pointer. */
        private final TextScroll textScroll = new TextScroll();

        /**
         * How far a line has to slide to be read whole: nothing while it fits, then, after 750 ms of
         * hover, a 500 ms scroll to the left edge of its overflow, and back when the pointer leaves.
         */
        private double offset;

        private Row(Module module, Panel panel) {
            this.module = module;
            this.panel = panel;
            this.enabled = module.isActive() ? 1 : 0;
            this.groups.addAll(module.settings.groups);

            rebuild();
        }

        /**
         * Lays the settings out as they read right now.
         *
         * <p>The client keeps each group folded on its own and hides every setting whose condition
         * is not met, so this list cannot be built once at start up: a folded group, a checkbox that
         * reveals its own children, or a filter that hides half a group all change it. The rows are
         * cached per setting and per group, so their animations survive the reshuffle.
         */
        private void rebuild() {
            settingsRows.clear();

            SettingGroup defaultGroup = groups.isEmpty() ? null : groups.get(0);

            for (SettingGroup group : groups) {
                boolean anyVisible = false;

                for (Setting<?> setting : group) {
                    if (setting.isVisible()) {
                        anyVisible = true;
                        break;
                    }
                }

                // The first group is the one the client shows without a title, so it is unfolded
                // and unlabelled; every other group is a header that folds its own settings.
                if (group != defaultGroup) {
                    SettingRow header = groupRowsByKey.computeIfAbsent(group, g -> new SettingRow(this, g));
                    header.hasSettings = anyVisible;
                    settingsRows.add(header);

                    if (!group.sectionExpanded) continue;
                }

                for (Setting<?> setting : group) {
                    if (!setting.isVisible()) continue;

                    SettingRow row = settingRowsByKey.computeIfAbsent(setting,
                        s -> new SettingRow(this, s));
                    settingsRows.add(row);

                    // An open list of choices unfolds under the setting it belongs to.
                    if (setting instanceof EnumSetting<?> option && row.dropdownOpen) {
                        addOptions(row, option);
                    }
                }
            }
        }

        /**
         * Adds one row per choice of an enumeration, in the order it declares them.
         *
         * <p>The rows are cached per value, so hovering one does not restart its animation as the
         * list is rebuilt.
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        private void addOptions(SettingRow owner, EnumSetting<?> option) {
            Map<Object, SettingRow> cache = optionRowsByKey.computeIfAbsent(option,
                s -> new LinkedHashMap<>());

            for (Object value : option.get().getDeclaringClass().getEnumConstants()) {
                settingsRows.add(cache.computeIfAbsent(value,
                    v -> new SettingRow(this, (EnumSetting) option, owner, v)));
            }
        }

        /** Whether this module is one the search asks for. */
        private boolean matches() {
            if (searchQuery.isEmpty()) return true;
            if (module.title.toLowerCase(Locale.ROOT).contains(searchQuery)) return true;

            // The copied client filters on the module's aliases too, so a short word finds it.
            for (String alias : module.aliases) {
                if (alias.toLowerCase(Locale.ROOT).contains(searchQuery)) return true;
            }

            return false;
        }

        private int settingsHeight() {
            int[] heights = new int[settingsRows.size()];

            for (int i = 0; i < heights.length; i++) heights[i] = settingsRows.get(i).height();

            return ZioListLayout.totalHeight(heights);
        }

        private int height() {
            return ROW_HEIGHT + (int) Math.round(blockHeight() * openAnim);
        }

        /** Everything under the row's own line: its settings, then its bind line. */
        private int blockHeight() {
            return settingsHeight() + ROW_HEIGHT;
        }

        /** Whether the pointer is on this row's bind line, as last drawn. */
        private boolean isOverBindRow(double mouseX, double mouseY) {
            return mouseX > panel.x && mouseX < panel.x + PANEL_WIDTH
                && mouseY > bindTop && mouseY < bindTop + ROW_HEIGHT
                && panel.withinBody(mouseY);
        }

        private void update(double delta, double mouseX, double mouseY) {
            int fps = ZioAnim.fps();

            enabled = ZioAnim.approach(enabled, module.isActive() ? 1 : 0, 2.0, fps);
            openAnim = ZioAnim.approach(openAnim, open ? 1 : 0, 2.0, fps);

            boolean over = isOver(mouseX, mouseY);
            if (over && !hovered) GuiSounds.hover(theme);
            hovered = over;

            long now = System.currentTimeMillis();

            // The bind line follows the pointer on its own, with the same 65 ms lift as a row.
            boolean overBind = open && isOverBindRow(mouseX, mouseY);
            if (overBind && !bindHovered) GuiSounds.hover(theme);
            bindHovered = overBind;
            bindHoverAnim.setTarget(now, overBind, ZioAnim.HOVER_MILLIS);
            bindHover = bindHoverAnim.value(now);
            hoverAnim.setTarget(now, over, ZioAnim.HOVER_MILLIS);
            hover = hoverAnim.value(now);

            rebuild();

            double settingOffset = 0;
            int rowTop = (int) Math.round(panel.y + offset + ROW_HEIGHT);

            for (SettingRow setting : settingsRows) {
                setting.offset = settingOffset;
                settingOffset += setting.height();

                setting.update(delta, mouseX, mouseY, rowTop);
            }
        }

        private boolean isOver(double mouseX, double mouseY) {
            int top = (int) Math.round(panel.y + offset);
            return mouseX > panel.x && mouseX < panel.x + PANEL_WIDTH
                && mouseY > top && mouseY < top + ROW_HEIGHT
                && panel.withinBody(mouseY);
        }
    }

    /**
     * One line under a module: either a setting, or the header of the group that holds settings.
     */
    private final class SettingRow {
        private final Panel panel;
        private final Module module;
        private final Setting<?> setting;
        private final SettingGroup group;

        /** Set on a choice of an enumeration, with the row of the setting it belongs to. */
        private final Object option;
        private final EnumSetting<?> enumOwner;
        private final SettingRow enumRow;

        private boolean hasSettings;
        private boolean hovered;
        private boolean dropdownOpen;

        /** Colour picker state: whether it is unfolded, and which of its three parts is dragged. */
        private boolean pickerOpen;
        private int pickerZone;
        private float[] hsb = {0f, 0f, 1f};
        private int alphaValue = 255;

        /** List editor state: whether it is unfolded, its own search, and how far it is scrolled. */
        private boolean listOpen;
        private boolean listSearchFocused;
        private String listSearch = "";
        private double listScroll;
        private List<ListEntry> listCandidates;

        /** The top of this line on screen, for the picker's drags. */
        private int top;

        private double fill;
        /** How far an open list's accent bar has slid from its first choice, in pixels. */
        private double slide;
        /** The picker's markers, which glide to their value at the client's slower speed. */
        private double markerSatX;
        private double markerBriY;
        private double markerHueX;
        private double markerAlphaX;
        private double markerExpansion;
        private double markerContrast;
        /** How far each entry of the open list has faded in, by value. */
        private final Map<Object, Double> listFade = new LinkedHashMap<>();
        private double hover;
        private double offset;
        private final ZioAnim.Timed hoverAnim = new ZioAnim.Timed(0f);
        private final TextScroll textScroll = new TextScroll();

        /** The same 750 ms, then 500 ms, sideways scroll a module name gets. */
        private SettingRow(Row owner, Setting<?> setting) {
            this.panel = owner.panel;
            this.module = owner.module;
            this.setting = setting;
            this.group = null;
            this.option = null;
            this.enumOwner = null;
            this.enumRow = null;
        }

        private SettingRow(Row owner, SettingGroup group) {
            this.panel = owner.panel;
            this.module = owner.module;
            this.setting = null;
            this.group = group;
            this.option = null;
            this.enumOwner = null;
            this.enumRow = null;
        }

        private SettingRow(Row owner, EnumSetting<?> enumOwner, SettingRow enumRow, Object option) {
            this.panel = owner.panel;
            this.module = owner.module;
            this.setting = null;
            this.group = null;
            this.enumOwner = enumOwner;
            this.enumRow = enumRow;
            this.option = option;
        }

        private String optionLabel() {
            return String.valueOf(option);
        }

        /** The height of this line, which a colour picker makes taller. */
        private int height() {
            return ZioListLayout.rowHeight(pickerOpen, listOpen);
        }

        /**
         * The picker's four markers each glide to their value, and the square's grows while it is held:
         * {@code markerExpansion} and {@code markerContrast}, at 0.35 rather than the screen's speed of
         * two, which is what gives the copied client's picker its slow drift.
         */
        private void updateMarkers() {
            int fps = ZioAnim.fps();
            double extent = COLOUR_SQUARE - 1;
            boolean held = draggingSetting == this && pickerZone == 1;

            markerSatX = ZioColorMarker.slide(markerSatX, hsb[1] * extent, fps);
            markerBriY = ZioColorMarker.slide(markerBriY, (1 - hsb[2]) * extent, fps);
            markerHueX = ZioColorMarker.slide(markerHueX, hsb[0] * extent, fps);
            markerAlphaX = ZioColorMarker.slide(markerAlphaX,
                ZioColorMarker.alphaMarker(COLOUR_SQUARE, alphaValue), fps);
            markerExpansion = ZioColorMarker.slide(markerExpansion, held ? 1 : 0, fps);
            markerContrast = ZioColorMarker.contrast(markerContrast,
                ZioColorMarker.luminance(java.awt.Color.HSBtoRGB(hsb[0], hsb[1], hsb[2])) > 0.5 ? 1 : 0, fps);
        }

        /** Reads the colour into the picker's own hue, saturation, brightness and alpha. */
        private void loadHsb() {
            SettingColor value = ((ColorSetting) setting).get();

            hsb = java.awt.Color.RGBtoHSB(value.r, value.g, value.b, null);
            alphaValue = value.a;
        }

        private int packedColour() {
            SettingColor value = ((ColorSetting) setting).get();
            return (value.a << 24) | (value.r << 16) | (value.g << 8) | value.b;
        }

        /** Which part of the picker the pointer is on: 1 square, 2 hue, 3 alpha, 0 elsewhere. */
        private int zoneAt(double mouseY) {
            int squareTop = top + ROW_HEIGHT + COLOUR_PADDING;
            int hueTop = squareTop + COLOUR_SQUARE + COLOUR_PADDING;
            int alphaTop = hueTop + COLOUR_SLIDER + COLOUR_PADDING;

            if (mouseY >= squareTop && mouseY < squareTop + COLOUR_SQUARE) return 1;
            if (mouseY >= hueTop && mouseY < hueTop + COLOUR_SLIDER) return 2;
            if (mouseY >= alphaTop && mouseY < alphaTop + COLOUR_SLIDER) return 3;

            return 0;
        }

        /** How many choices the open list this line belongs to offers. */
        private int optionCount() {
            return enumOwner == null ? 0 : enumOwner.get().getDeclaringClass().getEnumConstants().length;
        }

        /** Whether this is the last choice, where the box closes. */
        private boolean optionIsLast() {
            return option instanceof Enum<?> value && value.ordinal() == optionCount() - 1;
        }

        /** Picks this choice, and folds the list back up. */
        @SuppressWarnings({"unchecked", "rawtypes"})
        private void selectOption() {
            if (enumOwner == null) return;

            ((EnumSetting) enumOwner).set(option);
            if (enumRow != null) enumRow.dropdownOpen = false;
        }

        private void update(double delta, double mouseX, double mouseY, int rowTop) {
            top = rowTop + (int) Math.round(offset);

            boolean over = mouseX > panel.x && mouseX < panel.x + PANEL_WIDTH
                && mouseY > top && mouseY < top + ROW_HEIGHT
                && panel.withinBody(mouseY);

            if (over && !hovered) GuiSounds.hover(theme);
            hovered = over;

            long now = System.currentTimeMillis();
            hoverAnim.setTarget(now, over, ZioAnim.HOVER_MILLIS);
            hover = hoverAnim.value(now);

            // An open enumeration's accent bar slides from the first choice to the chosen one; folded,
            // it travels back to hide behind the setting's own line.
            if (setting instanceof EnumSetting<?> choice) {
                double target = dropdownOpen ? choice.get().ordinal() * ROW_HEIGHT : -ROW_HEIGHT;

                slide = ZioEnumBox.slide(slide, target, ZioAnim.fps());
            }

            if (pickerOpen) updateMarkers();

            if (group != null || option != null) return;

            // The fill of a boolean or a number is its value: full width when on, or where the
            // slider sits for a number.
            double target;

            if (setting instanceof BoolSetting bool) {
                target = bool.get() ? 1 : 0;
            } else if (setting instanceof IntSetting number) {
                target = (PANEL_WIDTH - 2) * fractionOf(number.get(), number.min, number.max);
            } else if (setting instanceof DoubleSetting number) {
                target = (PANEL_WIDTH - 2) * fractionOf(number.get(), number.min, number.max);
            } else if (setting instanceof EnumSetting<?>) {
                // The copied client fills the whole row behind an enumeration of choices.
                target = 1;
            } else {
                target = 0;
            }

            // A number's bar is the client's own InterpolatedFloat(6.0F, false): three times the panel's
            // speed, and uneased, so the fill tracks the slider instead of drifting behind it. Every
            // other fill — a boolean, an enumeration's full width bar — eases at the screen's speed of
            // two, which is what ClampedInterpolatedFloat(2.0F, true) does there.
            boolean numeric = setting instanceof IntSetting || setting instanceof DoubleSetting;

            fill = numeric
                ? ZioAnim.linear(fill, target, ZioAnim.NUMERIC_FILL_SPEED, ZioAnim.fps())
                : ZioAnim.approach(fill, target, 2.0, ZioAnim.fps());
        }
    }

    /**
     * The handle that opens this screen.
     *
     * <p>It is not added to the client's tab bar, which already has its own screens: picking the Zio
     * theme is what opens this one.
     */
    public static class ZioTab extends Tab {
        public ZioTab() {
            super("Zio");
        }

        @Override
        public TabScreen createScreen(GuiTheme theme) {
            return new ZioScreen(theme, this);
        }

        @Override
        public boolean isScreen(Screen screen) {
            return screen instanceof ZioScreen;
        }
    }
}
