package io.github.fisher2911.skyblocklevels;

import io.github.fisher2911.skyblocklevels.item.ItemManager;
import io.github.fisher2911.skyblocklevels.listener.BlockBreakListener;
import io.github.fisher2911.skyblocklevels.listener.BlockPlaceListener;
import io.github.fisher2911.skyblocklevels.listener.PlayerInteractListener;
import io.github.fisher2911.skyblocklevels.listener.PlayerJoinListener;
import io.github.fisher2911.skyblocklevels.user.UserManager;
import io.github.fisher2911.skyblocklevels.world.Worlds;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;

public final class SkyblockLevels extends JavaPlugin {

    private Worlds worlds;
    private ItemManager itemManager;
    private UserManager userManager;

    @Override
    public void onEnable() {
        this.worlds = new Worlds(this, new HashMap<>());
        this.itemManager = new ItemManager(this, new HashMap<>());
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
                new BlockBreakListener(this),
                new BlockPlaceListener(this),
                this.worlds
        ).forEach(this::registerListener);
    }

    public void registerListener(Listener listener) {
        this.getServer().getPluginManager().registerEvents(listener, this);
    }

    public ItemManager getItemManager() {
        return this.itemManager;
    }

    public UserManager getUserManager() {
        return this.userManager;
    }

    public Worlds getWorlds() {
        return this.worlds;
    }
}
