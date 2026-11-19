package com.pro0101.vaultline.modules.hyperglide.utilities;

import com.pro0101.vaultline.mixins.InteractionAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Centralizes client-to-server packet helpers used by modules.
 */
public final class Packets {
    private static final MinecraftClient client = MinecraftClient.getInstance();

    private Packets() {}

    /**
     * Sends a player action through the sequenced interaction path.
     *
     * @param pos block position
     * @param side block face
     */
    public static void action(Action action, BlockPos pos, Direction side) {
        ((InteractionAccessor) client.interactionManager)
            .hyperglide$sequence(client.world, sequence ->
                new PlayerActionC2SPacket(action, pos, side, sequence)
            );
    }

    /**
     * @param pos block position
     * @param side block face
     */
    public static void direct(Action action, BlockPos pos, Direction side) {
        client.getNetworkHandler().sendPacket(
            new PlayerActionC2SPacket(action, pos, side)
        );
    }

    /**
     * Sends a block interaction through the sequenced interaction path.
     *
     */
    public static void block(Hand hand, BlockHitResult hit) {
        ((InteractionAccessor) client.interactionManager)
            .hyperglide$sequence(client.world, sequence ->
                new PlayerInteractBlockC2SPacket(hand, hit, sequence)
            );
    }

    /**
     * Sends a block interaction with an explicit sequence number.
     *
     */
    public static void block(Hand hand, BlockHitResult hit, int sequence) {
        client.getNetworkHandler().sendPacket(
            new PlayerInteractBlockC2SPacket(hand, hit, sequence)
        );
    }

    /**
     * Sends an item-use interaction through the sequenced interaction path.
     *
     */
    public static void item(Hand hand, float yaw, float pitch) {
        ((InteractionAccessor) client.interactionManager)
            .hyperglide$sequence(client.world, sequence ->
                new PlayerInteractItemC2SPacket(hand, sequence, yaw, pitch)
            );
    }

    public static void command(ClientCommandC2SPacket.Mode mode) {
        client.getNetworkHandler().sendPacket(
            new ClientCommandC2SPacket(client.player, mode)
        );
    }

    /**
     * Sends the supplied player input without modifying local input.
     *
     */
    public static void input(PlayerInput input) {
        client.getNetworkHandler().sendPacket(
            new PlayerInputC2SPacket(input)
        );
    }

    /**
     * Builds and sends player input from the supplied movement states.
     *
     */
    public static void input(boolean forward,
        boolean backward, boolean left, boolean right,
        boolean jump, boolean sneak, boolean sprint) {

        input(new PlayerInput(
            forward, backward, left, right,
            jump, sneak, sprint
        ));
    }

    public static void sync(PlayerInput input) {
        input(input);
        client.player.input.playerInput = input;
    }

    public static PlayerInput state() {
        return client.player.input.playerInput;
    }

    /**
     * Copies the current input while replacing jump and sprint states.
     *
     */
    public static PlayerInput state(boolean jump, boolean sprint) {
        PlayerInput input = state();

        return new PlayerInput(
            input.forward(), input.backward(),
            input.left(), input.right(),
            jump, input.sneak(), sprint
        );
    }

    /**
     * Copies the current input while replacing jump and sprint states.
     *
     * @param movement whether to preserve directional movement
     */
    public static PlayerInput state(
        boolean movement, boolean jump, boolean sprint) {

        PlayerInput input = state();

        return new PlayerInput(
            movement && input.forward(),
            movement && input.backward(),
            movement && input.left(),
            movement && input.right(),
            jump, input.sneak(), sprint
        );
    }

    public static PlayerInput forward() {
        PlayerInput input = state();

        return new PlayerInput(
            true, false, input.left(), input.right(),
            input.jump(), input.sneak(), input.sprint()
        );
    }

    /**
     * Copies the current input while replacing the sneak state.
     *
     */
    public static PlayerInput sneak(boolean sneak) {
        PlayerInput input = state();

        return new PlayerInput(
            input.forward(), input.backward(),
            input.left(), input.right(),
            input.jump(), sneak, input.sprint()
        );
    }

    /**
     * Sends a pong packet as a separator between packet groups.
     *
     */
    public static void pong(int parameter) {
        client.getNetworkHandler().sendPacket(
            new CommonPongC2SPacket(parameter)
        );
    }

    /**
     * @param slot hotbar slot to select
     */
    public static void slot(int slot) {
        client.getNetworkHandler().sendPacket(
            new UpdateSelectedSlotC2SPacket(slot)
        );
    }
}
