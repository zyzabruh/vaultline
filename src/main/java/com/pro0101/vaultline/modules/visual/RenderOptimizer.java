package com.pro0101.vaultline.modules.visual;

import com.pro0101.vaultline.VaultlineAddon;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

/**
 * Switches off overlays that get in the way.
 *
 * <p>Most options cancel a method at its head, so the work behind it never happens: the scoreboard
 * is not laid out, the weather pass is not recorded, the fire is not drawn. Full bright is the odd
 * one out — it has no draw call to cancel, so it fills the lightmap texture with white while active,
 * which lights the whole world without touching a block.
 */
public class RenderOptimizer extends Module {
    private final SettingGroup general = this.settings.getDefaultGroup();

    private final Setting<Boolean> fireOverlay = this.general.add(new BoolSetting.Builder()
        .name("no-fire-overlay")
        .description("Hides the flames drawn over the screen while you burn.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> hurtCam = this.general.add(new BoolSetting.Builder()
        .name("no-hurt-cam")
        .description("Stops the camera from tilting when you take damage.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> particles = this.general.add(new BoolSetting.Builder()
        .name("no-particles")
        .description("Drops every client particle the game tries to spawn. Large win on a weak GPU, but explosions stop showing.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> weather = this.general.add(new BoolSetting.Builder()
        .name("no-weather")
        .description("Stops rain and snow from being drawn, without changing the weather the server sends.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> scoreboard = this.general.add(new BoolSetting.Builder()
        .name("no-scoreboard")
        .description("Hides the scoreboard on the right of the screen.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> title = this.general.add(new BoolSetting.Builder()
        .name("no-title")
        .description("Hides titles and subtitles, such as the ones shown when joining a server.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> overlayMessage = this.general.add(new BoolSetting.Builder()
        .name("no-overlay-message")
        .description("Hides the action bar text above the hotbar.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> guiBackground = this.general.add(new BoolSetting.Builder()
        .name("no-gui-background")
        .description("Removes the dark tint and the blur behind opened screens while in a world.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> fullBright = this.general.add(new BoolSetting.Builder()
        .name("full-bright")
        .description("Lights the whole world as if every block were fully lit. Overrides the gamma option, which vanilla caps at its normal maximum.")
        .defaultValue(false)
        .build()
    );

    public RenderOptimizer() {
        super(VaultlineAddon.VISUALS, "render-optimizer", "Turns off the fire overlay, the hurt camera, particles, weather and the HUD overlays you do not need.");
    }

    public boolean noFireOverlay() {
        return this.isActive() && this.fireOverlay.get();
    }

    public boolean noHurtCam() {
        return this.isActive() && this.hurtCam.get();
    }

    public boolean noParticles() {
        return this.isActive() && this.particles.get();
    }

    public boolean noWeather() {
        return this.isActive() && this.weather.get();
    }

    public boolean noScoreboard() {
        return this.isActive() && this.scoreboard.get();
    }

    public boolean noTitle() {
        return this.isActive() && this.title.get();
    }

    public boolean noOverlayMessage() {
        return this.isActive() && this.overlayMessage.get();
    }

    /**
     * Reports whether the lightmap should be lit at full brightness.
     *
     * @return true when full bright is on
     */
    public boolean fullBright() {
        return this.isActive() && this.fullBright.get();
    }

    public boolean noGuiBackground() {
        return this.isActive() && this.guiBackground.get();
    }
}
