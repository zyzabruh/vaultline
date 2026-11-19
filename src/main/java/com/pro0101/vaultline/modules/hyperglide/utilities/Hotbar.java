package com.pro0101.vaultline.modules.hyperglide.utilities;

import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.block.Block;
import net.minecraft.block.FallingBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Handles common hotbar searches and slot changes.
 */
public final class Hotbar {
    private static final MinecraftClient client = MinecraftClient.getInstance();

    private Hotbar() {}

    public static int find(Item item) {
        return find(stack -> stack.isOf(item));
    }

    public static int find(Predicate<ItemStack> predicate) {
        return find(0, 8, predicate);
    }

    /**
     * Finds the first matching stack inside a hotbar range.
     *
     * @param first first hotbar slot
     * @param last last hotbar slot
     * @return matching hotbar slot, or -1 when unavailable
     */
    public static int find(int first, int last,
        Predicate<ItemStack> predicate) {

        int start = Math.min(first, last);
        int end = Math.max(first, last);

        for (int slot = start; slot <= end; slot++) {
            if (predicate.test(stack(slot))) return slot;
        }

        return -1;
    }

    /**
     * Finds a matching stack while checking the selected slot first.
     *
     * @return matching hotbar slot, or -1 when unavailable
     */
    public static int first(Predicate<ItemStack> predicate) {
        int selected = selected();
        if (predicate.test(stack(selected))) {
            return selected;
        }

        for (int slot = 0; slot < 9; slot++) {
            if (slot != selected && predicate.test(stack(slot))) {
                return slot;
            }
        }

        return -1;
    }

    public static int best(
        Predicate<ItemStack> predicate,
        ToIntFunction<ItemStack> score) {

        int slot = -1;
        int best = Integer.MIN_VALUE;

        for (int idx = 0; idx < 9; idx++) {
            ItemStack stack = stack(idx);
            if (!predicate.test(stack)) continue;

            int current = score.applyAsInt(stack);
            if (current <= best) continue;

            best = current;
            slot = idx;
        }

        return slot;
    }

    /**
     * Finds an allowed full cube block in the hotbar.
     *
     * @param filter block list used to select blocks
     * @param pos destination used to check the collision shape
     * @return matching hotbar slot, or -1 when unavailable
     */
    public static int block(BlockFilter filter, BlockPos pos) {
        return find(stack -> {
            if (!(stack.getItem() instanceof BlockItem item)) {
                return false;
            }

            Block block = item.getBlock();
            if (!filter.allowed(block) || block instanceof FallingBlock) {
                return false;
            }

            return Block.isShapeFullCube(
                block.getDefaultState().getCollisionShape(client.world, pos)
            );
        });
    }

    public static int count(Item item) {
        int count = 0;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = stack(slot);
            if (stack.isOf(item)) count += stack.getCount();
        }

        return count;
    }

    public static int selected() {
        return API.slot(client.player.getInventory());
    }

    public static ItemStack stack(int slot) {
        return client.player.getInventory().getStack(slot);
    }

    public static void set(int slot) {
        client.player.getInventory().setSelectedSlot(slot);
    }

    public static void sync(int slot) {
        Packets.slot(slot);
    }

    /**
     * Changes the selected hotbar slot on the client and server.
     *
     */
    public static void select(int slot) {
        set(slot);
        sync(slot);
    }

    /**
     * Temporarily swaps an inventory item into the selected hotbar slot.
     *
     * @param slot hotbar or inventory slot to use
     * @return true when the swap was prepared
     */
    public static boolean swap(int slot) {
        return InvUtils.swap(slot, true);
    }

    /**
     * Restores the hotbar state saved by the last temporary swap.
     */
    public static void restore() {
        InvUtils.swapBack();
    }
}
