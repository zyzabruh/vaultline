package com.pro0101.vaultline.modules.hyperglide.utilities;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class Elytra {
    private static final MinecraftClient client = MinecraftClient.getInstance();

    private Elytra() {}

    /**
     * @return true when an elytra is equipped in the chest slot
     */
    public static boolean equipped() {
        return client.player.getEquippedStack(
            EquipmentSlot.CHEST
        ).isOf(Items.ELYTRA);
    }

    public static boolean chestplate() {
        return chestplate(client.player.getEquippedStack(
            EquipmentSlot.CHEST
        ));
    }

    /**
     * Checks whether a stack can be worn as a chestplate.
     */
    public static boolean chestplate(ItemStack stack) {
        if (stack.isEmpty() || stack.isOf(Items.ELYTRA)) {
            return false;
        }

        EquippableComponent equipment = stack.get(
            DataComponentTypes.EQUIPPABLE
        );

        if (equipment == null) return false;
        return equipment.slot() == EquipmentSlot.CHEST;
    }

    /**
     * @return matching hotbar slot, or -1 when unavailable
     */
    public static int hotbar() {
        return Hotbar.best(
            stack -> stack.isOf(Items.ELYTRA),
            Elytra::remaining
        );
    }

    public static int remaining(ItemStack stack) {
        if (!stack.isDamageable()) return Integer.MAX_VALUE;
        return stack.getMaxDamage() - stack.getDamage();
    }

    public static void start() {
        Packets.command(ClientCommandC2SPacket.Mode.START_FALL_FLYING);
    }

    public static boolean firework() {
        if (client.player == null) return false;

        float yaw = client.player.getYaw();
        float pitch = client.player.getPitch();

        return firework(yaw, pitch);
    }

    public static boolean firework(float yaw, float pitch) {
        if (!Client.interaction() || !Inventory.ready()) {
            return false;
        }

        int slot = Hotbar.first(
            stack -> stack.isOf(Items.FIREWORK_ROCKET)
        );

        if (slot >= 0) {
            int selected = Hotbar.selected();
            if (selected != slot) Hotbar.select(slot);

            try {
                ItemStack stack = client.player.getMainHandStack();
                if (!stack.isOf(Items.FIREWORK_ROCKET)) {
                    return false;
                }

                Packets.item(Hand.MAIN_HAND, yaw, pitch);
                return true;
            } finally {
                if (selected != slot) Hotbar.select(selected);
            }
        }

        ItemStack stack = client.player.getOffHandStack();
        if (stack.isOf(Items.FIREWORK_ROCKET)) {
            Packets.item(Hand.OFF_HAND, yaw, pitch);
            return true;
        }

        return false;
    }

    /**
     * Calculates elytra movement for the next tick.
     *
     */
    public static Vec3d glide(Vec3d velocity, Vec3d rotation) {
        return glide(velocity, rotation, 0.08);
    }

    public static Vec3d glide(Vec3d velocity, Vec3d rotation, double gravity) {
        double length = rotation.horizontalLength();
        double speed = velocity.horizontalLength();

        double cosine = length * length * Math.min(1.0, rotation.length() / 0.4);
        velocity = velocity.add(0.0, gravity * (cosine * 0.75 - 1.0), 0.0);

        return glide(velocity, rotation, length, speed, cosine);
    }

    /**
     * Applies the remaining elytra movement for the current tick.
     *
     */
    private static Vec3d glide(Vec3d velocity, Vec3d rotation,
        double length, double speed, double cosine) {

        velocity = fall(velocity, rotation, length, cosine);
        velocity = rise(velocity, rotation, length, speed);
        velocity = align(velocity, rotation, length, speed);

        return velocity.multiply(0.99, 0.98, 0.99);
    }

    private static Vec3d fall(Vec3d velocity,
        Vec3d rotation, double length, double cosine) {

        if (velocity.y < 0.0 && length > 0.0) {
            double lift = velocity.y * -0.1 * cosine;

            velocity = velocity.add(
                rotation.x * lift / length,
                lift,
                rotation.z * lift / length
            );
        }

        return velocity;
    }

    private static Vec3d rise(Vec3d velocity,
        Vec3d rotation, double length, double speed) {

        double angle = MathHelper.clamp(-rotation.y, -1.0, 1.0);
        angle = Math.asin(angle);

        if (angle < 0.0 && length > 0.0) {
            double lift = speed * -Math.sin(angle) * 0.04;

            velocity = velocity.add(
                -rotation.x * lift / length,
                lift * 3.2,
                -rotation.z * lift / length
            );
        }

        return velocity;
    }

    private static Vec3d align(Vec3d velocity,
        Vec3d rotation, double length, double speed) {

        if (length > 0.0) {
            velocity = velocity.add(
                (rotation.x / length * speed - velocity.x) * 0.1,
                0.0,
                (rotation.z / length * speed - velocity.z) * 0.1
            );
        }

        return velocity;
    }
}
