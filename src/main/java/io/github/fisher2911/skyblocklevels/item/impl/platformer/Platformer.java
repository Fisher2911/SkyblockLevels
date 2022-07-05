package io.github.fisher2911.skyblocklevels.item.impl.platformer;

import com.google.common.collect.Multimaps;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.item.SkyTool;
import io.github.fisher2911.skyblocklevels.placeholder.Transformer;
import io.github.fisher2911.skyblocklevels.user.User;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public abstract class Platformer implements SkyTool {

    protected static final Map<Class<?>, Transformer<Object>> PLATFORMER_PLACEHOLDERS = Map.of(Platformer.class,
            Transformer.builder(Multimaps.newSetMultimap(new HashMap<>(), HashSet::new)).
                    with("%amount%", p -> ((Platformer) p).getUsesLeft()).
                    build()
    );

    protected final SkyblockLevels plugin;
    protected final long id;
    protected final String itemId;
    protected final ItemSupplier itemSupplier;

    public Platformer(SkyblockLevels plugin, long id, String itemId, ItemSupplier itemSupplier) {
        this.plugin = plugin;
        this.id = id;
        this.itemId = itemId;
        this.itemSupplier = itemSupplier;;
    }

    @Override
    public void onBreak(User user, BlockBreakEvent event) {

    }

    @Override
    public ItemStack getItemStack() {
        return this.itemSupplier.get(PLATFORMER_PLACEHOLDERS, this);
    }

    @Override
    public String getItemId() {
        return this.itemId;
    }

    @Override
    public long getId() {
        return this.id;
    }

    // returns amount of placed blocks
    protected int place(PlayerInteractEvent event, Material material, int radius, int amount) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return 0;
        if (event.getBlockFace() != BlockFace.UP) return 0;
        final Location location = event.getClickedBlock().getLocation();
        final int minX = location.getBlockX() - radius;
        final int maxX = location.getBlockX() + radius;
        final int minZ = location.getBlockZ() - radius;
        final int maxZ = location.getBlockZ() + radius;

        int placed = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (placed >= amount) break;
                final Block block = location.getWorld().getBlockAt(x, location.getBlockY(), z);
                if (block.getType() == Material.AIR) {
                    block.setType(material);
                    placed++;
                }
            }
        }
        return placed;
    }

    @Override
    public void takeDamage(ItemStack itemStack, int damage) {

    }

    protected abstract String getUsesLeft();

}
