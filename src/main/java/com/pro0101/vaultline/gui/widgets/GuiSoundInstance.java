package com.pro0101.vaultline.gui.widgets;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;

/**
 * One of the GUI sounds, played from the addon's own sound files.
 *
 * <p>The sound manager already knows every entry of {@code sounds.json} by identifier, so a sound can
 * be played without a registered {@code SoundEvent}: this instance asks the manager for the set under
 * its identifier instead of resolving one through the registry. That is the part a vanilla
 * {@code PositionedSoundInstance} cannot do for a file that is not in the game's registry, and it is
 * why the copied client ships its own audio instead of sound events.
 */
public final class GuiSoundInstance implements SoundInstance {
    private final Identifier sound;
    private final float volume;
    private final float pitch;

    public GuiSoundInstance(Identifier sound, float volume, float pitch) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public Identifier getId() {
        return sound;
    }

    @Override
    public WeightedSoundSet getSoundSet(SoundManager manager) {
        return manager.get(sound);
    }

    /**
     * The sound to play, resolved from the manager's own table.
     *
     * <p>It cannot be left null: the sound system dereferences it without checking, and a missing
     * entry has to come back as the game's own placeholder rather than as nothing at all.
     */
    @Override
    public Sound getSound() {
        SoundManager manager = MinecraftClient.getInstance().getSoundManager();
        WeightedSoundSet set = manager == null ? null : manager.get(sound);

        if (set == null) return SoundManager.MISSING_SOUND;

        Sound resolved = set.getSound(SoundInstance.createRandom());
        return resolved != null ? resolved : SoundManager.MISSING_SOUND;
    }

    @Override
    public SoundCategory getCategory() {
        return SoundCategory.MASTER;
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public boolean isRelative() {
        return true;
    }

    @Override
    public int getRepeatDelay() {
        return 0;
    }

    @Override
    public float getVolume() {
        return volume;
    }

    @Override
    public float getPitch() {
        return pitch;
    }

    @Override
    public double getX() {
        return 0;
    }

    @Override
    public double getY() {
        return 0;
    }

    @Override
    public double getZ() {
        return 0;
    }

    @Override
    public AttenuationType getAttenuationType() {
        return AttenuationType.NONE;
    }

    @Override
    public boolean shouldAlwaysPlay() {
        return true;
    }
}
