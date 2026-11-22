package com.pro0101.vaultline.modules.bephax.searcharea;

import com.pro0101.vaultline.VaultlineAddon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import static com.pro0101.vaultline.modules.bephax.util.Utils.*;
import java.io.*;

public class SearchAreaMode
{

    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    protected final SearchArea searchArea;
    protected final MinecraftClient mc;
    private final SearchAreaModes type;
    protected long paused = 0;

    public SearchAreaMode(SearchAreaModes type) {
        this.searchArea = Modules.get().get(SearchArea.class);
        this.mc = MinecraftClient.getInstance();
        this.type = type;
    }

    public void onTick()
    {

    }

    public void onActivate()
    {

    }

    public void onDeactivate()
    {
        setPressed(mc.options.forwardKey, false);
    }

    public void onMessageReceive(ReceiveMessageEvent event)
    {
        if (event.getMessage().getString().contains("joined the game"))
        {
            paused = (long) (System.nanoTime() + 1e10);
        }
    }

    protected File getJsonFile(String fileName) {
        return new File(new File(new File(MeteorClient.FOLDER, "search-area"), searchArea.saveLocation.get()), fileName + ".json");
    }

    protected void saveToJson(boolean goingToStart, PathingData pd)
    {
        if (pd == null) return;
        // The last position does not matter when disconnecting on the way to the start.
        if (!goingToStart) pd.currPos = mc.player.getBlockPos();
        try {
            File file = getJsonFile(type.toString());
            file.getParentFile().mkdirs();
            Writer writer = new FileWriter(file);
            GSON.toJson(pd, writer);
            writer.close();
        } catch (IOException e) {
            VaultlineAddon.LOG.error("Could not save the search area path", e);
        }
    }

    protected static class PathingData
    {
        public BlockPos initialPos;
        public BlockPos currPos;
        public float yawDirection;
        public boolean mainPath;
    }

    public void clear()
    {
        clear(type.toString());
    }

    public void clear(String mode)
    {
        File file = getJsonFile(mode);
        file.delete();
    }

    public void clearAll()
    {
        for (SearchAreaModes mode : SearchAreaModes.values())
        {
            clear(mode.toString());
        }
    }

    @Override
    public String toString()
    {
        return type.toString();
    }

}
