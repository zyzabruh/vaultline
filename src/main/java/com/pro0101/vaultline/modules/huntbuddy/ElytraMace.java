package com.pro0101.vaultline.modules.huntbuddy;

import com.pro0101.vaultline.VaultlineAddon;
import com.pro0101.vaultline.modules.huntbuddy.util.GliderSwap;
import com.pro0101.vaultline.modules.huntbuddy.util.QuietClicks;
import com.pro0101.vaultline.modules.huntbuddy.util.Rockets;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

/**
 * Dives from an elytra glide onto a target and hits it with a mace.
 *
 * <p>The module runs a small state machine. It glides towards the target's hitbox, climbs while it
 * is not high enough above it, then dives with the pitch held at {@code dive-pitch}. Falling is
 * what gives the mace its damage, so the glide is stopped just before the hit: the elytra is
 * swapped for a chestplate, the mace is brought to the hand, the target is attacked, and the
 * elytra goes back on for the climb out.
 *
 * <p>Two rules keep it from trapping the player: a chestplate with Curse of Binding is never
 * swapped in, and the elytra is only removed when a usable one is still in the inventory. The
 * armour swap works through {@link InvUtils} rather than by clicking, so it also works while the
 * player is moving.
 *
 * <p>While the module is diving it reports {@link #isDiving()}, which lets {@code control-fly} and
 * {@code elytra-recast} stand down instead of fighting it for the same rotation.
 */
public class ElytraMace extends Module {
    private enum Stage {
        /** Looking for a target, or waiting for the cooldown to end. */
        Idle,
        /** Gliding towards the target and gaining the height the dive needs. */
        Approach,
        /** Diving on the target with the mace ready. */
        Dive,
        /** The hit landed; the elytra is going back on. */
        Escape
    }

    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgCombat = this.settings.createGroup("Combat");
    private final SettingGroup sgTargets = this.settings.createGroup("Targets");
    private final SettingGroup sgRender = this.settings.createGroup("Render");

    private final Setting<Double> range = this.sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Farthest distance at which a target is picked up, in blocks.")
        .defaultValue(64.0)
        .min(8.0)
        .max(256.0)
        .sliderRange(8.0, 128.0)
        .build()
    );

    private final Setting<Double> height = this.sgGeneral.add(new DoubleSetting.Builder()
        .name("height")
        .description("How far above the target the dive starts, in blocks.")
        .defaultValue(8.0)
        .min(2.0)
        .max(40.0)
        .sliderRange(2.0, 24.0)
        .build()
    );

    private final Setting<Double> approachPitch = this.sgGeneral.add(new DoubleSetting.Builder()
        .name("approach-pitch")
        .description("Pitch held while closing in, and while climbing to the dive height. Positive looks down.")
        .defaultValue(12.0)
        .min(-60.0)
        .max(60.0)
        .sliderRange(-60.0, 60.0)
        .build()
    );

    private final Setting<Double> divePitch = this.sgGeneral.add(new DoubleSetting.Builder()
        .name("dive-pitch")
        .description("Pitch held during the dive. Positive looks down.")
        .defaultValue(55.0)
        .min(10.0)
        .max(90.0)
        .sliderRange(10.0, 90.0)
        .build()
    );

    private final Setting<Boolean> autoBoost = this.sgGeneral.add(new BoolSetting.Builder()
        .name("auto-boost")
        .description("Lights a rocket when the glide slows down and no rocket is attached.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> hitRange = this.sgCombat.add(new DoubleSetting.Builder()
        .name("hit-range")
        .description("Horizontal distance at which the dive turns into a hit.")
        .defaultValue(3.0)
        .min(1.0)
        .max(8.0)
        .sliderRange(1.0, 6.0)
        .build()
    );

    private final Setting<Boolean> swapChest = this.sgCombat.add(new BoolSetting.Builder()
        .name("swap-chestplate")
        .description("Wears a chestplate for the hit, so the elytra is not the armour that takes the landing. A piece with Curse of Binding is never used.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> restoreElytra = this.sgCombat.add(new BoolSetting.Builder()
        .name("restore-elytra")
        .description("Puts the elytra back on and starts gliding again after the hit.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> escapeRocket = this.sgCombat.add(new BoolSetting.Builder()
        .name("escape-rocket")
        .description("Lights a rocket once the elytra is back on, to climb away from the target.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> cooldown = this.sgCombat.add(new IntSetting.Builder()
        .name("cooldown")
        .description("Ticks to wait after a hit before picking up another target.")
        .defaultValue(40)
        .min(10)
        .max(200)
        .sliderRange(10, 200)
        .build()
    );

    private final Setting<Boolean> players = this.sgTargets.add(new BoolSetting.Builder()
        .name("players")
        .description("Target other players. Friends are skipped.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> monsters = this.sgTargets.add(new BoolSetting.Builder()
        .name("monsters")
        .description("Target hostile mobs.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> animals = this.sgTargets.add(new BoolSetting.Builder()
        .name("animals")
        .description("Target passive animals.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> render = this.sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Draws a box on the current target.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> sideColor = this.sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("Fill colour of the target box.")
        .defaultValue(new SettingColor(255, 60, 60, 35))
        .visible(this.render::get)
        .build()
    );

    private final Setting<SettingColor> lineColor = this.sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Outline colour of the target box.")
        .defaultValue(new SettingColor(255, 60, 60, 255))
        .visible(this.render::get)
        .build()
    );

    private static volatile boolean diving;

    private Stage stage = Stage.Idle;
    private Entity target;
    private int cooldownTicks;
    private int boostCooldown;
    private int escapeTicks;
    private boolean struck;

    public ElytraMace() {
        super(VaultlineAddon.CATEGORY, "elytra-mace",
            "Dives from a glide onto a target and hits it with a mace, swapping the elytra for a chestplate on the way down."
        );
    }

    /**
     * Reports whether the module is steering the player down towards a target.
     */
    public static boolean isDiving() {
        return diving;
    }

    @Override
    public void onActivate() {
        this.stage = Stage.Idle;
        this.target = null;
        this.cooldownTicks = 0;
        this.boostCooldown = 0;
        this.escapeTicks = 0;
        this.struck = false;
        diving = false;

        if (this.mc.player != null
            && !GliderSwap.isGlider(this.mc.player.getEquippedStack(EquipmentSlot.CHEST))
            && this.findElytra() == -1) {
            this.warning("No elytra equipped or carried.");
        }
    }

    @Override
    public void onDeactivate() {
        this.stage = Stage.Idle;
        this.target = null;
        diving = false;

        if (this.swapChest.get()) this.wearElytraIfMissing();
    }

    @Override
    public String getInfoString() {
        if (this.target == null || this.mc.player == null) return null;

        return this.target.getName().getString() + ", " + String.format("%.0f", this.mc.player.distanceTo(this.target));
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (this.mc.player == null || this.mc.world == null) return;

        if (this.cooldownTicks > 0) this.cooldownTicks--;
        if (this.boostCooldown > 0) this.boostCooldown--;

        switch (this.stage) {
            case Idle -> this.idle();
            case Approach -> this.approach();
            case Dive -> this.dive();
            case Escape -> this.escape();
        }

        diving = this.stage == Stage.Dive || this.stage == Stage.Approach || this.stage == Stage.Escape;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!this.render.get() || this.target == null) return;

        event.renderer.box(this.target.getBoundingBox(), this.sideColor.get(), this.lineColor.get(),
            ShapeMode.Both, 0);
    }

    // Stages

    private void idle() {
        if (this.struck) {
            this.struck = false;
            this.cooldownTicks = this.cooldown.get();
            return;
        }

        if (this.cooldownTicks > 0) return;
        if (!this.mc.player.isGliding()) {
            this.startGliding();
            return;
        }

        Entity found = this.findTarget();
        if (found == null) return;

        this.target = found;
        this.stage = Stage.Approach;
    }

    private void approach() {
        if (this.targetLost()) return;

        boolean highEnough = this.heightAbove() >= this.height.get();

        this.steer(this.approachPitch.get(), !highEnough);
        if (this.autoBoost.get()) this.boost();

        if (highEnough && this.horizontalDistance() <= this.range.get()) {
            this.stage = Stage.Dive;
        }
    }

    private void dive() {
        if (this.targetLost()) return;
        if (this.mc.player.isOnGround()) {
            this.stage = Stage.Idle;
            this.target = null;
            return;
        }

        this.steer(this.divePitch.get(), false);

        boolean closeEnough = this.horizontalDistance() <= this.hitRange.get();
        boolean falling = this.mc.player.fallDistance > 1.0F || this.heightAbove() <= 3.0;

        if (closeEnough && falling) this.strike();
    }

    private void escape() {
        this.escapeTicks++;

        if (this.escapeTicks > 60) {
            this.stage = Stage.Idle;
            this.target = null;
            this.escapeTicks = 0;
            return;
        }

        if (!this.restoreElytra.get()) {
            this.stage = Stage.Idle;
            this.target = null;
            this.escapeTicks = 0;
            return;
        }

        this.wearElytraIfMissing();

        if (this.mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
            this.startGliding();
            if (this.escapeRocket.get() && this.escapeTicks < 10) this.boost();

            if (this.mc.player.isGliding()) {
                this.stage = Stage.Idle;
                this.target = null;
                this.escapeTicks = 0;
            }
        }
    }

    // Actions

    private void strike() {
        if (this.mc.interactionManager == null || this.target == null) return;

        // Check the mace before giving up the glide: without it there is nothing to land and the
        // player would fall with a chestplate on.
        if (!InvUtils.find(Items.MACE).found()) {
            this.warning("No mace in the inventory, the dive is called off.");
            this.stage = Stage.Idle;
            this.target = null;
            return;
        }

        if (!this.holdMace()) return;

        if (this.mc.player.isGliding()) this.mc.player.stopGliding();

        if (this.swapChest.get()) this.wearChestplate();

        Rotations.rotate(Rotations.getYaw(this.target), Rotations.getPitch(this.target));
        this.mc.interactionManager.attackEntity(this.mc.player, this.target);
        this.mc.player.swingHand(Hand.MAIN_HAND);

        this.struck = true;
        this.stage = Stage.Escape;
        this.escapeTicks = 0;
    }

    private void steer(double pitch, boolean climb) {
        double yaw = Rotations.getYaw(this.target);
        double wantPitch = climb && !this.isAboveTarget() ? -this.divePitch.get() * 0.4 : pitch;

        Rotations.rotate(MathHelper.wrapDegrees(yaw), wantPitch);
    }

    private void boost() {
        if (this.mc.interactionManager == null) return;
        if (!this.mc.player.isGliding() || this.boostCooldown > 0) return;
        if (GliderSwap.rocketAttached(this.mc.player)) return;

        FindItemResult rocket = InvUtils.findInHotbar(Rockets::isPlain);
        if (!rocket.found()) return;

        if (rocket.isOffhand()) {
            this.mc.interactionManager.interactItem(this.mc.player, Hand.OFF_HAND);
            this.mc.player.swingHand(Hand.OFF_HAND);
        } else {
            InvUtils.swap(rocket.slot(), true);
            this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
            this.mc.player.swingHand(Hand.MAIN_HAND);
            InvUtils.swapBack();
        }

        this.boostCooldown = 10;
    }

    private boolean holdMace() {
        FindItemResult hotbar = InvUtils.findInHotbar(Items.MACE);
        if (!hotbar.found()) {
            FindItemResult stored = InvUtils.find(Items.MACE);
            if (!stored.found() || !QuietClicks.readyOrSettle()) return false;

            InvUtils.move().from(stored.slot()).toHotbar(this.mc.player.getInventory().getSelectedSlot());
            return false;
        }

        // Hold the hit for a tick after the swap so the attack is sent with the mace in hand.
        // The mace in the offhand would be selected as if it were a hotbar slot, which points the
        // hand at the wrong item; bring it to the hotbar instead, then hold the hit for a tick.
        if (hotbar.isOffhand()) {
            if (!QuietClicks.readyOrSettle()) return false;
            InvUtils.move().fromOffhand().toHotbar(this.mc.player.getInventory().getSelectedSlot());
            return false;
        }

        if (!hotbar.isMainHand()) {
            InvUtils.swap(hotbar.slot(), false);
            return false;
        }

        return true;
    }

    private void wearChestplate() {
        ItemStack chest = this.mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (!GliderSwap.isGlider(chest)) return;

        int slot = this.findSafeChestplate();
        if (slot == -1) return;

        InvUtils.move().from(slot).toArmor(2);
    }

    private void wearElytraIfMissing() {
        ItemStack chest = this.mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.isOf(Items.ELYTRA)) return;

        int slot = this.findElytra();
        if (slot == -1) return;

        InvUtils.move().from(slot).toArmor(2);
    }

    private int findSafeChestplate() {
        for (int i = 0; i < 36; i++) {
            if (GliderSwap.isSafeChestPiece(this.mc.player.getInventory().getStack(i))) return i;
        }

        return -1;
    }

    private int findElytra() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = this.mc.player.getInventory().getStack(i);
            if (GliderSwap.isGlider(stack) && !stack.willBreakNextUse()) return i;
        }

        return -1;
    }

    private void startGliding() {
        if (this.mc.player.isGliding() || this.mc.player.isOnGround()) return;
        if (!GliderSwap.isGlider(this.mc.player.getEquippedStack(EquipmentSlot.CHEST))) return;

        this.mc.player.startGliding();
        if (this.mc.player.networkHandler != null) {
            this.mc.player.networkHandler.sendPacket(
                new ClientCommandC2SPacket(this.mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING)
            );
        }
    }

    // Target

    private Entity findTarget() {
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;

        for (Entity entity : this.mc.world.getEntities()) {
            if (!this.isTargetable(entity)) continue;

            double distance = this.mc.player.distanceTo(entity);
            if (distance >= bestDistance) continue;

            best = entity;
            bestDistance = distance;
        }

        return best;
    }

    private boolean isTargetable(Entity entity) {
        if (entity == this.mc.player || !entity.isAlive()) return false;

        // The range applies to every kind of target. Checking it only for mobs let a player anywhere
        // in the loaded chunks be picked, which the dive then abandoned on the next tick.
        if (this.mc.player.distanceTo(entity) > this.range.get()) return false;

        if (entity instanceof PlayerEntity player) {
            return this.players.get() && Friends.get().shouldAttack(player);
        }

        if (entity instanceof Monster) return this.monsters.get();

        return entity instanceof AnimalEntity && this.animals.get();
    }

    private boolean targetLost() {
        if (this.target == null || !this.target.isAlive()
            || this.mc.player.distanceTo(this.target) > this.range.get() + 8.0) {
            this.target = null;
            this.stage = Stage.Idle;
            return true;
        }

        return false;
    }

    private boolean isAboveTarget() {
        return this.heightAbove() > 0;
    }

    private double heightAbove() {
        return this.mc.player.getY() - this.target.getBoundingBox().maxY;
    }

    private double horizontalDistance() {
        double dx = this.mc.player.getX() - this.target.getX();
        double dz = this.mc.player.getZ() - this.target.getZ();

        return Math.sqrt(dx * dx + dz * dz);
    }
}
