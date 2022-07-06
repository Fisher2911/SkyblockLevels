package io.github.fisher2911.skyblocklevels.item.impl.crop;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.ItemSerializer;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.item.SkyBlock;
import io.github.fisher2911.skyblocklevels.user.CollectionCondition;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.util.Range;
import io.github.fisher2911.skyblocklevels.util.weight.Weight;
import io.github.fisher2911.skyblocklevels.util.weight.WeightedList;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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

public class SkyCrop implements SkyBlock {

    private static final String TABLE = "sky_crop";

    private final SkyblockLevels plugin;
    private final long id;
    private final String itemId;
    private final Material material;
    private final ItemSupplier itemSupplier;
    private final int tickDelay;
    private final WeightedList<Supplier<ItemStack>> items;
    private final List<ItemSupplier> guaranteedItems;
    private final Range itemCount;
    private final CollectionCondition collectionCondition;
    private final Set<Material> placeableOn;

    private int currentTickCounter;

    public SkyCrop(SkyblockLevels plugin, long id, String itemId, Material material, ItemSupplier itemSupplier, int tickDelay, WeightedList<Supplier<ItemStack>> items, Range itemCount, List<ItemSupplier> guaranteedItems, CollectionCondition collectionCondition, Set<Material> placeableOn) {
        this.plugin = plugin;
        this.id = id;
        this.itemId = itemId;
        this.material = material;
        this.itemSupplier = itemSupplier;
        this.tickDelay = tickDelay;
        this.items = items;
        this.itemCount = itemCount;
        this.guaranteedItems = guaranteedItems;
        this.collectionCondition = collectionCondition;
        this.placeableOn = placeableOn;
    }

    @Override
    public void onBreak(User user, BlockBreakEvent event) {
        final Block block = event.getBlock();
        final Location location = block.getLocation();
        final WorldPosition position = WorldPosition.fromLocation(location);
        if (!this.isMelonOrPumpkin(block)) this.plugin.getWorlds().removeBlock(position);
        block.setType(this.fromBlock(block));
        if (!(this.collectionCondition.isAllowed(user.getCollection()))) return;
        int i = this.itemCount.getRandom();
        while (i > 0) {
            final Supplier<ItemStack> itemStackSupplier = this.items.getRandom();
            if (itemStackSupplier == null) return;
            final ItemStack itemStack = itemStackSupplier.get();
            if (itemStack == null || itemStack.getType() == Material.AIR) return;
            location.getWorld().dropItem(location, itemStack);
            i--;
        }
        for (ItemSupplier guaranteed : this.guaranteedItems) {
            final ItemStack itemStack = guaranteed.get();
            if (itemStack == null || itemStack.getType() == Material.AIR) continue;
            location.getWorld().dropItem(location, itemStack);
        }
    }

    @Override
    public void onMineBlock(User user, Block block) {

    }

    @Override
    public void onBlockDamage(User user, BlockDamageEvent event) {

    }

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

    @Override
    public void onClick(User user, PlayerInteractEvent event) {
        event.getPlayer().sendMessage("Clicked sky crop");
    }

    @Override
    public void tick(WorldPosition worldPosition) {
        final Block block = worldPosition.toLocation().getBlock();
        if (!(block.getBlockData() instanceof Ageable ageable)) return;
        final int age = ageable.getAge();
        if (this.currentTickCounter++ < this.calculateTicksPerGrowth(ageable)) return;
        if (age == ageable.getMaximumAge()) {
            if (this.isMelonOrPumpkin(block)) block.setType(this.fromSeeds(block.getType()));
            return;
        }
        ageable.setAge(age + 1);
        block.setBlockData(ageable);
        this.currentTickCounter = 0;
    }

    private boolean isMelonOrPumpkin(Block block) {
        return block.getType() == Material.MELON_SEEDS || block.getType() == Material.PUMPKIN_SEEDS;
    }

    private Material fromSeeds(Material material) {
        return switch (material) {
            case MELON_SEEDS -> Material.MELON;
            case PUMPKIN_SEEDS -> Material.PUMPKIN;
            default -> material;
        };
    }

    private Material fromBlock(Block block) {
        return switch (block.getType()) {
            case MELON -> Material.MELON_SEEDS;
            case PUMPKIN -> Material.PUMPKIN_SEEDS;
            default -> Material.AIR;
        };
    }

    private int calculateTicksPerGrowth(Ageable ageable) {
        final int maxAge = ageable.getMaximumAge();
        return this.tickDelay / maxAge;
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

    public static class Serializer implements TypeSerializer<Supplier<SkyCrop>> {

        private static final String ITEM_ID = "item-id";
        private static final String ITEM = "item";
        private static final String MATERIAL = "material";
        private static final String TICK_DELAY = "tick-delay";
        private static final String ITEMS = "items";
        private static final String GUARANTEED_ITEMS = "guaranteed-items";
        private static final String ITEM_COUNT = "item-count";
        private static final String COLLECTION_REQUIREMENTS = "collection-requirements";
        private static final String PLACEABLE_ON = "placeable-on";

        private static final Serializer INSTANCE = new Serializer();

        private Serializer() {
        }

        @Override
        public Supplier<SkyCrop> deserialize(Type type, ConfigurationNode node) {
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
                final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
                final CollectionCondition requirements = CollectionCondition.serializer().deserialize(CollectionCondition.class, node.node(COLLECTION_REQUIREMENTS));
                final Set<Material> placeableOn = node.node(PLACEABLE_ON).getList(String.class, new ArrayList<>()).
                        stream().
                        map(Material::matchMaterial).
                        collect(Collectors.toSet());
                return () -> new SkyCrop(
                        plugin,
                        plugin.getItemManager().generateNextId(),
                        itemId,
                        material,
                        itemSupplier,
                        tickDelay,
                        items,
                        itemCount,
                        guaranteedItems,
                        requirements,
                        placeableOn
                );
            } catch (
                    SerializationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void serialize(Type type, @Nullable Supplier<SkyCrop> obj, ConfigurationNode node) throws SerializationException {

        }
    }

}
