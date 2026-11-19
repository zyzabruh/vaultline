package com.pro0101.vaultline.modules.render;

import com.pro0101.vaultline.VaultlineAddon;
import com.pro0101.vaultline.render.SkyboxImage;
import com.pro0101.vaultline.render.SkyboxRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

/**
 * Replaces the sky with a fullscreen shader.
 *
 * <p>The sky is drawn by {@link SkyboxRenderer} in the sky pass, with the vanilla gradient
 * cancelled. Each mode is a separate fragment shader, so switching mode changes the shader rather
 * than a setting inside one.
 */
public class Skybox extends Module {
    public enum Mode {
        Aurora,
        Nebula,
        Sunset,
        Storm,
        Twilight,
        Galaxy,
        Ocean,
        Ember,
        Image
    }

    private final SettingGroup general = this.settings.getDefaultGroup();
    private final SettingGroup dimensions = this.settings.createGroup("Dimensions");

    private final Setting<Mode> mode = this.general.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Which sky shader is drawn.")
        .defaultValue(Mode.Aurora)
        .build()
    );

    private final Setting<String> imageFile = this.general.add(new StringSetting.Builder()
        .name("image-file")
        .description("PNG inside config/vaultline/skybox/, drawn as an equirectangular panorama. A missing file falls back to Sunset.")
        .defaultValue("sky.png")
        .visible(() -> this.mode.get() == Mode.Image)
        .build()
    );

    private final Setting<SettingColor> tint = this.general.add(new ColorSetting.Builder()
        .name("tint")
        .description("Colour the shader output is multiplied by.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );

    private final Setting<Double> animationSpeed = this.general.add(new DoubleSetting.Builder()
        .name("animation-speed")
        .description("Multiplier for every animated sky. Zero freezes the motion.")
        .defaultValue(1.0)
        .range(0.0, 3.0)
        .visible(() -> this.mode.get() != Mode.Sunset)
        .build()
    );

    private final Setting<Double> intensity = this.general.add(new DoubleSetting.Builder()
        .name("intensity")
        .description("Brightness of the coloured part of the sky. The tint is an accent, so this changes the aurora, the lightning or the halo, not the whole image.")
        .defaultValue(1.0)
        .range(0.2, 2.0)
        .sliderRange(0.2, 2.0)
        .build()
    );

    private final Setting<Boolean> followWorldTime = this.general.add(new BoolSetting.Builder()
        .name("follow-world-time")
        .description("Fades night skies out during the day: the accent follows the sun instead of being painted over a blue sky.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> rainDims = this.general.add(new BoolSetting.Builder()
        .name("rain-dims")
        .description("Dims the accent while it rains, the way the vanilla sky darkens.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> hideClouds = this.general.add(new BoolSetting.Builder()
        .name("hide-clouds")
        .description("Hides the vanilla clouds, which would otherwise drift in front of the shader sky.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> darkSky = this.general.add(new BoolSetting.Builder()
        .name("replace-dark-sky")
        .description("Hides the dark disc vanilla paints below the horizon, so the shader covers the whole sky instead of stopping at it.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> darkFog = this.general.add(new BoolSetting.Builder()
        .name("replace-dark-fog")
        .description("In the Nether and the End, fades the terrain into the fog colour below instead of the vanilla one, which would otherwise sit against the shader as a wall.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> fog = this.general.add(new ColorSetting.Builder()
        .name("fog-color")
        .description("Fog colour used in the Nether and the End while the shader sky is drawn.")
        .defaultValue(new SettingColor(18, 16, 28, 255))
        .visible(this.darkFog::get)
        .build()
    );

    private final Setting<Boolean> celestial = this.general.add(new BoolSetting.Builder()
        .name("hide-celestial-bodies")
        .description("Also hides the vanilla sun, moon, stars and sunrise tint, which would otherwise be drawn over the shader.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> overworld = this.dimensions.add(new BoolSetting.Builder()
        .name("overworld")
        .description("Draw the shader sky in the Overworld.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> nether = this.dimensions.add(new BoolSetting.Builder()
        .name("nether")
        .description("Draw the shader sky in the Nether, which has no sky of its own.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> end = this.dimensions.add(new BoolSetting.Builder()
        .name("end")
        .description("Draw the shader sky in the End, in place of its own sky box.")
        .defaultValue(true)
        .build()
    );

    public Skybox() {
        super(VaultlineAddon.CATEGORY, "skybox",
            "Replaces the sky with a fullscreen shader."
        );
    }

    @Override
    public String getInfoString() {
        return this.mode.get().name();
    }

    /**
     * Loads the panorama ahead of the first frame that needs it, so opening the mode does not stall
     * the sky pass on a file read.
     */
    @Override
    public void onActivate() {
        if (this.mode.get() == Mode.Image) SkyboxImage.get(this.imageFile.get());
    }

    public void draw() {
        SettingColor color = this.tint.get();

        SkyboxRenderer.render(
            this.mode.get(),
            SkyboxRenderer.accent(
                color.r / 255.0F, color.g / 255.0F, color.b / 255.0F,
                this.accentScale()
            ),
            this.imageFile.get(),
            this.animationSpeed.get().floatValue()
        );
    }

    /**
     * Folds brightness, time of day and rain into the one factor the accent is scaled by.
     *
     * @return accent scale, 0 to 2
     */
    private float accentScale() {
        float scale = this.intensity.get().floatValue();

        if (this.followWorldTime.get() && this.mc.world != null) {
            scale *= SkyboxRenderer.nightFactor(this.mc.world.getTimeOfDay());
        }

        if (this.rainDims.get() && this.mc.world != null) {
            scale *= SkyboxRenderer.rainFactor(
                this.mc.world.getRainGradient(this.mc.getRenderTickCounter().getTickProgress(false))
            );
        }

        return scale;
    }

    public Mode mode() {
        return this.mode.get();
    }

    /**
     * Returns the tint applied to the sky, for previews.
     *
     */
    public SettingColor tintColor() {
        return this.tint.get();
    }

    /**
     * Reports whether the dark fog colour should be replaced.
     *
     * @return true when the fog colour follows the sky
     */
    public boolean hideDarkFog() {
        return this.darkFog.get();
    }

    /**
     * Returns the fog colour used in the dark dimensions.
     *
     */
    public SettingColor fog() {
        return this.fog.get();
    }

    /**
     * Reports whether the dark sky of the Nether and the End should be replaced.
     *
     * @return true when the shader replaces the dark sky
     */
    public boolean hideDarkSky() {
        return this.darkSky.get();
    }

    /**
     * Reports whether the shader should be drawn in the dimension the player is in.
     *
     * <p>A shader sky suits the Overworld far better than the cramped Nether, and the End has its
     * own look, so each dimension can be left alone. The check is repeated by every render hook,
     * which lets the plain vanilla sky come back in one dimension without disabling the module.
     *
     * @return true when the current dimension is allowed to use the shader
     */
    public boolean enabledInDimension() {
        if (this.mc.world == null) return true;

        var key = this.mc.world.getRegistryKey();

        if (key == net.minecraft.world.World.OVERWORLD) return this.overworld.get();
        if (key == net.minecraft.world.World.NETHER) return this.nether.get();
        if (key == net.minecraft.world.World.END) return this.end.get();

        return true;
    }

    /**
     * Reports whether the vanilla sun and moon should be cancelled.
     *
     * @return true when the celestial bodies are hidden
     */
    public boolean hideCelestial() {
        return this.celestial.get();
    }

    public boolean hideClouds() {
        return this.hideClouds.get();
    }
}
