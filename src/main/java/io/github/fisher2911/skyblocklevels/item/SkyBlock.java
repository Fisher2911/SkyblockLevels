package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.user.User;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public interface SkyBlock extends SpecialSkyItem {

    void onBreak(User user, BlockBreakEvent event);

    void onPlace(User user, BlockPlaceEvent event);

    SkyBlock EMPTY = new SkyBlock() {
        @Override
        public SkyItem getSkyItem() { return SkyItem.EMPTY; }
        @Override
        public void onBreak(User user, BlockBreakEvent event) {}
        @Override
        public void onPlace(User user, BlockPlaceEvent event) {}
    };

    static boolean isEmpty(SkyBlock skyBlock) {
        return skyBlock == EMPTY;
    }

}
