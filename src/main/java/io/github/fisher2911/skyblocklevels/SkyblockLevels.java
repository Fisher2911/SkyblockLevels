package io.github.fisher2911.skyblocklevels;

import io.github.fisher2911.skyblocklevels.item.ItemManager;
import io.github.fisher2911.skyblocklevels.listener.BlockBreakListener;
import io.github.fisher2911.skyblocklevels.listener.PlayerInteractListener;
import io.github.fisher2911.skyblocklevels.listener.PlayerJoinListener;
import io.github.fisher2911.skyblocklevels.user.UserManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;

public final class SkyblockLevels extends JavaPlugin {

    private ItemManager itemManager;
    private UserManager userManager;

    @Override
    public void onEnable() {
        this.itemManager = new ItemManager(new HashMap<>());
        this.userManager = new UserManager(new HashMap<>());
        this.registerListeners();
    }

    @Override
    public void onDisable() {
    }

    private void registerListeners() {
        List.of(
                new PlayerJoinListener(this.userManager),
                new PlayerInteractListener(this),
                new BlockBreakListener(this)
        ).forEach(
                listener -> getServer().getPluginManager().registerEvents(listener, this));
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public UserManager getUserManager() {
        return userManager;
    }
}
