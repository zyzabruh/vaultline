package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The routing decisions of the copied screen, checked without a game client. */
class ZioInputRouterTest {
    private static final ZioInputRouter.Focus LIST = new ZioInputRouter.Focus(true, false, false, false);
    private static final ZioInputRouter.Focus EDITING = new ZioInputRouter.Focus(false, true, false, false);
    private static final ZioInputRouter.Focus BINDING = new ZioInputRouter.Focus(false, false, true, false);
    private static final ZioInputRouter.Focus SCREEN_SEARCH = new ZioInputRouter.Focus(false, false, false, true);

    @Test
    void escapeFoldsTheOpenListInsteadOfLeavingTheScreen() {
        assertEquals(ZioInputRouter.KeyAction.LIST_CLOSE,
            ZioInputRouter.key(ZioInputRouter.KEY_ESCAPE, false, LIST));
        assertEquals(ZioInputRouter.KeyAction.ESCAPE,
            ZioInputRouter.key(ZioInputRouter.KEY_ESCAPE, false, ZioInputRouter.Focus.NONE));
    }

    @Test
    void theListKeepsTheKeyEvenWhenAnEditorOrBindingAlsoBelievesItHasIt() {
        ZioInputRouter.Focus crowded = new ZioInputRouter.Focus(true, true, true, true);

        assertEquals(ZioInputRouter.KeyAction.LIST_CLOSE,
            ZioInputRouter.key(ZioInputRouter.KEY_ESCAPE, false, crowded));
        assertEquals(ZioInputRouter.KeyAction.LIST_BACKSPACE,
            ZioInputRouter.key(ZioInputRouter.KEY_BACKSPACE, false, crowded));
        assertEquals(ZioInputRouter.KeyAction.LIST_CLEAR,
            ZioInputRouter.key(ZioInputRouter.KEY_A, true, crowded));
        assertEquals(ZioInputRouter.KeyAction.LIST_PASTE,
            ZioInputRouter.key(ZioInputRouter.KEY_V, true, crowded));
    }

    @Test
    void backspaceClearAndPasteAreTheListsOwn() {
        assertEquals(ZioInputRouter.KeyAction.LIST_BACKSPACE,
            ZioInputRouter.key(ZioInputRouter.KEY_BACKSPACE, false, LIST));
        assertEquals(ZioInputRouter.KeyAction.LIST_CLEAR,
            ZioInputRouter.key(ZioInputRouter.KEY_A, true, LIST));
        assertEquals(ZioInputRouter.KeyAction.LIST_PASTE,
            ZioInputRouter.key(ZioInputRouter.KEY_V, true, LIST));
        assertEquals(ZioInputRouter.KeyAction.LIST_ENTER,
            ZioInputRouter.key(ZioInputRouter.KEY_ENTER, false, LIST));
    }

    @Test
    void lettersAreLeftForCharTyped() {
        assertEquals(ZioInputRouter.KeyAction.PASS, ZioInputRouter.key(ZioInputRouter.KEY_A, false, LIST));
        assertEquals(ZioInputRouter.KeyAction.PASS, ZioInputRouter.key(ZioInputRouter.KEY_V, false, LIST));
        assertEquals(ZioInputRouter.KeyAction.PASS, ZioInputRouter.key(70, false, LIST));

        assertEquals(ZioInputRouter.CharAction.LIST_SEARCH, ZioInputRouter.charTyped('s', LIST));
    }

    @Test
    void withoutAListTheEditorComesFirstThenTheBindingThenTheScreenSearch() {
        assertEquals(ZioInputRouter.KeyAction.EDIT_CANCEL,
            ZioInputRouter.key(ZioInputRouter.KEY_ESCAPE, false, EDITING));
        assertEquals(ZioInputRouter.KeyAction.EDIT_COMMIT,
            ZioInputRouter.key(ZioInputRouter.KEY_ENTER, false, EDITING));

        assertEquals(ZioInputRouter.KeyAction.BIND_KEY,
            ZioInputRouter.key(ZioInputRouter.KEY_ESCAPE, false, BINDING));

        assertEquals(ZioInputRouter.KeyAction.SCREEN_SEARCH_CLOSE,
            ZioInputRouter.key(ZioInputRouter.KEY_ESCAPE, false, SCREEN_SEARCH));
    }

    @Test
    void theScreenSearchOpensOnCtrlFOnlyWhenNothingElseHasTheKeyboard() {
        assertEquals(ZioInputRouter.KeyAction.OPEN_SCREEN_SEARCH,
            ZioInputRouter.key(ZioInputRouter.KEY_F, true, ZioInputRouter.Focus.NONE));
        assertEquals(ZioInputRouter.KeyAction.PASS,
            ZioInputRouter.key(ZioInputRouter.KEY_F, false, ZioInputRouter.Focus.NONE));
        assertEquals(ZioInputRouter.KeyAction.PASS,
            ZioInputRouter.key(ZioInputRouter.KEY_F, true, LIST));
    }

    @Test
    void aTypedCharacterGoesToTheOpenListsSearchAndNeverToTheScreenSearch() {
        ZioInputRouter.Focus crowded = new ZioInputRouter.Focus(true, true, true, true);

        assertEquals(ZioInputRouter.CharAction.LIST_SEARCH, ZioInputRouter.charTyped('a', crowded));
        assertEquals(ZioInputRouter.CharAction.LIST_SEARCH, ZioInputRouter.charTyped(' ', crowded));
        assertEquals(ZioInputRouter.CharAction.LIST_SEARCH, ZioInputRouter.charTyped(':', crowded));
    }

    @Test
    void aTypedCharacterGoesElsewhereOnlyWhenNoListIsOpen() {
        assertEquals(ZioInputRouter.CharAction.EDIT_FIELD, ZioInputRouter.charTyped('1', EDITING));
        assertEquals(ZioInputRouter.CharAction.SCREEN_SEARCH, ZioInputRouter.charTyped('m', SCREEN_SEARCH));
        assertEquals(ZioInputRouter.CharAction.PASS, ZioInputRouter.charTyped('m', BINDING));
        assertEquals(ZioInputRouter.CharAction.PASS, ZioInputRouter.charTyped('m', ZioInputRouter.Focus.NONE));
        assertEquals(ZioInputRouter.CharAction.PASS, ZioInputRouter.charTyped(9, LIST));
    }

    @Test
    void thePresetNameFieldTypesLikeAnyOtherField() {
        ZioInputRouter.Focus presetName = new ZioInputRouter.Focus(false, false, false, false, true);

        assertEquals(ZioInputRouter.KeyAction.EDIT_CANCEL,
            ZioInputRouter.key(ZioInputRouter.KEY_ESCAPE, false, presetName));
        assertEquals(ZioInputRouter.KeyAction.EDIT_COMMIT,
            ZioInputRouter.key(ZioInputRouter.KEY_ENTER, false, presetName));
        assertEquals(ZioInputRouter.KeyAction.EDIT_BACKSPACE,
            ZioInputRouter.key(ZioInputRouter.KEY_BACKSPACE, false, presetName));
        assertEquals(ZioInputRouter.KeyAction.EDIT_CLEAR,
            ZioInputRouter.key(ZioInputRouter.KEY_A, true, presetName));
        assertEquals(ZioInputRouter.KeyAction.EDIT_PASTE,
            ZioInputRouter.key(ZioInputRouter.KEY_V, true, presetName));
        assertEquals(ZioInputRouter.CharAction.EDIT_FIELD, ZioInputRouter.charTyped('p', presetName));
    }

    @Test
    void anOpenListStillComesBeforeThePresetNameField() {
        ZioInputRouter.Focus both = new ZioInputRouter.Focus(true, false, false, false, true);

        assertEquals(ZioInputRouter.KeyAction.LIST_CLOSE,
            ZioInputRouter.key(ZioInputRouter.KEY_ESCAPE, false, both));
        assertEquals(ZioInputRouter.CharAction.LIST_SEARCH, ZioInputRouter.charTyped('p', both));
    }

    @Test
    void theWheelBelongsToTheOpenListUnderThePointer() {
        assertEquals(ZioInputRouter.ScrollDecision.LIST, ZioInputRouter.scroll(true));
        assertEquals(ZioInputRouter.ScrollDecision.PANEL, ZioInputRouter.scroll(false));
    }

    @Test
    void theScrollOfAListIsBoundedWhenDrivenByTheWheel() {
        int rowHeight = ZioListLayout.ROW_HEIGHT;
        int entries = ZioListLayout.LIST_ROWS + 3;
        double max = ZioListLayout.maxScroll(entries);

        assertEquals(0, ZioInputRouter.scrollBy(0, 1, entries, rowHeight));
        assertEquals(rowHeight, ZioInputRouter.scrollBy(0, -1, entries, rowHeight));
        assertEquals(max, ZioInputRouter.scrollBy(max, -50, entries, rowHeight));
        assertEquals(0, ZioInputRouter.scrollBy(0, 50, entries, rowHeight));

        // A list that fits does not move at all.
        assertEquals(0, ZioInputRouter.scrollBy(0, -1, ZioListLayout.LIST_ROWS, rowHeight));
    }

    @Test
    void aClickInAnUnfoldedListLandsOnTheRightPart() {
        int rowTop = 100;

        assertEquals(ZioListLayout.ClickZone.HEADER, ZioListLayout.listZone(rowTop, rowTop));
        assertEquals(ZioListLayout.ClickZone.HEADER,
            ZioListLayout.listZone(rowTop + ZioListLayout.ROW_HEIGHT - 1, rowTop));

        assertEquals(ZioListLayout.ClickZone.SEARCH,
            ZioListLayout.listZone(rowTop + ZioListLayout.ROW_HEIGHT + 3, rowTop));

        double entriesTop = rowTop + ZioListLayout.ROW_HEIGHT + 3 + 13 + 3;

        assertEquals(ZioListLayout.ClickZone.ENTRIES, ZioListLayout.listZone(entriesTop, rowTop));
        assertEquals(ZioListLayout.ClickZone.ENTRIES, ZioListLayout.listZone(
            entriesTop + ZioListLayout.LIST_ROWS * ZioListLayout.ROW_HEIGHT - 1, rowTop));

        // The padding under the last entry and the line below the list are nobody's.
        assertEquals(ZioListLayout.ClickZone.NONE,
            ZioListLayout.listZone(entriesTop + ZioListLayout.LIST_ROWS * ZioListLayout.ROW_HEIGHT, rowTop));
        assertEquals(ZioListLayout.ClickZone.NONE,
            ZioListLayout.listZone(rowTop + ZioListLayout.rowHeight(false, true), rowTop));
    }

    @Test
    void theEntryUnderThePointerFollowsTheScroll() {
        int rowTop = 100;
        double entriesTop = rowTop + ZioListLayout.ROW_HEIGHT + 3 + 13 + 3;

        assertEquals(0, ZioListLayout.entryIndex(entriesTop, rowTop, 0));
        assertEquals(1, ZioListLayout.entryIndex(entriesTop + ZioListLayout.ROW_HEIGHT, rowTop, 0));
        assertEquals(1, ZioListLayout.entryIndex(entriesTop, rowTop, ZioListLayout.ROW_HEIGHT));
        assertEquals(-1, ZioListLayout.entryIndex(entriesTop - 1, rowTop, 0));
    }

    @Test
    void clickingACheckboxWritesIntoTheSettingAndTheValueReadsBack() {
        FakeSetting setting = new FakeSetting(List.of("a", "b"));

        // The click produces the new value through the pure layer, then the setting is given it.
        setting.set(ZioListLayout.toggled(setting.get(), "c"));

        assertEquals(List.of("a", "b", "c"), setting.get());
        assertTrue(ZioListLayout.selected(setting.get(), "c"));

        // Clicking the same box again takes it back out, and the read side agrees.
        setting.set(ZioListLayout.toggled(setting.get(), "c"));

        assertEquals(List.of("a", "b"), setting.get());
        assertFalse(ZioListLayout.selected(setting.get(), "c"));
    }

    /** The smallest thing that behaves like a list setting: a value that can be read and replaced. */
    private static final class FakeSetting {
        private final List<String> values = new ArrayList<>();

        private FakeSetting(List<String> initial) {
            values.addAll(initial);
        }

        private List<String> get() {
            return values;
        }

        private void set(List<String> next) {
            values.clear();
            values.addAll(next);
        }
    }
}
