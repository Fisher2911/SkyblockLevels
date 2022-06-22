package io.github.fisher2911.skyblocklevels.item.impl;

import io.github.fisher2911.skyblocklevels.item.SkyItem;
import io.github.fisher2911.skyblocklevels.item.SkyTool;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.util.ItemBuilder;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ExplosionTool implements SkyTool {

    private final SkyItem skyItem = new SkyItem("explosion_tool", ItemBuilder.from(Material.WOODEN_PICKAXE));

    @Override
    public void onBreak(User user, BlockBreakEvent event) {
        final Location location = event.getBlock().getLocation();
        final WorldPosition position = WorldPosition.fromLocation(location);
        position.getWorld().createExplosion(location, 1);
    }

    @Override
    public SkyItem getSkyItem() {
        return this.skyItem;
    }

    @Override
    public void onUse(User user, PlayerInteractEvent event) {
        user.sendMessage("Break a block to use me!");
    }
}
