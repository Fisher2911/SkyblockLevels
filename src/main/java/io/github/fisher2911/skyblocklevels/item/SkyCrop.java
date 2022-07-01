package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.user.CollectionCondition;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.util.weight.Weight;
import io.github.fisher2911.skyblocklevels.util.weight.WeightedList;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
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
    private final CollectionCondition collectionCondition;

    private int currentTickCounter;

    public SkyCrop(SkyblockLevels plugin, long id, String itemId, Material material, ItemSupplier itemSupplier, int tickDelay, WeightedList<Supplier<ItemStack>> items, CollectionCondition collectionCondition) {
        this.plugin = plugin;
        this.id = id;
        this.itemId = itemId;
        this.material = material;
        this.itemSupplier = itemSupplier;
        this.tickDelay = tickDelay;
        this.items = items;
        this.collectionCondition = collectionCondition;
    }

    @Override
    public void onBreak(User user, BlockBreakEvent event) {
        final Block block = event.getBlock();
        final Location location = block.getLocation();
        final WorldPosition position = WorldPosition.fromLocation(location);
        this.plugin.getWorlds().removeBlock(position);
        block.setType(Material.AIR);
        if (!(this.collectionCondition.isAllowed(user.getCollection()))) return;
        final Supplier<ItemStack> itemStackSupplier = this.items.getRandom();
        if (itemStackSupplier == null) return;
        final ItemStack itemStack = itemStackSupplier.get();
        if (itemStack == null) return;
        location.getWorld().dropItem(location, itemStack);
    }

    @Override
    public void onMineBlock(User user, Block block) {

    }

    @Override
    public void onBlockDamage(User user, BlockDamageEvent event) {

    }

    @Override
    public void onPlace(User user, BlockPlaceEvent event) {
        if (!(this.collectionCondition.isAllowed(user.getCollection()))) {
            user.sendMessage("<red>You do not meet the collection requirements for this crop.");
            return;
        }
        final Block block = event.getBlock();
        block.setType(this.material);
        final WorldPosition position = WorldPosition.fromLocation(block.getLocation());
        this.plugin.getWorlds().addBlock(this, position);
    }

    @Override
    public void onClick(User user, PlayerInteractEvent event) {

    }

    @Override
    public void tick(WorldPosition worldPosition) {
        final Block block = worldPosition.toLocation().getBlock();
        if (!(block.getBlockData() instanceof Ageable ageable)) return;
        final int age = ageable.getAge();
        if (age == ageable.getMaximumAge()) return;
        if (this.currentTickCounter++ < this.calculateTicksPerGrowth(ageable)) return;
        ageable.setAge(age + 1);
        block.setBlockData(ageable);
        this.currentTickCounter = 0;
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

        private static final String ITEM_ID = "item_id";
        private static final String ITEM = "item";
        private static final String MATERIAL = "material";
        private static final String TICK_DELAY = "tick-delay";
        private static final String ITEMS = "items";
        private static final String COLLECTION_REQUIREMENTS = "collection-requirements";

        private static final Serializer INSTANCE = new Serializer();

        private Serializer() {}

        @Override
        public Supplier<SkyCrop> deserialize(Type type, ConfigurationNode node) {
            try {
                final String itemId = node.node(ITEM_ID).getString();
                final ItemSupplier itemSupplier = ItemSerializer.deserialize(node.node(ITEM));
                final int tickDelay = node.node(TICK_DELAY).getInt();
                final Material material = Material.valueOf(node.node(MATERIAL).getString());
                final TypeSerializer<WeightedList<ItemSupplier>> serializer = WeightedList.serializer(ItemSupplier.class, ItemSerializer.INSTANCE);
                final WeightedList<Supplier<ItemStack>> items =
                        new WeightedList<>(
                                serializer.deserialize(WeightedList.class, node.node(ITEMS)).
                                        getWeightList().
                                        stream().
                                        map(w -> new Weight<>((Supplier<ItemStack>) () -> w.getValue().get(), w.getWeight())).
                                        collect(Collectors.toList()));
                final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
                final CollectionCondition requirements = CollectionCondition.serializer().deserialize(CollectionCondition.class, node.node(COLLECTION_REQUIREMENTS));
                return () -> new SkyCrop(
                        plugin,
                        plugin.getItemManager().generateNextId(),
                        itemId,
                        material,
                        itemSupplier,
                        tickDelay,
                        items,
                        requirements
                );
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        }
        @Override
        public void serialize(Type type, @Nullable Supplier<SkyCrop> obj, ConfigurationNode node) throws SerializationException {

        }
    }

}
