package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.user.User;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;

public interface SkyTool extends Usable, SpecialSkyItem {

    void onBreak(User user, BlockBreakEvent event);

}
