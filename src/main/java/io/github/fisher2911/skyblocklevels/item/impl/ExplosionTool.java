package io.github.fisher2911.skyblocklevels.item.impl;

import io.github.fisher2911.skyblocklevels.item.ItemBuilder;
import io.github.fisher2911.skyblocklevels.item.SkyTool;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ExplosionTool implements SkyTool {

    private final ItemStack itemStack = ItemBuilder.from(Material.WOODEN_PICKAXE).build();
    private final long id;

    @Nullable
    private WorldPosition currentlyBreaking;
    private long lastBreakTime;

    public ExplosionTool(long id) {
        this.id = id;
    }

    @Override
    public void onBreak(User user, BlockBreakEvent event) {
        final Location location = event.getBlock().getLocation();
        final WorldPosition position = WorldPosition.fromLocation(location);
        position.getWorld().createExplosion(location, 1);
    }

    @Override
    public ItemStack getItemStack() {
        return this.itemStack;
    }

    @Override
    public void onUse(User user, PlayerInteractEvent event) {
    }

    @Override
    public String getItemId() {
        return "explosion_tool";
    }

    @Override
    public long getId() {
        return this.id;
    }

    @Override
    public boolean uniqueInInventory() {
        return true;
    }
}
