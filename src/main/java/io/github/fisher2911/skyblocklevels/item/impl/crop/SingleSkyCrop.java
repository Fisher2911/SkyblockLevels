package io.github.fisher2911.skyblocklevels.item.impl.crop;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.ItemSerializer;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.user.CollectionCondition;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.util.Range;
import io.github.fisher2911.skyblocklevels.util.weight.Weight;
import io.github.fisher2911.skyblocklevels.util.weight.WeightedList;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SingleSkyCrop extends SkyCrop {

    private static final String TABLE = SingleSkyCrop.class.getSimpleName().toLowerCase();

    private int currentTickCounter;

    public SingleSkyCrop(
            SkyblockLevels plugin,
            long id,
            String itemId,
            Material material,
            ItemSupplier itemSupplier,
            int tickDelay,
            WeightedList<Supplier<ItemStack>> items,
            WeightedList<Supplier<ItemStack>> bonusItems,
            List<ItemSupplier> guaranteedItems,
            Range itemCount,
            CollectionCondition collectionCondition,
            Set<Material> placeableOn
    ) {
        super(plugin, id, itemId, material, itemSupplier, tickDelay, items, bonusItems, guaranteedItems, itemCount, collectionCondition, placeableOn);
    }

    @Override
    public void onGrow(BlockGrowEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onBreak(User user, BlockBreakEvent event) {
        final Block block = event.getBlock();
        final Location location = block.getLocation();
        final WorldPosition position = WorldPosition.fromLocation(location);
        final Player player = event.getPlayer();
        final boolean shifting = player.isSneaking();
        event.setDropItems(false);
        if (!this.isMelonOrPumpkin(block) || shifting) this.plugin.getWorlds().removeBlock(position);
        final Material setType = this.fromBlock(block);
        if (setType != Material.AIR && !shifting) {
            block.getRelative(BlockFace.DOWN).setType(Material.FARMLAND);
        }
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> block.setType(setType), 1L);
        if (!(this.collectionCondition.isAllowed(user.getCollection()))) return;
        this.dropItems(block);
    }

    private void dropItems(Block block) {
        final Location location = block.getLocation();
        for (ItemSupplier guaranteed : this.guaranteedItems) {
            final ItemStack itemStack = guaranteed.get();
            if (itemStack == null || itemStack.getType() == Material.AIR || itemStack.getAmount() == 0) continue;
            location.getWorld().dropItem(location, itemStack);
        }

        if (block.getBlockData() instanceof Ageable ageable && ageable.getAge() < ageable.getMaximumAge()) return;
        int i = this.itemCount.getRandom();
        while (i > 0) {
            final Supplier<ItemStack> itemStackSupplier = this.items.getRandom();
            if (itemStackSupplier == null) continue;
            final ItemStack itemStack = itemStackSupplier.get();
            if (itemStack == null || itemStack.getType() == Material.AIR || itemStack.getAmount() == 0) continue;
            location.getWorld().dropItem(location, itemStack);
            i--;
        }
        final Supplier<ItemStack> bonusItemSupplier = this.bonusItems.getRandom();
        if (bonusItemSupplier == null) return;
        final ItemStack itemStack = bonusItemSupplier.get();
        if (itemStack == null || itemStack.getType() == Material.AIR || itemStack.getAmount() == 0) return;
        location.getWorld().dropItem(location, itemStack);
    }

    @Override
    public void onDestroy(BlockDestroyEvent event) {
        event.setCancelled(true);
        final Block block = event.getBlock();
        this.dropItems(block);
        block.setBlockData(event.getNewState(), true);
        this.plugin.getWorlds().removeBlock(WorldPosition.fromLocation(block.getLocation()));
    }

    @Override
    public void onMineBlock(User user, Block block) {

    }

    @Override
    public void onBlockDamage(User user, BlockDamageEvent event) {

    }

    @Override
    public void onClick(User user, PlayerInteractEvent event) {

    }

    @Override
    public void tick(WorldPosition worldPosition) {
        final Block block = worldPosition.toLocation().getBlock();
        if (!(block.getBlockData() instanceof Ageable ageable)) return;
        final int age = ageable.getAge();
        if (this.currentTickCounter++ < this.tickDelay) return;
        if (age == ageable.getMaximumAge()) {
            if (this.isMelonOrPumpkin(block)) block.setType(this.fromStem(block.getType()));
            return;
        }
        ageable.setAge(age + 1);
        block.setBlockData(ageable);
        this.currentTickCounter = 0;
    }

    private boolean isMelonOrPumpkin(Block block) {
        return block.getType() == Material.MELON ||
                block.getType() == Material.PUMPKIN ||
                block.getType() == Material.PUMPKIN_STEM ||
                block.getType() == Material.MELON_STEM;
    }

    private Material fromStem(Material material) {
        return switch (material) {
            case MELON_STEM -> Material.MELON;
            case PUMPKIN_STEM -> Material.PUMPKIN;
            default -> material;
        };
    }

    private Material fromBlock(Block block) {
        return switch (block.getType()) {
            case MELON -> Material.MELON_STEM;
            case PUMPKIN -> Material.PUMPKIN_STEM;
            default -> Material.AIR;
        };
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public ItemStack getItemStack() {
        return this.itemSupplier.get();
    }

    @Override
    public String getItemId() {
        return this.itemId;
    }

    @Override
    public long getId() {
        return this.id;
    }

    @Override
    public boolean uniqueInInventory() {
        return false;
    }

    @Override
    public String getTableName() {
        return TABLE;
    }

    public static Serializer serializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements TypeSerializer<Supplier<SingleSkyCrop>> {

        private static final String ITEM_ID = "item-id";
        private static final String ITEM = "item";
        private static final String MATERIAL = "material";
        private static final String TICK_DELAY = "tick-delay";
        private static final String ITEMS = "items";
        private static final String BONUS_ITEMS = "bonus-items";
        private static final String GUARANTEED_ITEMS = "guaranteed-items";
        private static final String ITEM_COUNT = "item-count";
        private static final String COLLECTION_REQUIREMENTS = "collection-requirements";
        private static final String PLACEABLE_ON = "placeable-on";

        private static final Serializer INSTANCE = new Serializer();

        private Serializer() {
        }

        @Override
        public Supplier<SingleSkyCrop> deserialize(Type type, ConfigurationNode node) {
            try {
                final String itemId = node.node(ITEM_ID).getString();
                final ItemSupplier itemSupplier = ItemSerializer.deserialize(node.node(ITEM));
                final int tickDelay = node.node(TICK_DELAY).getInt();
                final Material material = Material.valueOf(node.node(MATERIAL).getString());
                final TypeSerializer<WeightedList<ItemSupplier>> serializer = WeightedList.serializer(ItemSupplier.class, ItemSerializer.INSTANCE);
                final ConfigurationNode guaranteedItemsNode = node.node(GUARANTEED_ITEMS);
                final List<ItemSupplier> guaranteedItems = guaranteedItemsNode.childrenMap().values().stream().
                        map(itemNode -> {
                            try {
                                return ItemSerializer.INSTANCE.deserialize(ItemSupplier.class, itemNode);
                            } catch (Exception e) {
                                throw new IllegalArgumentException("Failed to deserialize item supplier", e);
                            }
                        }).
                        collect(Collectors.toList());
                final Range itemCount = Range.serializer().deserialize(Range.class, node.node(ITEM_COUNT));
                final WeightedList<Supplier<ItemStack>> items =
                        new WeightedList<>(
                                serializer.deserialize(WeightedList.class, node.node(ITEMS)).
                                        getWeightList().
                                        stream().
                                        map(w -> new Weight<>((Supplier<ItemStack>) () -> w.getValue().get(), w.getWeight())).
                                        collect(Collectors.toList()));
                final WeightedList<Supplier<ItemStack>> bonusItems =
                        new WeightedList<>(
                                serializer.deserialize(WeightedList.class, node.node(BONUS_ITEMS)).
                                        getWeightList().
                                        stream().
                                        map(w -> new Weight<>((Supplier<ItemStack>) () -> w.getValue().get(), w.getWeight())).
                                        collect(Collectors.toList()));
                final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
                final CollectionCondition requirements = CollectionCondition.serializer().deserialize(CollectionCondition.class, node.node(COLLECTION_REQUIREMENTS));
                final Set<Material> placeableOn = node.node(PLACEABLE_ON).getList(String.class, new ArrayList<>()).
                        stream().
                        map(Material::matchMaterial).
                        collect(Collectors.toSet());
                return () -> new SingleSkyCrop(
                        plugin,
                        plugin.getDataManager().generateNextId(),
                        itemId,
                        material,
                        itemSupplier,
                        tickDelay,
                        items,
                        bonusItems,
                        guaranteedItems,
                        itemCount,
                        requirements,
                        placeableOn
                );
            } catch (
                    SerializationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void serialize(Type type, @Nullable Supplier<SingleSkyCrop> obj, ConfigurationNode node) throws SerializationException {

        }
    }
}
