package io.github.fisher2911.skyblocklevels.listener;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.user.BukkitUser;
import io.github.fisher2911.skyblocklevels.user.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

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
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final BukkitUser user = this.userManager.removeUser(player.getUniqueId());
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            this.userManager.saveUser(user);
        });
    }
}
