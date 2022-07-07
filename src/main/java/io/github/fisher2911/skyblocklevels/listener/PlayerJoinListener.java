package io.github.fisher2911.skyblocklevels.listener;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.SpecialSkyItem;
import io.github.fisher2911.skyblocklevels.user.BukkitUser;
import io.github.fisher2911.skyblocklevels.user.UserManager;
import io.github.fisher2911.skyblocklevels.util.Keys;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;

public class PlayerJoinListener implements Listener {

    private final SkyblockLevels plugin;
    private final UserManager userManager;

    public PlayerJoinListener(SkyblockLevels plugin) {
        this.plugin = plugin;
        this.userManager = plugin.getUserManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        this.userManager.loadUser(player.getUniqueId());
//        PacketHelper.sendMiningFatiguePacket(player);
        player.getInventory().addItem(new ItemStack(Material.OAK_SAPLING));
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            for (ItemStack itemStack : player.getInventory()) {
                if (itemStack == null) continue;
                final String type = Keys.getSkyItemId(itemStack);
                if (type.isBlank()) continue;
                final String clazz = Keys.getSkyItemClass(itemStack);
                this.plugin.getLogger().info("class: " + clazz);
                this.plugin.getLogger().info("type: " + type);
                if (clazz == null) continue;
                final long id = Keys.getSkyItem(itemStack);
                this.plugin.getLogger().info("Id is: " + id);
                if (id == -1) continue;
                final SpecialSkyItem item = this.plugin.getDataManager().loadItem(clazz, type, id);
                if (item == SpecialSkyItem.EMPTY) continue;
                this.plugin.getItemManager().registerItem(item);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final BukkitUser user = this.userManager.removeUser(player.getUniqueId());
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            final Multimap<Class<?>, SpecialSkyItem> map = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);
            for (ItemStack itemStack : player.getInventory()) {
                if (itemStack == null) continue;
                final SpecialSkyItem skyItem = this.plugin.getItemManager().getItem(itemStack);
                if (skyItem == SpecialSkyItem.EMPTY) continue;
                map.put(skyItem.getClass(), skyItem);
            }
            for (var entry : map.asMap().entrySet()) {
                this.plugin.getDataManager().saveItems(entry.getValue(), entry.getKey());
            }
        });
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.userManager.saveUser(user));
    }
}
