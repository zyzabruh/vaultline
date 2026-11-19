package com.pro0101.vaultline.modules.hyperglide.utilities;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.PlayerInput;
import java.util.function.BooleanSupplier;

public final class Flight {
    private final MinecraftClient client = MinecraftClient.getInstance();

    private static final Flight instance = new Flight();

    private static final int grace = 3;
    private static final int chest = 6;
    private static final int gliding = 7;

    private boolean enabled;
    private boolean active;

    private int slot = -1;
    private boolean restart;
    private boolean restore;

    private int dry;
    private boolean liquid;

    private BooleanSupplier request;

    private Flight() {}


    public static Flight get() {
        return instance;
    }

    public void enabled(boolean enabled) {
        if (this.enabled == enabled) return;

        this.enabled = enabled;
        if (!enabled) this.clear();
    }

    public boolean spoof() {
        return this.enabled && !Baritone.elytra()
            && !this.liquid && !Player.liquid();
    }

    public boolean standby() {
        return this.spoof() && !this.active;
    }

    /**
     * @return true when spoofing preparation cannot complete
     */
    public boolean blocked() {
        return this.spoof() && !this.prepare();
    }

    public boolean available() {
        return this.spoof() ? this.ready() : Elytra.equipped();
    }

    public boolean ready() {
        if (!this.spoof()) return false;

        if (!Client.interaction() || !Inventory.ready()) {
            return false;
        }

        if (this.active || this.prepared()) {
            return true;
        }

        if (!Elytra.equipped()) return false;
        return Hotbar.find(Elytra::chestplate) >= 0;
    }

    public boolean active() {
        return this.spoof() && this.active;
    }

    public boolean mute() {
        return this.spoof() && (this.active || this.prepared());
    }



    public boolean activate() {
        if (!this.spoof()) return false;
        if (this.active) return true;

        if (!Client.interaction() || !Inventory.ready() ||
            !this.client.player.isGliding() || !this.prepare()) {
            return false;
        }

        this.slot = Elytra.hotbar();
        if (this.slot < 0) return false;

        this.active = true;
        this.restart = false;
        this.restore = false;

        return true;
    }

    public boolean start() {
        if (!this.spoof()) return false;
        if (this.active) return true;

        if (!this.prepare() ||
            this.client.player.isOnGround()) {
            return false;
        }

        this.active = true;
        this.restart = true;
        this.restore = false;

        if (this.begin()) return true;

        this.clear();
        return false;
    }

    public void tick() {
        this.liquid();

        if (!this.active) return;

        if (!this.enabled || !Client.ready()) {
            this.clear();
            return;
        }

        if (!this.spoof()) {
            this.normal();
            return;
        }

        this.sprint();

        if (!this.continuing()) {
            this.clear();
            return;
        }

        if (this.restart && !this.restore) {
            this.begin();
        }
    }

    public void finish() {
        if (!this.restore || !Client.ready() ||
            !this.client.player.isGliding()) {
            return;
        }

        this.flush();

        if (Elytra.equipped() && this.slot >= 0) {
            this.inventory();
            Inventory.swap(chest, this.slot);
        }

        this.slot = -1;
        this.restore = false;
    }

    public boolean normal() {
        if (!Client.interaction() || !Inventory.ready()) {
            return Elytra.equipped();
        }

        if (Elytra.equipped()) {
            this.reset();
            return true;
        }

        int slot = Elytra.hotbar();
        if (slot < 0) return false;

        this.inventory();
        Inventory.swap(chest, slot);

        if (!Elytra.equipped()) {
            return false;
        }

        this.reset();
        return true;
    }

    private void liquid() {
        if (!Client.ready()) {
            this.dry = 0;
            return;
        }

        if (Player.liquid()) {
            this.liquid = true;
            this.dry = 0;
            return;
        }

        if (!this.liquid) {
            this.dry = 0;
            return;
        }

        if (this.client.player.isOnGround() &&
            !this.client.player.isGliding()) {
            this.liquid = false;
            this.dry = 0;
            return;
        }

        if (!this.client.player.isGliding()) {
            this.dry = 0;
            return;
        }

        if (++this.dry < grace) return;

        this.liquid = false;
        this.dry = 0;
    }



    /**
     * Runs an action now or remembers it for later.
     */
    public boolean request(BooleanSupplier action) {
        if (action == null) return false;
        if (!this.active()) return action.getAsBoolean();

        if (this.request == null) this.request = action;
        return true;
    }

    public boolean request() {
        return this.request(Elytra::firework);
    }

    /**
     * Keeps the local gliding flag when the server clears it.
     */
    public byte sync(byte flags) {
        boolean flying = (flags & (1 << gliding)) != 0;
        if (!this.active() || flying) return flags;

        if (!this.continuing()) {
            this.clear();
            return flags;
        }

        if (!this.restore) this.restart = true;
        return (byte) (flags | (1 << gliding));
    }



    public boolean prepare() {
        if (!this.spoof()) return false;

        if (!Client.interaction() || !Inventory.ready()) {
            return false;
        }

        if (this.prepared()) return true;
        if (!Elytra.equipped()) return false;

        int slot = Hotbar.find(Elytra::chestplate);
        if (slot < 0) return false;

        this.inventory();
        Inventory.swap(chest, slot);

        return this.prepared();
    }

    private boolean prepared() {
        if (Elytra.equipped()) return false;

        if (!Client.interaction() || !Inventory.ready()) {
            return false;
        }

        return Elytra.chestplate() && Elytra.hotbar() >= 0;
    }

    private boolean begin() {
        if (!this.prepared() || !this.continuing()) {
            return false;
        }

        int slot = Elytra.hotbar();
        if (slot < 0) return false;

        this.slot = slot;

        this.inventory();
        Inventory.swap(chest, slot);

        this.jump(false);
        Elytra.start();
        this.jump(true);

        Packets.pong(Integer.MIN_VALUE);

        this.restart = false;
        this.restore = true;

        this.flush();
        return true;
    }



    /**
     * Runs a pending action while the elytra is equipped.
     */
    private void flush() {
        if (this.request == null) return;

        if (this.request.getAsBoolean()) {
            this.request = null;
        }
    }

    private void inventory() {
        this.sprint();
        Packets.input(Packets.state(false, false, false));
    }

    private void sprint() {
        if (this.client.player.isSprinting()) {
            Packets.command(ClientCommandC2SPacket.Mode.STOP_SPRINTING);
            this.client.player.setSprinting(false);
        }

        PlayerInput input = Packets.state();
        if (!input.sprint()) return;

        this.client.player.input.playerInput =
            Packets.state(input.jump(), false);
    }

    /**
     * Sends the jump state used around the takeoff packet.
     */
    private void jump(boolean pressed) {
        Packets.sync(Packets.state(pressed, false));
    }



    private boolean continuing() {
        return Client.ready() && !Player.liquid()
            && !this.client.player.isOnGround()
            && !this.client.player.hasVehicle()
            && !this.client.player.getAbilities().flying
            && !this.client.player.hasStatusEffect(StatusEffects.LEVITATION);
    }

    private void clear() {
        if (this.restore && Client.ready() &&
            Elytra.equipped() && this.slot >= 0) {

            this.inventory();
            Inventory.swap(chest, this.slot);
        }

        this.reset();
    }

    private void reset() {
        this.active = false;
        this.restart = false;
        this.restore = false;

        this.slot = -1;
        this.request = null;
    }

}
