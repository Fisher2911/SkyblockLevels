package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.user.User;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public interface SkyBlock {

    void onBreak(User user, BlockBreakEvent event);

    void onPlace(User user, BlockPlaceEvent event);

}
