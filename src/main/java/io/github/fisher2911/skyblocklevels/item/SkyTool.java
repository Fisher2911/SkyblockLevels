package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.user.User;
import org.bukkit.event.block.BlockBreakEvent;

public interface SkyTool extends Usable {

    void onBreak(User user, BlockBreakEvent event);

}
