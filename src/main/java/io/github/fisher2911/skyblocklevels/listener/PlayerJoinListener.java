package io.github.fisher2911.skyblocklevels.listener;

import io.github.fisher2911.skyblocklevels.packet.PacketHelper;
import io.github.fisher2911.skyblocklevels.user.BukkitUser;
import io.github.fisher2911.skyblocklevels.user.Collection;
import io.github.fisher2911.skyblocklevels.user.Cooldowns;
import io.github.fisher2911.skyblocklevels.user.UserManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;

public class PlayerJoinListener implements Listener {

    private final UserManager userManager;

    public PlayerJoinListener(UserManager userManager) {
        this.userManager = userManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        this.userManager.loadUser(player.getUniqueId());
        PacketHelper.sendMiningFatiguePacket(player);
    }
}
