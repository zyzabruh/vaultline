package com.pro0101.vaultline.gui.zio;

/**
 * Which part of the copied screen owns an input event.
 *
 * <p>The screen keeps several things that can hold the keyboard at once — an unfolded list's search
 * field, a number or string being typed into, a key being bound, the screen wide search — and the
 * order they are offered events in is the whole of the behaviour. This class decides that order with
 * nothing but primitives, enums and records, so it can be checked without a game client; the screen
 * only carries out what it decides.
 */
public final class ZioInputRouter {
    // The keys the screen reacts to, by their GLFW code, so this file needs no client type.
    public static final int KEY_ESCAPE = 256;
    public static final int KEY_ENTER = 257;
    public static final int KEY_BACKSPACE = 259;
    public static final int KEY_TAB = 258;
    public static final int KEY_KP_ENTER = 335;
    public static final int KEY_A = 65;
    public static final int KEY_F = 70;
    public static final int KEY_V = 86;

    /** What currently has the keyboard; several may be true at once and the order decides. */
    public record Focus(boolean listSearch, boolean editingSetting, boolean bindingKey, boolean screenSearch,
        boolean presetName) {
        public static final Focus NONE = new Focus(false, false, false, false, false);

        public Focus(boolean listSearch, boolean editingSetting, boolean bindingKey, boolean screenSearch) {
            this(listSearch, editingSetting, bindingKey, screenSearch, false);
        }

        public boolean any() {
            return listSearch || editingSetting || bindingKey || screenSearch || presetName;
        }
    }

    public enum KeyAction {
        /** Not this screen's key: let it go through untouched. */
        PASS,
        LIST_CLOSE,
        LIST_BACKSPACE,
        LIST_ENTER,
        LIST_CLEAR,
        LIST_PASTE,
        EDIT_CANCEL,
        EDIT_COMMIT,
        EDIT_BACKSPACE,
        EDIT_CLEAR,
        EDIT_PASTE,
        BIND_KEY,
        SCREEN_SEARCH_CLOSE,
        SCREEN_SEARCH_BACKSPACE,
        SCREEN_SEARCH_CONFIRM,
        SCREEN_SEARCH_COMPLETE,
        OPEN_SCREEN_SEARCH,
        /** Escape: fold an open list if there is one, otherwise leave the screen. */
        ESCAPE
    }

    public enum CharAction {
        PASS,
        LIST_SEARCH,
        EDIT_FIELD,
        SCREEN_SEARCH
    }

    public enum ScrollDecision {
        LIST,
        PANEL
    }

    private ZioInputRouter() {
    }

    /**
     * The key's owner.
     *
     * <p>An unfolded list comes first: while its search field is open, Escape folds it instead of
     * leaving the screen, and Backspace, Enter, Ctrl+A and Ctrl+V are the list's own. An editor or a
     * key binding that also believes it has the focus does not get the key before it. Letters are not
     * claimed here at all, they arrive through {@link #charTyped(int, Focus)}.
     */
    public static KeyAction key(int key, boolean control, Focus focus) {
        if (focus.listSearch()) {
            return switch (key) {
                case KEY_ESCAPE -> KeyAction.LIST_CLOSE;
                case KEY_BACKSPACE -> KeyAction.LIST_BACKSPACE;
                case KEY_ENTER, KEY_KP_ENTER -> KeyAction.LIST_ENTER;
                case KEY_A -> control ? KeyAction.LIST_CLEAR : KeyAction.PASS;
                case KEY_V -> control ? KeyAction.LIST_PASTE : KeyAction.PASS;
                default -> KeyAction.PASS;
            };
        }

        if (focus.presetName()) {
            return switch (key) {
                case KEY_ESCAPE -> KeyAction.EDIT_CANCEL;
                case KEY_ENTER, KEY_KP_ENTER -> KeyAction.EDIT_COMMIT;
                case KEY_BACKSPACE -> KeyAction.EDIT_BACKSPACE;
                case KEY_A -> control ? KeyAction.EDIT_CLEAR : KeyAction.PASS;
                case KEY_V -> control ? KeyAction.EDIT_PASTE : KeyAction.PASS;
                default -> KeyAction.PASS;
            };
        }

        if (focus.editingSetting()) {
            return switch (key) {
                case KEY_ESCAPE -> KeyAction.EDIT_CANCEL;
                case KEY_ENTER, KEY_KP_ENTER -> KeyAction.EDIT_COMMIT;
                case KEY_BACKSPACE -> KeyAction.EDIT_BACKSPACE;
                case KEY_A -> control ? KeyAction.EDIT_CLEAR : KeyAction.PASS;
                case KEY_V -> control ? KeyAction.EDIT_PASTE : KeyAction.PASS;
                default -> KeyAction.PASS;
            };
        }

        if (focus.bindingKey()) {
            return KeyAction.BIND_KEY;
        }

        if (focus.screenSearch()) {
            return switch (key) {
                case KEY_ESCAPE -> KeyAction.SCREEN_SEARCH_CLOSE;
                case KEY_BACKSPACE -> KeyAction.SCREEN_SEARCH_BACKSPACE;
                case KEY_ENTER, KEY_KP_ENTER -> KeyAction.SCREEN_SEARCH_CONFIRM;
                case KEY_TAB -> KeyAction.SCREEN_SEARCH_COMPLETE;
                default -> KeyAction.PASS;
            };
        }

        if (control && key == KEY_F) return KeyAction.OPEN_SCREEN_SEARCH;
        if (key == KEY_ESCAPE) return KeyAction.ESCAPE;

        return KeyAction.PASS;
    }

    /** Where a typed character goes: the open list first, and never the screen search beside it. */
    public static CharAction charTyped(int codepoint, Focus focus) {
        if (codepoint < 32) return CharAction.PASS;

        if (focus.listSearch()) return CharAction.LIST_SEARCH;
        if (focus.presetName()) return CharAction.EDIT_FIELD;
        if (focus.editingSetting()) return CharAction.EDIT_FIELD;
        if (focus.bindingKey()) return CharAction.PASS;
        if (focus.screenSearch()) return CharAction.SCREEN_SEARCH;

        return CharAction.PASS;
    }

    /** The wheel belongs to an open list under the pointer, and otherwise to the panel. */
    public static ScrollDecision scroll(boolean overOpenList) {
        return overOpenList ? ScrollDecision.LIST : ScrollDecision.PANEL;
    }

    /** The scroll of a list after a wheel notch, kept inside what the list can show. */
    public static double scrollBy(double scroll, double amount, int entries, int rowHeight) {
        return ZioListLayout.clampScroll(scroll - amount * rowHeight, entries);
    }
}
