package io.github.fisher2911.skyblocklevels.listener;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
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
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

public class PlayerInteractListener implements Listener {

    private final SkyblockLevels plugin;
    private final Worlds worlds;
    private final ItemManager itemManager;
    private final UserManager userManager;

    public PlayerInteractListener(SkyblockLevels plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
        this.userManager = plugin.getUserManager();
        this.worlds = plugin.getWorlds();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        BukkitUser user = this.userManager.getUser(player);
        if (user == null) return;
        final ItemStack damaged = event.getItem();
        this.itemManager.handle(user, damaged, event);
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onContainerOpen(InventoryOpenEvent event) {
        final Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof BlockInventoryHolder)) return;
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            for (ItemStack itemStack : inventory) {
                if (itemStack == null) continue;
                final String type = Keys.getSkyItemId(itemStack);
                if (type.isBlank()) continue;
                final String clazz = Keys.getSkyItemClass(itemStack);
                if (clazz == null) continue;
                final long id = Keys.getSkyItem(itemStack);
                if (id == -1) continue;
                final SpecialSkyItem item = this.plugin.getDataManager().loadItem(clazz, type, id);
                if (item == SpecialSkyItem.EMPTY) continue;
                this.plugin.getItemManager().registerItem(item);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onContainerClose(InventoryCloseEvent event) {
        final Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof BlockInventoryHolder)) return;
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            final Multimap<Class<?>, SpecialSkyItem> map = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);
            for (ItemStack itemStack : inventory) {
                if (itemStack == null) continue;
                final SpecialSkyItem skyItem = this.plugin.getItemManager().getItem(itemStack);
                if (skyItem == SpecialSkyItem.EMPTY || !skyItem.uniqueInInventory()) continue;
                map.put(skyItem.getClass(), skyItem);
            }
            for (var entry : map.asMap().entrySet()) {
                this.plugin.getDataManager().saveItems(entry.getValue(), entry.getKey());
            }
        });
    }
}
