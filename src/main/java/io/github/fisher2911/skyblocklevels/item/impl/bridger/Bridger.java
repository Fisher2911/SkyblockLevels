package io.github.fisher2911.skyblocklevels.item.impl.bridger;

import com.google.common.collect.Multimaps;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.item.SkyTool;
import io.github.fisher2911.skyblocklevels.placeholder.Transformer;
import io.github.fisher2911.skyblocklevels.user.User;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public abstract class Bridger implements SkyTool {

    protected static final Map<Class<?>, Transformer<Object>> BRIDGER_PLACEHOLDERS = Map.of(Bridger.class,
            Transformer.builder(Multimaps.newSetMultimap(new HashMap<>(), HashSet::new)).
                    with("%amount%", b -> ((Bridger) b).getUsesLeft()).
                    build()
    );

    protected final SkyblockLevels plugin;
    protected final long id;
    protected final String itemId;
    protected final ItemSupplier itemSupplier;

    public Bridger(SkyblockLevels plugin, long id, String itemId, ItemSupplier itemSupplier) {
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
        return this.itemSupplier.get(BRIDGER_PLACEHOLDERS, this);
    }

    @Override
    public String getItemId() {
        return this.itemId;
    }

    @Override
    public void onUse(User user, PlayerInteractEvent event) {

    }

    @Override
    public long getId() {
        return this.id;
    }

    // returns amount of placed blocks
    protected int place(PlayerInteractEvent event, Material material, int amount) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return 0;
        final Player player = event.getPlayer();
        if (event.getBlockFace() != BlockFace.UP) return 0;
        Block block = event.getClickedBlock();
        final BlockFace facing = player.getFacing();
        int placed = 0;
        while (amount > 0) {
            final Block newBlock = block.getRelative(facing);
            if (!newBlock.getType().isAir()) return placed;
            newBlock.setType(material);
            amount--;
            placed++;
            block = newBlock;
        }
        return placed;
    }

    protected abstract String getUsesLeft();
}
