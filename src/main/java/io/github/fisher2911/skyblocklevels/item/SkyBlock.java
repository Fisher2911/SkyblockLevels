package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public interface SkyBlock extends SpecialSkyItem {

    void onBreak(User user, BlockBreakEvent event);

    void onMineBlock(User user, Block block);

    void onBlockDamage(User user, BlockDamageEvent event);

    void onPlace(User user, BlockPlaceEvent event);

    void onClick(User user, PlayerInteractEvent event);

    void tick(WorldPosition worldPosition);

    boolean isAsync();

    SkyBlock EMPTY = new SkyBlock() {
        @Override
        public ItemStack getItemStack() { return ItemBuilder.EMPTY.build(); }
        @Override
        public void onBreak(User user, BlockBreakEvent event) {}
        @Override
        public void onMineBlock(User user, Block block) {}
        @Override
        public void onBlockDamage(User user, BlockDamageEvent event) {}
        @Override
        public void onPlace(User user, BlockPlaceEvent event) {}
        @Override
        public void onClick(User user, PlayerInteractEvent event) {}
        @Override
        public void tick(WorldPosition worldPosition) {}
        @Override
        public boolean isAsync() { return true; }
        @Override
        public String getItemId() { return ""; }
        @Override
        public long getId() { return -1; }
        @Override
        public boolean uniqueInInventory() { return false; }
    };

    static boolean isEmpty(SkyBlock skyBlock) {
        return skyBlock == EMPTY;
    }

}
