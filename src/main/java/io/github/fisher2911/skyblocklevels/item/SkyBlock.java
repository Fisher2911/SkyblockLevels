package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public interface SkyBlock extends SpecialSkyItem {

    void onBreak(User user, BlockBreakEvent event);

    void onPlace(User user, BlockPlaceEvent event);

    void onClick(User user, PlayerInteractEvent event);

    void tick(WorldPosition worldPosition);

    boolean isAsync();

    SkyBlock EMPTY = new SkyBlock() {
        @Override
        public SkyItem getSkyItem() { return SkyItem.EMPTY; }
        @Override
        public void onBreak(User user, BlockBreakEvent event) {}
        @Override
        public void onPlace(User user, BlockPlaceEvent event) {}
        @Override
        public void onClick(User user, PlayerInteractEvent event) {}
        @Override
        public void tick(WorldPosition worldPosition) {}
        @Override
        public boolean isAsync() { return true; }
    };

    static boolean isEmpty(SkyBlock skyBlock) {
        return skyBlock == EMPTY;
    }

}
