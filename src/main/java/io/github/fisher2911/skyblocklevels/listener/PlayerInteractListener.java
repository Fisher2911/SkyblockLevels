package io.github.fisher2911.skyblocklevels.listener;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.ItemManager;
import io.github.fisher2911.skyblocklevels.item.SkyBlock;
import io.github.fisher2911.skyblocklevels.item.SpecialSkyItem;
import io.github.fisher2911.skyblocklevels.item.Usable;
import io.github.fisher2911.skyblocklevels.user.BukkitUser;
import io.github.fisher2911.skyblocklevels.user.UserManager;
import io.github.fisher2911.skyblocklevels.util.Keys;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import io.github.fisher2911.skyblocklevels.world.Worlds;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Objects;

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
        final BukkitUser user = this.userManager.getUser(player);
        if (user == null) return;
        final WorldPosition position = WorldPosition.fromLocation(event.getClickedBlock().getLocation());
        this.itemManager.handle(user, this.worlds.getBlockAt(position), event);
        if (event.getItem() == null) return;
        final SpecialSkyItem item = this.itemManager.getItem(Keys.getSkyItem(event.getItem()));
        if (item.equals(SpecialSkyItem.EMPTY)) return;
        if (item instanceof Usable) {
            this.itemManager.handle(user, event.getItem(), event);
        }
        if (item instanceof SkyBlock && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            final Block placing = event.getClickedBlock().getRelative(event.getBlockFace());
            if (!placing.getWorld().getNearbyEntities(placing.getBoundingBox()).isEmpty()) return;
            final BlockPlaceEvent placeEvent = new BlockPlaceEvent(
                    placing,
                    placing.getState(),
                    event.getClickedBlock(),
                    event.getItem(),
                    player,
                    true,
                    Objects.requireNonNullElse(event.getHand(), EquipmentSlot.HAND)
            );
            this.itemManager.handle(user, event.getItem(), placeEvent);
            if (placeEvent.isCancelled()) event.setCancelled(true);
        }
    }

}
