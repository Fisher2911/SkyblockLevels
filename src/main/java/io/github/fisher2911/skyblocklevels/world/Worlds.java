package io.github.fisher2911.skyblocklevels.world;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.SkyBlock;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Worlds implements Listener {

    private final SkyblockLevels plugin;
    private final Map<UUID, WorldManager> worlds;

    public Worlds(SkyblockLevels plugin, Map<UUID, WorldManager> worlds) {
        this.plugin = plugin;
        this.worlds = worlds;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        final World world = event.getWorld();
        final WorldManager worldManager = new WorldManager(this.plugin, world, new ConcurrentHashMap<>());
        this.plugin.registerListener(worldManager);
        this.worlds.put(world.getUID(), worldManager);
        worldManager.startTicking();
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        final World world = event.getWorld();
        final WorldManager worldManager = this.worlds.remove(world.getUID());
        if (worldManager == null) return;
        worldManager.shutdown();
        HandlerList.unregisterAll(worldManager);
    }

    public void addBlock(SkyBlock block, WorldPosition position) {
        final WorldManager worldManager = this.worlds.get(position.getWorld().getUID());
        if (worldManager == null) return;
        worldManager.addBlock(block, position);
    }

    public void removeBlock(WorldPosition worldPosition) {
        final WorldManager worldManager = this.worlds.get(worldPosition.getWorld().getUID());
        if (worldManager == null) return;
        worldManager.removeBlock(worldPosition);
    }

    public SkyBlock getBlockAt(WorldPosition worldPosition) {
        final WorldManager worldManager = this.worlds.get(worldPosition.getWorld().getUID());
        if (worldManager == null) return SkyBlock.EMPTY;
        return worldManager.getBlockAt(worldPosition.getPosition());
    }
}
