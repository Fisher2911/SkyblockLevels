package io.github.fisher2911.skyblocklevels.listener;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.ItemManager;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.user.UserManager;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import io.github.fisher2911.skyblocklevels.world.Worlds;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerInteractListener implements Listener {

    private final Worlds worlds;
    private final ItemManager itemManager;
    private final UserManager userManager;

    public PlayerInteractListener(SkyblockLevels plugin) {
        this.itemManager = plugin.getItemManager();
        this.userManager = plugin.getUserManager();
        this.worlds = plugin.getWorlds();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final User user = this.userManager.getUser(player);
        if (user == null) return;
        if (event.getItem().getType() == Material.BEDROCK) {
            this.itemManager.giveItem(user, "explosion_tool");
        }
        this.itemManager.handle(user, event.getItem(), event);
        final WorldPosition position = WorldPosition.fromLocation(event.getClickedBlock().getLocation());
        this.itemManager.handle(user, this.worlds.getBlockAt(position).getSkyItem().getId(), event);
    }

}
