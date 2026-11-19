package com.pro0101.vaultline.gui.zio;

import meteordevelopment.meteorclient.MeteorClient;
import org.lwjgl.glfw.GLFW;

/**
 * The pointer shapes the copied client switches between, ported from its {@code gui/CursorType}.
 *
 * <p>Its three shapes are the ones used here, with the same GLFW constants:
 * {@code STANDARD(() -> GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR))},
 * {@code POINTER(... GLFW_HAND_CURSOR)} and {@code INPUT(... GLFW_IBEAM_CURSOR)}. The screen chooses a
 * shape while it draws and applies the choice on the next frame; its screen does exactly that at the
 * top of {@code render} ({@code cursor.apply(); cursor = CursorType.STANDARD;}) and puts the arrow back
 * when the screen closes ({@code onClose}: {@code CursorType.STANDARD.apply()}).
 */
public enum ZioCursor {
    STANDARD(GLFW.GLFW_ARROW_CURSOR),
    POINTER(GLFW.GLFW_HAND_CURSOR),
    INPUT(GLFW.GLFW_IBEAM_CURSOR);

    /** The shape the screen asked for while drawing the current frame. */
    private static ZioCursor chosen = STANDARD;

    /** The shape actually installed on the window, so the call is only made when it changes. */
    private static ZioCursor applied = STANDARD;

    private final int shape;
    private long handle = -1L;

    ZioCursor(int shape) {
        this.shape = shape;
    }

    /** The GLFW standard cursor this shape is created from. */
    public int shape() {
        return shape;
    }

    public static ZioCursor chosen() {
        return chosen;
    }

    public static void set(ZioCursor cursor) {
        chosen = cursor == null ? STANDARD : cursor;
    }

    public static void reset() {
        chosen = STANDARD;
    }

    /** Installs this shape on the game window, unless it is already the one in use. */
    public void apply() {
        if (this == applied) return;

        if (MeteorClient.mc == null || MeteorClient.mc.getWindow() == null) return;

        long cursor = handle();

        if (cursor == 0L) return;

        GLFW.glfwSetCursor(MeteorClient.mc.getWindow().getHandle(), cursor);
        applied = this;
    }

    /** The window handle for this shape, created once and kept. */
    private long handle() {
        if (handle == -1L) handle = GLFW.glfwCreateStandardCursor(shape);

        return handle;
    }
}
