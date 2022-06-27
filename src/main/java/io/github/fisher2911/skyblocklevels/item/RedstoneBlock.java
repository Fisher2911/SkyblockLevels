package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.user.User;
import org.bukkit.event.block.BlockRedstoneEvent;

public interface RedstoneBlock extends SpecialSkyItem {

    void onActivate(User user, BlockRedstoneEvent event);

}
