package com.pro0101.vaultline.modules.visual;

import com.pro0101.vaultline.VaultlineAddon;
import com.pro0101.vaultline.render.HandFx;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

/**
 * Draws the first-person hands through Vaultline's own pipelines.
 *
 * <p>Two toggles, both off by default: the fill tints the arm and the held item, the outline adds a
 * rim along the silhouette, and both together give the glow. The geometry, the poses and the
 * lighting are the game's; only the render type, and with it the pipeline and the fragment stage,
 * come from the mod. With both off nothing is intercepted and the hands are exactly vanilla.
 */
public class HandShader extends Module {
    /** Module id as it appears in the config and in the module list. */
    public static final String ID = "hand-shader";

    /** Setting name of the fill toggle. */
    public static final String HAND_FILL = "hand-fill";

    /** Setting name of the outline toggle. */
    public static final String HAND_OUTLINE = "hand-outline";

    private final SettingGroup general = this.settings.getDefaultGroup();

    private final Setting<Boolean> handFill = this.general.add(new BoolSetting.Builder()
        .name(HAND_FILL)
        .description("Tints the first-person hands and the held item with the Vaultline accent. Off means off only when the outline is off too.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> handOutline = this.general.add(new BoolSetting.Builder()
        .name(HAND_OUTLINE)
        .description("Draws a rim along the silhouette of the first-person hands. Off means off only when the fill is off too.")
        .defaultValue(false)
        .build()
    );

    /**
     * Builds the module.
     */
    public HandShader() {
        super(VaultlineAddon.VISUALS, ID, "Draws the first-person hands with the mod's fill and outline instead of vanilla.");
    }

    /**
     * Whether the hands take the tinted fill.
     *
     * @return the hand-fill setting
     */
    public boolean handFill() {
        return this.handFill.get();
    }

    /**
     * Whether the hands take the rim.
     *
     * @return the hand-outline setting
     */
    public boolean handOutline() {
        return this.handOutline.get();
    }

    /**
     * The two toggles as the renderer reads them.
     *
     * <p>Both toggles off still gives the module a visible look: an enabled module that draws
     * nothing would be a setting the user has to guess.
     *
     * @return current hand settings
     */
    public HandFx.Settings current() {
        return new HandFx.Settings(handFill(), handOutline());
    }
}
