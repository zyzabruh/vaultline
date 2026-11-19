package com.pro0101.vaultline;

import com.mojang.logging.LogUtils;
import com.pro0101.vaultline.gui.zio.ZioBindModes;
import com.pro0101.vaultline.gui.zio.ZioGames;
import com.pro0101.vaultline.gui.zio.ZioHudMarginStore;
import com.pro0101.vaultline.gui.zio.ZioHudSizeStore;
import com.pro0101.vaultline.gui.zio.ZioHudSizes;
import com.pro0101.vaultline.gui.zio.ZioScaleStore;
import net.fabricmc.loader.api.FabricLoader;
import com.pro0101.vaultline.commands.ChestTrackerCommand;
import com.pro0101.vaultline.commands.HudEditorCommand;
import com.pro0101.vaultline.hud.ChestCounterHud;
import com.pro0101.vaultline.hud.DiscoveryHud;
import com.pro0101.vaultline.hud.PresetPreviewHud;
import com.pro0101.vaultline.hud.FlightHud;
import com.pro0101.vaultline.hud.StashHud;
import com.pro0101.vaultline.modules.basefinder.BaseFinder;
import com.pro0101.vaultline.modules.bephax.AutoDyeShulkers;
import com.pro0101.vaultline.modules.bephax.BetterStashFinder;
import com.pro0101.vaultline.modules.bephax.chesttracker.ChestTracker;
import com.pro0101.vaultline.modules.bephax.searcharea.SearchArea;
import com.pro0101.vaultline.modules.bephax.StashBrander;
import com.pro0101.vaultline.modules.huntbuddy.ElytraMace;
import com.pro0101.vaultline.modules.huntbuddy.ElytraRecast;
import com.pro0101.vaultline.modules.huntbuddy.HotbarRefill;
import com.pro0101.vaultline.modules.huntbuddy.InventoryCleaner;
import com.pro0101.vaultline.modules.huntbuddy.Pitch40;
import com.pro0101.vaultline.modules.huntbuddy.RocketBoost;
import com.pro0101.vaultline.modules.huntbuddy.RocketFly;
import com.pro0101.vaultline.modules.huntbuddy.SignHistory;
import com.pro0101.vaultline.util.BaritoneCompat;
import com.pro0101.vaultline.util.XaeroCompat;
import com.pro0101.vaultline.modules.huntbuddy.sorter.ShulkerSorter;
import com.pro0101.vaultline.modules.hyperglide.AirPlace;
import com.pro0101.vaultline.modules.hyperglide.AutoPilot;
import com.pro0101.vaultline.modules.hyperglide.BounceFly;
import com.pro0101.vaultline.modules.hyperglide.RocketCycle;
import com.pro0101.vaultline.modules.hyperglide.ControlFly;
import com.pro0101.vaultline.modules.hyperglide.ElytraTweaks;
import com.pro0101.vaultline.modules.hyperglide.EasyAccess;
import com.pro0101.vaultline.modules.render.BlockShader;
import com.pro0101.vaultline.modules.visual.Ambience;
import com.pro0101.vaultline.modules.visual.Animations;
import com.pro0101.vaultline.modules.visual.FlowESP;
import com.pro0101.vaultline.modules.visual.HandShader;
import com.pro0101.vaultline.modules.visual.HitEffect;
import com.pro0101.vaultline.modules.visual.ItemESP;
import com.pro0101.vaultline.modules.visual.JumpCircles;
import com.pro0101.vaultline.modules.visual.NameTags;
import com.pro0101.vaultline.modules.visual.RenderOptimizer;
import com.pro0101.vaultline.modules.visual.StorageESP;
import com.pro0101.vaultline.modules.visual.TotemAnimation;
import com.pro0101.vaultline.modules.visual.Trails;
import com.pro0101.vaultline.modules.visual.TwoDESP;
import com.pro0101.vaultline.modules.visual.Weather;
import com.pro0101.vaultline.modules.visual.WorldParticles;
import com.pro0101.vaultline.modules.render.Shaders;
import com.pro0101.vaultline.modules.render.Skybox;
import com.pro0101.vaultline.modules.hyperglide.FastPortal;
import com.pro0101.vaultline.modules.hyperglide.FastFrame;
import com.pro0101.vaultline.modules.hyperglide.MiningTweaks;
import com.pro0101.vaultline.modules.hyperglide.Navigation;
import com.pro0101.vaultline.modules.hyperglide.Overview;
import com.pro0101.vaultline.modules.stashorganizer.StashOrganizer;
import com.pro0101.vaultline.modules.stashorganizer.StashSessionStore;
import com.pro0101.vaultline.gui.FutureTheme;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.GithubRepo;
import com.pro0101.vaultline.gui.ZioTheme;
import com.pro0101.vaultline.gui.zio.ZioScreen;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.render.RenderAfterWorldEvent;
import meteordevelopment.orbit.listeners.ConsumerListener;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import org.slf4j.Logger;

public class VaultlineAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Stash");
    public static final Category VISUALS = new Category("Visuals");
    public static final HudGroup HUD_GROUP = new HudGroup("Stash");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Vaultline");

        // Baritone assist: AutoPilot's climbing steps go through Baritone, which needs the player
        // lined up on every step. Skip it cleanly when Baritone is not installed.
        if (BaritoneCompat.isAvailable()) BaritoneCompat.register();

        // GUI themes, offered in Meteor's theme dropdown
        GuiThemes.add(new FutureTheme());
        GuiThemes.add(new ZioTheme());

        // The ported ClickGUI, as its own tab next to the client's screens. It is the only way in:
        // picking the theme must not open it a second time.
        Tabs.add(new ZioScreen.ZioTab());

        // The zoom the screen was last left at, in the mod's own config file. The screen reads it on
        // every frame, so binding it here is enough for the choice to survive a restart.
        ZioScaleStore.bind(FabricLoader.getInstance().getConfigDir().resolve(ZioScaleStore.FILE_NAME));
        ZioBindModes.bind(FabricLoader.getInstance().getConfigDir().resolve(ZioBindModes.FILE_NAME));

        // Whether the secret sequence has unlocked the Game panel, in its own file next to the binds.
        ZioGames.bind(FabricLoader.getInstance().getConfigDir().resolve(ZioGames.FILE_NAME));

        // The HUD safe margins the ported HUD editor writes while they are dragged, in their own file
        // next to the zoom. Read by the editor when it opens, so they survive a restart.
        ZioHudMarginStore.bind(FabricLoader.getInstance().getConfigDir().resolve(ZioHudMarginStore.FILE_NAME));

        // The sizes the HUD editor gave to elements, so a resized element comes back at that size.
        ZioHudSizes.bind(FabricLoader.getInstance().getConfigDir().resolve(ZioHudSizeStore.FILE_NAME));

        // The panel blur and bloom run between the world and the GUI, where the frame is complete
        // and still writable.
        MeteorClient.EVENT_BUS.subscribe(new ConsumerListener<>(RenderAfterWorldEvent.class, event -> ZioScreen.applyEffects()));

        // Modules
        Modules.get().add(new StashOrganizer());

        // Modules imported from other addons, GPL-3.0, see CREDITS.md
        Modules.get().add(new FastFrame());
        Modules.get().add(new Overview());
        Modules.get().add(new Navigation());
        Modules.get().add(new AutoPilot());
        Modules.get().add(new AirPlace());
        Modules.get().add(new BounceFly());
        Modules.get().add(new ControlFly());
        Modules.get().add(new ElytraTweaks());
        Modules.get().add(new RocketCycle());
        Modules.get().add(new MiningTweaks());
        Modules.get().add(new ShulkerSorter());
        Modules.get().add(new SignHistory());
        Modules.get().add(new HotbarRefill());
        Modules.get().add(new InventoryCleaner());
        Modules.get().add(new Skybox());
        Modules.get().add(new Shaders());
        Modules.get().add(new BlockShader());
        Modules.get().add(new StorageESP());
        Modules.get().add(new ItemESP());
        Modules.get().add(new NameTags());
        Modules.get().add(new Trails());
        Modules.get().add(new JumpCircles());
        Modules.get().add(new TwoDESP());
        Modules.get().add(new HitEffect());
        Modules.get().add(new Weather());
        Modules.get().add(new WorldParticles());
        Modules.get().add(new Ambience());
        Modules.get().add(new RenderOptimizer());
        Modules.get().add(new HandShader());
        Modules.get().add(new Animations());
        Modules.get().add(new TotemAnimation());
        Modules.get().add(new RocketFly());
        Modules.get().add(new Pitch40());
        Modules.get().add(new ElytraRecast());
        Modules.get().add(new ElytraMace());
        Modules.get().add(new RocketBoost());
        Modules.get().add(new AutoDyeShulkers());
        Modules.get().add(new StashBrander());
        Modules.get().add(new ChestTracker());
        Commands.add(new ChestTrackerCommand());
        Commands.add(new HudEditorCommand());
        Modules.get().add(new BetterStashFinder());
        Modules.get().add(new BaseFinder());
        Modules.get().add(new FlowESP());
        Modules.get().add(new SearchArea());
        Modules.get().add(new FastPortal());
        Modules.get().add(new EasyAccess());

        // Built against Xaero's Minimap, World Map and XaeroPlus. Registered by name so their classes are never loaded
        // when those mods are missing, instead of failing to verify and taking the whole addon down with them.
        if (XaeroCompat.isAvailable()) {
            XaeroCompat.register("com.pro0101.vaultline.modules.huntbuddy.TrailFollower");
            XaeroCompat.register("com.pro0101.vaultline.modules.huntbuddy.ChunkRadar");
        } else {
            LOG.info(
                "Xaero's Minimap, Xaero's World Map and XaeroPlus not found: trail-follower and chunk-radar stay disabled."
            );
        }

        // HUD
        Hud.get().register(StashHud.INFO);
        Hud.get().register(FlightHud.INFO);
        Hud.get().register(DiscoveryHud.INFO);
        Hud.get().register(PresetPreviewHud.INFO);
        Hud.get().register(ChestCounterHud.INFO);

        MeteorClient.EVENT_BUS.subscribe(this);
    }

    /** Re-enables the stash organizer when a saved plan belongs to the world being joined. */
    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        StashOrganizer module = Modules.get().get(StashOrganizer.class);

        if (module == null || module.isActive() || !module.resumesSession()) return;
        if (StashSessionStore.canResume()) module.toggle();
    }

    /**
     * Hides the pause and resume messages the Baritone assist prints while it lines the player up.
     * Baritone prefixes them with its tag unless the tag is turned off, so both forms are caught,
     * but ordinary chat that merely mentions the words is left alone.
     */
    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        String text = event.getMessage().getString().trim();
        if (text.equals("Paused") || text.equals("Resumed")) {
            event.cancel();
        } else if (text.startsWith("[Baritone]") &&
                   (text.contains("Paused") || text.contains("Resumed"))) {
            event.cancel();
        }
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
        Modules.registerCategory(VISUALS);
    }

    @Override
    public String getPackage() {
        return "com.pro0101.vaultline";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("zyzabruh", "vaultline");
    }
}
