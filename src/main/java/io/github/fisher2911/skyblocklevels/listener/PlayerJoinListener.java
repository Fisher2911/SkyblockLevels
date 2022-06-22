package io.github.fisher2911.skyblocklevels.listener;

import io.github.fisher2911.skyblocklevels.user.BukkitUser;
import io.github.fisher2911.skyblocklevels.user.Collection;
import io.github.fisher2911.skyblocklevels.user.UserManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;

public class PlayerJoinListener implements Listener {

    private final UserManager userManager;

    public PlayerJoinListener(UserManager userManager) {
        this.userManager = userManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.userManager.addUser(new BukkitUser(event.getPlayer().getUniqueId(), new Collection(new HashMap<>())));
    }
}
