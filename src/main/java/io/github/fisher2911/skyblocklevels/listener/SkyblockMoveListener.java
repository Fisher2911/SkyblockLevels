package io.github.fisher2911.skyblocklevels.listener;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import io.github.fisher2911.skyblocklevels.world.Worlds;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class SkyblockMoveListener implements Listener {

    private final SkyblockLevels plugin;
    private final Worlds worlds;

    public SkyblockMoveListener(SkyblockLevels plugin) {
        this.plugin = plugin;
        this.worlds = this.plugin.getWorlds();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonPush(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            if (this.worlds.isSkyBlock(WorldPosition.fromLocation(b.getLocation()))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) {
            if (this.worlds.isSkyBlock(WorldPosition.fromLocation(b.getLocation()))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(b -> this.worlds.isSkyBlock(WorldPosition.fromLocation(b.getLocation())));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(b -> this.worlds.isSkyBlock(WorldPosition.fromLocation(b.getLocation())));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobChangeBlock(EntityChangeBlockEvent event) {
        if (this.worlds.isSkyBlock(WorldPosition.fromLocation(event.getBlock().getLocation()))) {
            event.setCancelled(true);
        }
    }

}
