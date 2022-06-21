package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.user.User;
import org.bukkit.event.player.PlayerInteractEvent;

public interface Usable {

    SkyItem getItem();
    void onUse(User user, PlayerInteractEvent event);

}
