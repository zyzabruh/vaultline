package com.pro0101.vaultline.gui.widgets;

import com.pro0101.vaultline.gui.PresetTheme;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

/**
 * The sounds a preset theme plays while the pointer moves over its widgets and presses them.
 *
 * <p>The client this interface copies plays a clip from a folder of sound files for each of the
 * three events. The addon ships no audio, so the same three events are wired to Minecraft's own
 * sounds and each theme lets them be swapped: a tick while the pointer enters a control, a click
 * when it is pressed, and a second click for the right button. Volume is a percentage of the game's
 * sound volume, and the whole thing can be turned off per theme.
 *
 * <p>Both hooks are in mixins on Meteor's {@code WWidget} and {@code WPressable}, so every control
 * of the theme — including Meteor's own — is covered without subclassing any of them again.
 */
public final class GuiSounds {
    /** Minimum gap between two hover sounds, in milliseconds; a fast pointer would machine-gun them. */
    private static final long HOVER_GAP = 35;

    /** The files the copied client plays, copied into this addon as they are. */
    private static final Identifier HOVER_FILE = Identifier.of("vaultline", "gui.hover");
    private static final Identifier CLICK_FILE = Identifier.of("vaultline", "gui.click");
    private static final Identifier RIGHT_CLICK_FILE = Identifier.of("vaultline", "gui.rclick");

    private static long lastHover;
    private static boolean loggedSoundFailure;

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("Vaultline GUI sounds");

    private GuiSounds() {
    }

    /** The sounds a theme can be set to. */
    public enum GuiSound {
        NONE(null, null),
        CLICK(SoundEvents.UI_BUTTON_CLICK, null),
        HAT(SoundEvents.BLOCK_NOTE_BLOCK_HAT, null),
        PLING(SoundEvents.BLOCK_NOTE_BLOCK_PLING, null),
        PICKUP(null, SoundEvents.ENTITY_ITEM_PICKUP),
        TOAST(null, SoundEvents.UI_TOAST_IN);

        private final RegistryEntry<SoundEvent> entry;
        private final SoundEvent event;

        GuiSound(RegistryEntry<SoundEvent> entry, SoundEvent event) {
            this.entry = entry;
            this.event = event;
        }
    }

    /** Plays a theme's hover sound, if the theme is playing sounds at all. */
    public static void hover(GuiTheme widgetTheme) {
        PresetTheme theme = activeTheme(widgetTheme);
        if (theme == null || !theme.soundsEnabled()) return;

        long now = System.currentTimeMillis();
        if (now - lastHover < HOVER_GAP) return;
        lastHover = now;

        if (theme.portedSounds()) {
            playFile(HOVER_FILE, 1.0f, theme.soundVolume());
            return;
        }

        play(theme.hoverSound(), 1.5f, theme.soundVolume());
    }

    /** Plays a theme's click sound for the button that was released. */
    public static void click(GuiTheme widgetTheme, int button) {
        PresetTheme theme = activeTheme(widgetTheme);
        if (theme == null || !theme.soundsEnabled()) return;

        if (theme.portedSounds()) {
            playFile(button == 0 ? CLICK_FILE : RIGHT_CLICK_FILE, 1.0f, theme.soundVolume());
            return;
        }

        play(button == 0 ? theme.clickSound() : theme.rightClickSound(), 1.0f, theme.soundVolume());
    }

    /** Plays one of the copied client's own files, as the theme asks for. */
    private static void playFile(Identifier file, float pitch, double volumePercent) {
        MinecraftClient mc = MeteorClient.mc;
        if (mc == null || mc.getSoundManager() == null) return;

        float volume = (float) (Math.max(0, Math.min(100, volumePercent)) / 100.0);
        if (volume <= 0) return;

        // A sound is a finishing touch: a broken one drops out instead of taking the screen with it.
        try {
            mc.getSoundManager().play(new GuiSoundInstance(file, volume, pitch));
        } catch (RuntimeException e) {
            if (!loggedSoundFailure) {
                LOG.warn("Could not play " + file + "; the GUI will stay silent", e);
                loggedSoundFailure = true;
            }
        }
    }

    /** Returns the theme a widget belongs to, or null when it is not one of ours or not on show. */
    private static PresetTheme activeTheme(GuiTheme widgetTheme) {
        MinecraftClient mc = MeteorClient.mc;
        if (mc == null || !(mc.currentScreen instanceof WidgetScreen)) return null;

        PresetTheme theme = PresetTheme.owning(widgetTheme);
        return theme != null && theme.soundsEnabled() ? theme : null;
    }

    private static void play(GuiSound sound, float pitch, double volumePercent) {
        if (sound == null || sound == GuiSound.NONE) return;

        MinecraftClient mc = MeteorClient.mc;
        if (mc == null || mc.getSoundManager() == null) return;

        float volume = (float) (Math.max(0, Math.min(100, volumePercent)) / 100.0);
        if (volume <= 0) return;

        SoundEvent event = sound.event != null ? sound.event : sound.entry.value();

        // A UI sound is not positional: playing it through the sound manager keeps it at full
        // volume wherever the player stands, which a world sound at the player's feet does not.
        mc.getSoundManager().play(PositionedSoundInstance.master(event, pitch, volume));
    }
}
