package io.github.fisher2911.skyblocklevels.item.impl.crop;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.item.SkyBlock;
import io.github.fisher2911.skyblocklevels.user.CollectionCondition;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.util.Range;
import io.github.fisher2911.skyblocklevels.util.weight.WeightedList;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public abstract class SkyCrop implements SkyBlock {

    protected final SkyblockLevels plugin;
    protected final long id;
    protected final String itemId;
    protected final Material material;
    protected final ItemSupplier itemSupplier;
    protected final int tickDelay;
    protected final WeightedList<Supplier<ItemStack>> items;
    protected final List<ItemSupplier> guaranteedItems;
    protected final Range itemCount;
    protected final CollectionCondition collectionCondition;
    protected final Set<Material> placeableOn;

    public SkyCrop(
            SkyblockLevels plugin,
            long id,
            String itemId,
            Material material,
            ItemSupplier itemSupplier,
            int tickDelay,
            WeightedList<Supplier<ItemStack>> items,
            List<ItemSupplier> guaranteedItems,
            Range itemCount,
            CollectionCondition collectionCondition,
            Set<Material> placeableOn
    ) {
        this.plugin = plugin;
        this.id = id;
        this.itemId = itemId;
        this.material = material;
        this.itemSupplier = itemSupplier;
        this.tickDelay = tickDelay;
        this.items = items;
        this.guaranteedItems = guaranteedItems;
        this.itemCount = itemCount;
        this.collectionCondition = collectionCondition;
        this.placeableOn = placeableOn;
    }

    public abstract void onGrow(BlockGrowEvent event);

    @Override
    public void onPlace(User user, BlockPlaceEvent event) {
        final Block block = event.getBlock();
        if (!this.placeableOn.contains(block.getRelative(BlockFace.DOWN).getType())) return;
        if (!(this.collectionCondition.isAllowed(user.getCollection()))) {
            user.sendMessage("<red>You do not meet the collection requirements for this crop.");
            return;
        }
        block.setType(this.material);
        final WorldPosition position = WorldPosition.fromLocation(block.getLocation());
        this.plugin.getWorlds().addBlock(this, position);
    }

    protected int calculateTicksPerGrowth(Ageable ageable) {
        final int maxAge = ageable.getMaximumAge();
        return this.tickDelay / maxAge;
    }

}
