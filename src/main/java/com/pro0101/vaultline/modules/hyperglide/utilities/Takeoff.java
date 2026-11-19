package com.pro0101.vaultline.modules.hyperglide.utilities;

import net.minecraft.client.MinecraftClient;

/**
 * Controls the jump input needed to start elytra flight.
 */
public final class Takeoff {
    private static final MinecraftClient client = MinecraftClient.getInstance();

    private final Flight flight = Flight.get();

    private boolean active;
    private boolean pressed;
    private boolean ground;

    public void start() {
        this.start(false);
    }

    public void start(boolean ground) {
        this.active = true;
        this.pressed = false;
        this.ground = ground;
    }

    public void pulse() {
        if (!this.active) return;

        if (!this.ready()) {
            this.reset();
            return;
        }

        if (client.player.isOnGround()) {
            this.jump();
            return;
        }

        if (this.flight.spoof()) {
            this.spoof();
            return;
        }

        this.pressed = !this.pressed;
    }

    private boolean ready() {
        return Client.loaded()
            && this.flight.available()
            && !client.player.isGliding();
    }

    private void jump() {
        if (!this.ground) {
            this.reset();
        } else {
            this.pressed = true;
        }
    }

    private void spoof() {
        if (this.flight.active()) {
            this.pressed = Player.liquid();
            return;
        }

        if (!this.flight.start()) return;

        if (Player.liquid()) {
            this.pressed = true;
        } else {
            this.reset();
        }
    }

    public void reset() {
        this.active = false;
        this.pressed = false;
        this.ground = false;
    }

    public boolean active() {
        return this.active;
    }

    public boolean ground() {
        return this.ground;
    }

    public boolean pressed() {
        return this.pressed;
    }
}
