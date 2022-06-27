package io.github.fisher2911.skyblocklevels.listener;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.ItemManager;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.user.UserManager;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import io.github.fisher2911.skyblocklevels.world.Worlds;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

public class BlockPowerListener implements Listener {

    private final Worlds worlds;
    private final ItemManager itemManager;
    private final UserManager userManager;

    public BlockPowerListener(SkyblockLevels plugin) {
        this.itemManager = plugin.getItemManager();
        this.userManager = plugin.getUserManager();
        this.worlds = plugin.getWorlds();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPower(BlockRedstoneEvent event) {
        final WorldPosition position = WorldPosition.fromLocation(event.getBlock().getLocation());
        this.itemManager.handle(User.SERVER, this.worlds.getBlockAt(position), event);
    }

}
