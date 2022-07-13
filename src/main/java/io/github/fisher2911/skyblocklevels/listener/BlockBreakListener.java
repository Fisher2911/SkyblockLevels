package io.github.fisher2911.skyblocklevels.listener;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.ItemManager;
import io.github.fisher2911.skyblocklevels.item.SkyBlock;
import io.github.fisher2911.skyblocklevels.item.SpecialSkyItem;
import io.github.fisher2911.skyblocklevels.item.Usable;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.user.UserManager;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import io.github.fisher2911.skyblocklevels.world.Worlds;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;

public class BlockBreakListener implements Listener {

    private final SkyblockLevels plugin;
    private final Worlds worlds;
    private final ItemManager itemManager;
    private final UserManager userManager;

    public BlockBreakListener(SkyblockLevels plugin) {
        this.plugin = plugin;
        this.worlds = plugin.getWorlds();
        this.itemManager = plugin.getItemManager();
        this.userManager = plugin.getUserManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final User user = this.userManager.getUser(player);
        if (user == null) return;
        final SkyBlock block = this.worlds.getBlockAt(WorldPosition.fromLocation(event.getBlock().getLocation()));
        if (block == SkyBlock.EMPTY) this.plugin.getUserManager().addCollectionAmount(user, event.getBlock().getType().toString(), 1);
        this.itemManager.handle(user, block, event);
        final SpecialSkyItem skyItem = this.itemManager.getItem(player.getInventory().getItemInMainHand());
        if (!(skyItem instanceof Usable)) return;
        this.itemManager.handle(user, player.getInventory().getItemInMainHand(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDestroy(BlockDestroyEvent event) {
        this.itemManager.handle(User.SERVER, this.worlds.getBlockAt(WorldPosition.fromLocation(event.getBlock().getLocation())), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockDamageEvent event) {
        final Player player = event.getPlayer();
        final User user = this.userManager.getUser(player);
        if (user == null) return;
        this.itemManager.handle(user, this.worlds.getBlockAt(WorldPosition.fromLocation(event.getBlock().getLocation())), event);
    }
}
