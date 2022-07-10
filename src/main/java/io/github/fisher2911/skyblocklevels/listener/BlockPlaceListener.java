package io.github.fisher2911.skyblocklevels.listener;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.ItemManager;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.user.UserManager;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;

public class BlockPlaceListener implements Listener {

    private final SkyblockLevels plugin;
    private final ItemManager itemManager;
    private final UserManager userManager;
    
    public BlockPlaceListener(SkyblockLevels plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
        this.userManager = plugin.getUserManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final User user = this.userManager.getUser(player);
        if (user == null) return;
        this.itemManager.handle(user, event.getItemInHand(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGrow(BlockGrowEvent event) {
        this.itemManager.handle(User.SERVER, this.plugin.getWorlds().getBlockAt(WorldPosition.fromLocation(event.getBlock().getLocation())), event);
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGrow(BlockSpreadEvent event) {
        this.itemManager.handle(User.SERVER, this.plugin.getWorlds().getBlockAt(WorldPosition.fromLocation(event.getBlock().getLocation())), event);
    }

}
