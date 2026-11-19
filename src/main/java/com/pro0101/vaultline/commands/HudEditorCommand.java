package com.pro0101.vaultline.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pro0101.vaultline.gui.zio.ZioHudEditorScreen;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

/** Opens the ported HUD editor. */
public class HudEditorCommand extends Command {
    public HudEditorCommand() {
        super("hud-editor", "Opens the ported HUD editor.", "hudedit");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (mc.player == null) {
                error("§cNot in-game!");
                return SINGLE_SUCCESS;
            }

            mc.setScreen(new ZioHudEditorScreen());

            return SINGLE_SUCCESS;
        });
    }
}
