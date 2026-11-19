package com.pro0101.vaultline.gui.zio;

import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.screens.HudEditorScreen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The copied client's HUD editor, over this addon's own HUD.
 *
 * <p>It edits the {@link HudElement}s Meteor already has registered for Vaultline — the same objects
 * the HUD draws, with the same model, the same rendering and the same saved positions. Nothing here
 * introduces a second kind of element: the screen only moves and resizes what {@code Hud.get()} holds,
 * and every change goes through {@code HudElement.setPos} / {@code setSize}, which is what the client
 * saves.
 *
 * <p>What it takes from {@code open-zio-main}'s {@code gui/screens/HudEditorScreen}:
 * <ul>
 *   <li>the layout, which is the client's own: its elements are painted by
 *       {@code HudEditorScreen.renderElements} and only framed by this screen, and the safe-zone bands
 *       are the ones {@link ZioHudMargins} animates;</li>
 *   <li>the order of a press: an element first ({@code mouseClicked} scans the widgets and only calls
 *       the margin controller when none was hit), the margins second;</li>
 *   <li>a drag keeps the offset the element was grabbed at ({@code dragOffsetX/dragOffsetY}) and, on
 *       release, the element is snapped with a {@code 5.0F} distance
 *       ({@code widget.updateSnapping(screenWidth, screenHeight, 5.0F)}) — see {@link ZioHudLayout};</li>
 *   <li>right click on an element opens that element's own settings, which here is the module's screen
 *       Meteor already builds, instead of the copied client's panel row;</li>
 *   <li>the black safe-zone bands are drawn over everything, as that editor draws them.</li>
 * </ul>
 *
 * <p>The resize handle is this addon's addition, and it sticks: Meteor elements report the size they
 * measure at the end of every render through {@code HudElement.setSize}, which used to overwrite the
 * handle on the next frame. {@link ZioHudSizes} now answers that call with the size this editor set
 * (see {@code HudElementSizeMixin}), so the element keeps the forced size for good — until it is
 * cleared here with a right click, after which the element measures itself again.
 */
public class ZioHudEditorScreen extends Screen {
    /** The accent the ported screen uses for its own chrome. */
    private static final int ACCENT = 0x82C794F4;
    private static final int FRAME = 0xFFFFFFFF;
    private static final int LABEL = 0xFFE1E1E1;
    private static final int DIM = 0x66000000;
    /** The centre guides: white at forty percent of the screen's fade, as that editor draws them. */
    private static final int GUIDE = 0x66FFFFFF;

    private final ZioHudMargins margins = new ZioHudMargins();

    private HudElement dragElement;
    private boolean resizing;
    private double grabX;
    private double grabY;
    private ZioHudLayout.Rect dragStart;

    private HudElement selected;

    /** How far the hover label has faded in, and where it was last asked to sit. */
    private double hoverOpacity;

    public ZioHudEditorScreen() {
        super(Text.literal("Vaultline HUD editor"));

        int[] saved = ZioHudMarginStore.current();

        margins.setSafeX(saved[0]);
        margins.setSafeY(saved[1]);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // The safe zones follow the pointer and, while one is being dragged, the drag itself.
        margins.update(System.currentTimeMillis(), mouseX, mouseY, width, height);

        if (margins.draggingHorizontal() || margins.draggingVertical()) persistMargins();

        // The world is still visible, dimmed, so the HUD reads against a dark sheet.
        context.fill(0, 0, width, height, DIM);

        // The elements themselves, drawn by the client that owns them.
        HudEditorScreen.renderElements(context);

        for (ZioHudLayout.Rect guide : ZioHudLayout.centreGuides(width, height)) {
            context.fill(guide.x(), guide.y(), guide.right(), guide.bottom(), GUIDE);
        }

        drawFrames(context);
        drawBands(context);
        drawHoverLabel(context, mouseX, mouseY);
    }

    /**
     * The hovered element's name beside the pointer, sliding in from ten pixels away and fading as it
     * does there: {@code hoverLabelOpacity} at twice the animation speed, with its label offset.
     */
    private void drawHoverLabel(DrawContext context, int mouseX, int mouseY) {
        HudElement hovered = elementAt(mouseX, mouseY);

        hoverOpacity = ZioAnim.approach(hoverOpacity, hovered == null ? 0 : 1, ZioHudLayout.HOVER_SPEED,
            ZioAnim.fps());

        if (hovered == null || hoverOpacity < 0.01) return;

        int alpha = ZioHudLayout.labelAlpha(hoverOpacity);
        int x = mouseX + 10 + ZioHudLayout.labelOffset(hoverOpacity, 1);

        ZioFontRenderer.draw(context, hovered.info.name, x, mouseY - 10, (alpha << 24) | (LABEL & 0xFFFFFF));
    }

    /** A frame and a name around every element, plus a handle on the one being edited. */
    private void drawFrames(DrawContext context) {
        for (HudElement element : elements()) {
            ZioHudLayout.Rect rect = rectOf(element);
            int colour = element == selected ? ACCENT : FRAME;

            context.fill(rect.x() - 1, rect.y() - 1, rect.right() + 1, rect.y(), colour);
            context.fill(rect.x() - 1, rect.bottom(), rect.right() + 1, rect.bottom() + 1, colour);
            context.fill(rect.x() - 1, rect.y() - 1, rect.x(), rect.bottom() + 1, colour);
            context.fill(rect.right(), rect.y() - 1, rect.right() + 1, rect.bottom() + 1, colour);

            ZioFontRenderer.draw(context, element.info.name, rect.x(), Math.max(0, rect.y() - 11), LABEL);

            if (element == selected) {
                // The resize corner, drawn so it can be found.
                context.fill(rect.right() - ZioHudLayout.RESIZE_HANDLE, rect.bottom() - 2,
                    rect.right(), rect.bottom(), ACCENT);
                context.fill(rect.right() - 2, rect.bottom() - ZioHudLayout.RESIZE_HANDLE,
                    rect.right(), rect.bottom(), ACCENT);
            }
        }
    }

    /** The safe-zone bands, exactly the two the margin controller animates. */
    private void drawBands(DrawContext context) {
        long now = System.currentTimeMillis();

        float horizontalAlpha = margins.horizontalOverlay(now) / 255f;
        float verticalAlpha = margins.verticalOverlay(now) / 255f;

        if (horizontalAlpha > 0f) {
            int colour = bandColour(horizontalAlpha);

            context.fill(0, 0, margins.safeX(), height, colour);
            context.fill(width - margins.safeX(), 0, width, height, colour);
        }

        if (verticalAlpha > 0f) {
            int colour = bandColour(verticalAlpha);

            context.fill(0, 0, width, margins.safeY(), colour);
            context.fill(0, height - margins.safeY(), width, height, colour);
        }
    }

    private static int bandColour(float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));

        return a << 24;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double x = click.x();
        double y = click.y();

        if (click.button() == 0) {
            HudElement hit = elementAt(x, y);

            if (hit != null) {
                selected = hit;
                dragElement = hit;
                dragStart = rectOf(hit);
                resizing = ZioHudLayout.overResizeHandle(dragStart, x, y);
                grabX = x - dragStart.x();
                grabY = y - dragStart.y();

                return true;
            }

            selected = null;
            margins.click(x, y, click.button(), width, height);

            return true;
        }

        // A right click only selects: the element settings stay where the client already keeps them
        // (Meteor's own HUD editor), since this screen does not own that panel.
        if (click.button() == 1) {
            HudElement hit = elementAt(x, y);

            if (hit != null) {
                selected = hit;

                // Right click on a resized element gives it back its own measurement.
                if (ZioHudSizes.isForced(hit.info.name)) ZioHudSizes.clear(hit.info.name);

                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragElement == null) return super.mouseDragged(click, offsetX, offsetY);

        if (resizing) {
            // The element follows the corner but never grows past the screen, and the frame is redrawn
            // around the size it now reports.
            ZioHudLayout.Rect resized = ZioHudLayout.resize(dragStart, click.x(), click.y(), width, height);

            // Held in memory while the pointer moves, written out once it is released.
            ZioHudSizes.force(dragElement.info.name, resized.width(), resized.height());

            dragElement.setSize(resized.width(), resized.height());
            dragElement.updatePos();
        } else {
            ZioHudLayout.Rect moved = ZioHudLayout.drag(dragStart, grabX, grabY, click.x(), click.y(),
                width, height);

            dragElement.setPos(moved.x(), moved.y());
        }

        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragElement != null) {
            ZioHudLayout.Rect released = ZioHudLayout.snap(rectOf(dragElement), width, height,
                margins.safeX(), margins.safeY());

            dragElement.setPos(released.x(), released.y());
            dragElement.updatePos();

            // A size the handle set is kept: from here on the element reports this size instead of
            // measuring itself, and the size is written out with its position.
            if (ZioHudSizes.isForced(dragElement.info.name) || released.width() != dragStart.width()
                || released.height() != dragStart.height()) {
                ZioHudSizes.set(dragElement.info.name, released.width(), released.height());
                dragElement.setSize(released.width(), released.height());
                dragElement.updatePos();
            }

            dragElement = null;
            resizing = false;

            Hud.get().save();

            return true;
        }

        margins.release(click.button());
        persistMargins();

        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public void close() {
        persistMargins();
        Hud.get().save();

        super.close();
    }

    @Override
    public void removed() {
        // Leaving any other way still keeps the margins and the element positions.
        ZioCursor.STANDARD.apply();
        persistMargins();

        super.removed();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** The margins are what the HUD reads, so a drag is written out as soon as it happens. */
    private void persistMargins() {
        ZioHudMarginStore.set(margins.safeX(), margins.safeY());
    }

    private static ZioHudLayout.Rect rectOf(HudElement element) {
        return new ZioHudLayout.Rect(element.getX(), element.getY(),
            Math.max(1, element.getWidth()), Math.max(1, element.getHeight()));
    }

    /** The topmost element under the pointer, which is the one the client would draw last. */
    private HudElement elementAt(double x, double y) {
        List<HudElement> all = elements();

        for (int i = all.size() - 1; i >= 0; i--) {
            HudElement element = all.get(i);

            if (rectOf(element).contains(x, y)) return element;
        }

        return null;
    }

    private static List<HudElement> elements() {
        List<HudElement> list = new ArrayList<>();

        for (HudElement element : Hud.get()) list.add(element);

        return list;
    }
}
