package com.pro0101.vaultline.modules.huntbuddy.sorter;

import meteordevelopment.meteorclient.systems.friends.Friends;
import net.minecraft.entity.player.PlayerEntity;

import static meteordevelopment.meteorclient.MeteorClient.mc;

final class SorterSafety {
    static final long PAUSE_LIMIT_NANOS = 120_000_000_000L;
    static final long RESUME_DELAY_NANOS = 5_000_000_000L;

    String waitingReason(boolean ignoreFriends) {
        if (mc.world == null || mc.player == null) {
            return null;
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || player.getUuid().equals(mc.player.getUuid())) {
                continue;
            }
            if (ignoreFriends && Friends.get().isFriend(player)) {
                continue;
            }
            return "Player visible: " + player.getName().getString();
        }

        return null;
    }
}
