package io.github.fisher2911.skyblocklevels.item.impl.crop;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.database.CreateTableStatement;
import io.github.fisher2911.skyblocklevels.database.DataManager;
import io.github.fisher2911.skyblocklevels.database.DeleteStatement;
import io.github.fisher2911.skyblocklevels.database.InsertStatement;
import io.github.fisher2911.skyblocklevels.database.KeyType;
import io.github.fisher2911.skyblocklevels.database.SelectStatement;
import io.github.fisher2911.skyblocklevels.database.VarChar;
import io.github.fisher2911.skyblocklevels.item.ItemSerializer;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.item.SkyBlock;
import io.github.fisher2911.skyblocklevels.item.SpecialSkyItem;
import io.github.fisher2911.skyblocklevels.user.CollectionCondition;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.util.Range;
import io.github.fisher2911.skyblocklevels.util.weight.Weight;
import io.github.fisher2911.skyblocklevels.util.weight.WeightedList;
import io.github.fisher2911.skyblocklevels.world.Position;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
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
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MultiSkyCrop extends SkyCrop {

    private static final String TABLE = MultiSkyCrop.class.getSimpleName().toLowerCase();
    private static final String ID = "id";
    private static final String ITEM_ID = "item_id";
    private static final String TICK_COUNTER = "tick_counter";
    private static final String CROP_UNDER_X = "crop_under_x";
    private static final String CROP_UNDER_Y = "crop_under_y";
    private static final String CROP_UNDER_Z = "crop_under_z";
    private static final String BASE_X = "base_x";
    private static final String BASE_Y = "base_y";
    private static final String BASE_Z = "base_z";

    static {
        final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
        final DataManager dataManager = plugin.getDataManager();

        dataManager.addTable(CreateTableStatement.builder(TABLE).
                addField(Long.class, ID, KeyType.PRIMARY).
                addField(VarChar.ITEM_ID, ITEM_ID).
                addField(Integer.class, TICK_COUNTER).
                addField(Integer.class, CROP_UNDER_X).
                addField(Integer.class, CROP_UNDER_Y).
                addField(Integer.class, CROP_UNDER_Z).
                addField(Integer.class, BASE_X).
                addField(Integer.class, BASE_Y).
                addField(Integer.class, BASE_Z).
                build());

        dataManager.registerItemSaveConsumer(MultiSkyCrop.class, (conn, collection) -> {
            collection.forEach(item -> {
                if (!(item instanceof MultiSkyCrop crop)) return;
                InsertStatement.builder(TABLE).
                        newEntry().
                        addEntry(ID, item.getId()).
                        addEntry(ITEM_ID, item.getItemId()).
                        addEntry(TICK_COUNTER, crop.currentTickCounter).
                        addEntry(CROP_UNDER_X, crop.directlyUnder.getX()).
                        addEntry(CROP_UNDER_Y, crop.directlyUnder.getY()).
                        addEntry(CROP_UNDER_Z, crop.directlyUnder.getZ()).
                        addEntry(BASE_X, (int) crop.base.getX()).
                        addEntry(BASE_Y, (int) crop.base.getY()).
                        addEntry(BASE_Z, (int) crop.base.getZ()).
                        build().
                        execute(conn);
            });
        });

        dataManager.registerItemLoadFunction(TABLE, (conn, id) -> {
            final SelectStatement.Builder builder = SelectStatement.builder(TABLE).
                    selectAll().
                    condition(ID, String.valueOf(id));
            final List<MultiSkyCrop> list = builder.build().execute(conn, results -> {
                final String itemId = results.getString(ITEM_ID);
                if (!(plugin.getItemManager().getItem(itemId) instanceof final MultiSkyCrop item)) return null;
                final int tickCounter = results.getInt(TICK_COUNTER);
                final int cropUnderX = results.getInt(CROP_UNDER_X);
                final int cropUnderY = results.getInt(CROP_UNDER_Y);
                final int cropUnderZ = results.getInt(CROP_UNDER_Z);
                final int baseX = results.getInt(BASE_X);
                final int baseY = results.getInt(BASE_Y);
                final int baseZ = results.getInt(BASE_Z);
                final Position cropUnder = new Position(cropUnderX, cropUnderY, cropUnderZ);
                final Position base = new Position(baseX, baseY, baseZ);
                final MultiSkyCrop skyCrop = new MultiSkyCrop(
                        plugin,
                        id,
                        itemId,
                        item.material,
                        item.itemSupplier,
                        item.tickDelay,
                        item.items,
                        item.bonusItems,
                        item.guaranteedFullyGrownItems,
                        item.guaranteedItems,
                        item.itemCount,
                        item.collectionCondition,
                        item.placeableOn,
                        item.maxHeight,
                        base,
                        cropUnder
                );
                skyCrop.currentTickCounter = tickCounter;
                return skyCrop;
            });
            if (list.isEmpty()) return SpecialSkyItem.EMPTY;
            return list.get(0);
        });

        dataManager.registerItemDeleteConsumer(MultiSkyCrop.class, (conn, item) -> {
            DeleteStatement.builder(TABLE).
                    condition(ID, String.valueOf(item.getId())).
                    build().
                    execute(conn);
        });
    }

    private final int maxHeight;
    private Position directlyUnder;
    private Position base;
    private int currentTickCounter;
    private boolean removed;

    public MultiSkyCrop(
            SkyblockLevels plugin,
            long id,
            String itemId,
            Material material,
            ItemSupplier itemSupplier,
            int tickDelay,
            WeightedList<Supplier<ItemStack>> items,
            WeightedList<Supplier<ItemStack>> bonusItems,
            List<ItemSupplier> guaranteedFullyGrownItems,
            List<ItemSupplier> guaranteedItems,
            Range itemCount,
            CollectionCondition collectionCondition,
            Set<Material> placeableOn,
            int maxHeight
    ) {
        super(plugin, id, itemId, material, itemSupplier, tickDelay, items, bonusItems, guaranteedFullyGrownItems, guaranteedItems, itemCount, collectionCondition, placeableOn);
        this.maxHeight = maxHeight;
    }

    public MultiSkyCrop(
            SkyblockLevels plugin,
            long id,
            String itemId,
            Material material,
            ItemSupplier itemSupplier,
            int tickDelay,
            WeightedList<Supplier<ItemStack>> items,
            WeightedList<Supplier<ItemStack>> bonusItems,
            List<ItemSupplier> guaranteedFullyGrownItems,
            List<ItemSupplier> guaranteedItems,
            Range itemCount,
            CollectionCondition collectionCondition,
            Set<Material> placeableOn,
            int maxHeight,
            Position base,
            Position directlyUnder
    ) {
        this(plugin, id, itemId, material, itemSupplier, tickDelay, items, bonusItems, guaranteedFullyGrownItems, guaranteedItems, itemCount, collectionCondition, placeableOn, maxHeight);
        this.base = base;
        this.directlyUnder = directlyUnder;
    }

    public boolean isGrown() {
        return this.currentTickCounter >= this.tickDelay;
    }

    @Override
    public void onGrow(BlockGrowEvent event) {
        event.setCancelled(true);
    }

    public void onGrow(BlockSpreadEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onPlace(User user, BlockPlaceEvent event) {
        final Block block = event.getBlock();
        if (!this.placeableOn.contains(block.getRelative(BlockFace.DOWN).getType())) {
            event.setCancelled(true);
            return;
        }
        if (!(this.collectionCondition.isAllowed(user.getCollection()))) {
            user.sendMessage("<red>You do not meet the collection requirements for this crop.");
            event.setCancelled(true);
            return;
        }
        block.setType(this.material);
        final WorldPosition position = WorldPosition.fromLocation(block.getLocation());
        this.plugin.getWorlds().addBlock(this, position);
        this.base = position.getPosition();
        this.directlyUnder = this.base;
        final WorldPosition above = position.getRelative(BlockFace.UP);
        final MultiSkyCrop copy = this.copy(position.getPosition());
        this.plugin.getWorlds().addBlock(copy, above);
    }

    @Override
    public void onBreak(User user, BlockBreakEvent event) {
        final Block block = event.getBlock();
        final Location location = block.getLocation();
        if (!(this.collectionCondition.isAllowed(user.getCollection()))) {
            event.setCancelled(true);
            return;
        }
        final WorldPosition below = WorldPosition.fromLocation(location.getBlock().getRelative(BlockFace.DOWN).getLocation());
        final SkyBlock belowBlock = this.plugin.getWorlds().getBlockAt(below);
        if (belowBlock instanceof final MultiSkyCrop multiSkyCrop) {
            multiSkyCrop.currentTickCounter = 0;
        }
        WorldPosition above = WorldPosition.fromLocation(location);
        event.setCancelled(true);
        if (this.base.equals(this.directlyUnder)) this.plugin.getWorlds().removeBlock(above);
        event.setDropItems(false);
        this.dropItems(location);
        this.removed = true;
        this.plugin.getUserManager().addCollectionAmount(user, this.itemId, this.breakBlocksAbove(above) + 1);
    }

    private int breakBlocksAbove(WorldPosition above) {
        SkyBlock crop;
        above = above.getRelative(BlockFace.UP);
        int totalBroken = 0;
        while ((crop = this.plugin.getWorlds().getBlockAt(above)) != SkyBlock.EMPTY) {
            if (!(crop instanceof MultiSkyCrop multiSkyCrop)) break;
            if (!multiSkyCrop.itemId.equals(this.itemId)) break;
            this.plugin.getWorlds().removeBlock(above);
            multiSkyCrop.removed = true;
            totalBroken++;
            if (!multiSkyCrop.isGrown() && above.toLocation().getBlock().getType() != this.material) break;
            multiSkyCrop.dropItems(above.toLocation());
            above = above.getRelative(BlockFace.UP);
        }
        return totalBroken;
    }

    @Override
    public void onDestroy(BlockDestroyEvent event) {
//        event.setCancelled(true);
//        final Block block = event.getBlock();
//        final Location location = block.getLocation();
//        this.dropItems(location);
//        block.setBlockData(event.getNewState(), true);
//        this.breakBlocksAbove(WorldPosition.fromLocation(location));
//        if (this.base.equals(this.directlyUnder)) this.plugin.getWorlds().removeBlock(WorldPosition.fromLocation(location));
//        final SkyBlock below = this.plugin.getWorlds().getBlockAt(this.directlyUnder.toWorldPosition(block.getWorld()));
//        if (below instanceof MultiSkyCrop multiSkyCrop) {
//            multiSkyCrop.currentTickCounter = 0;
//        }
        final Block block = event.getBlock();
        final Location location = block.getLocation();
        final WorldPosition below = WorldPosition.fromLocation(location.getBlock().getRelative(BlockFace.DOWN).getLocation());
        final SkyBlock belowBlock = this.plugin.getWorlds().getBlockAt(below);
        if (belowBlock instanceof final MultiSkyCrop multiSkyCrop) {
            multiSkyCrop.currentTickCounter = 0;
        }
        WorldPosition above = WorldPosition.fromLocation(location);
        event.setCancelled(true);
        if (this.base.equals(this.directlyUnder)) this.plugin.getWorlds().removeBlock(above);
        event.setCancelled(true);
        this.dropItems(location);
        block.setBlockData(event.getNewState(), true);
        this.removed = true;
        this.breakBlocksAbove(above);
    }

    private void dropItems(Location location) {
        int i = this.itemCount.getRandom();
        if (location.getBlock().getType() != this.material) return;
        location.getBlock().setType(Material.AIR, false);
        while (i > 0) {
            final Supplier<ItemStack> itemStackSupplier = this.items.getRandom();
            if (itemStackSupplier == null) break;
            final ItemStack itemStack = itemStackSupplier.get();
            if (itemStack == null || itemStack.getType() == Material.AIR || itemStack.getAmount() == 0) continue;
            location.getWorld().dropItem(location, itemStack);
            i--;
        }
        for (ItemSupplier guaranteed : this.guaranteedItems) {
            final ItemStack itemStack = guaranteed.get();
            if (itemStack == null || itemStack.getType() == Material.AIR || itemStack.getAmount() == 0) continue;
            location.getWorld().dropItem(location, itemStack);
        }
        final Supplier<ItemStack> bonus = this.bonusItems.getRandom();
        if (bonus == null) return;
        final ItemStack itemStack = bonus.get();
        if (itemStack == null || itemStack.getType() == Material.AIR || itemStack.getAmount() == 0) return;
        location.getWorld().dropItem(location, itemStack);
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

    private final Predicate<WorldPosition> surroundingCanPlacePredicate = position -> {
        final Block block = position.toLocation().getBlock();
        if (this.material == Material.CACTUS) {
            return !block.getType().isSolid();
        }
        if (this.material == Material.SUGAR_CANE && position.getPosition().equals(this.base)) {
            return block.getRelative(BlockFace.DOWN).getType() == Material.WATER;
        }
        return true;
    };

    // todo fix cactus growing twice bug
    @Override
    public void tick(WorldPosition worldPosition) {
        if (this.removed) return;
        if (worldPosition.getPosition().distanceSquared(this.base) > Math.pow(this.maxHeight - 1, 2)) return;
        final SkyBlock blockUnder = this.plugin.getWorlds().getBlockAt(this.directlyUnder.toWorldPosition(worldPosition.getWorld()));
        if (blockUnder == SkyBlock.EMPTY) return;
        if (!(blockUnder instanceof MultiSkyCrop multiSkyCrop)) return;
        if (!multiSkyCrop.isGrown() && !this.directlyUnder.blocksEqual(worldPosition.getPosition())) return;
        final WorldPosition above = worldPosition.getRelative(BlockFace.UP);
        SpecialSkyItem cropAbove = this.plugin.getWorlds().getBlockAt(above);
        if (!(cropAbove instanceof MultiSkyCrop)) {
            cropAbove = this.copy(worldPosition.getPosition());
            this.plugin.getWorlds().addBlock((MultiSkyCrop) cropAbove, above);
        }
        if (!this.isGrown()) {
            this.currentTickCounter++;
            return;
        }
        if (above.getPosition().distanceSquared(this.base) < Math.pow(this.maxHeight, 2)) {
            if (!checkSurroundingCanPlace(above, this.surroundingCanPlacePredicate)) {
                final Location aboveLocation = above.toLocation();
                aboveLocation.getBlock().setType(this.material, false);
                this.dropItems(above.toLocation());
                this.currentTickCounter = 0;
                this.breakBlocksAbove(above);
                return;
            }
            above.toLocation().getBlock().setType(this.material, true);
        }
    }

    private static final BlockFace[] FACES = new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    private boolean checkSurroundingCanPlace(WorldPosition position, Predicate<WorldPosition> predicate) {
        for (BlockFace face : FACES) {
            final WorldPosition relative = position.getRelative(face);
            if (predicate.test(relative)) continue;
            return false;
        }
        return true;
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

    private MultiSkyCrop copy(Position blockUnder) {
        return new MultiSkyCrop(
                this.plugin,
                this.plugin.getDataManager().generateNextId(),
                this.itemId,
                this.material,
                this.itemSupplier,
                this.tickDelay,
                this.items,
                this.bonusItems,
                this.guaranteedFullyGrownItems,
                this.guaranteedItems,
                this.itemCount,
                this.collectionCondition,
                this.placeableOn,
                this.maxHeight,
                this.base,
                blockUnder
        );
    }

    public static Serializer serializer() {
        return Serializer.INSTANCE;
    }


    public static class Serializer implements TypeSerializer<Supplier<MultiSkyCrop>> {

        private static final String ITEM_ID = "item-id";
        private static final String ITEM = "item";
        private static final String MATERIAL = "material";
        private static final String TICK_DELAY = "tick-delay";
        private static final String ITEMS = "items";
        private static final String BONUS_ITEMS = "bonus-items";
        private static final String GUARANTEED_FULLY_GROWN_ITEMS = "guaranteed-fully-grown-items";
        private static final String GUARANTEED_ITEMS = "guaranteed-items";
        private static final String ITEM_COUNT = "item-count";
        private static final String COLLECTION_REQUIREMENTS = "collection-requirements";
        private static final String PLACEABLE_ON = "placeable-on";
        private static final String MAX_HEIGHT = "max-height";

        private static final Serializer INSTANCE = new Serializer();

        private Serializer() {
        }

        @Override
        public Supplier<MultiSkyCrop> deserialize(Type type, ConfigurationNode node) {
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
                final ConfigurationNode guaranteedFullyGrownItemsNode = node.node(GUARANTEED_FULLY_GROWN_ITEMS);
                final List<ItemSupplier> guaranteedFullyGrownItems = guaranteedFullyGrownItemsNode.childrenMap().values().stream().
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
                final int maxHeight = node.node(MAX_HEIGHT).getInt(3);
                return () -> new MultiSkyCrop(
                        plugin,
                        plugin.getDataManager().generateNextId(),
                        itemId,
                        material,
                        itemSupplier,
                        tickDelay,
                        items,
                        bonusItems,
                        guaranteedItems,
                        guaranteedFullyGrownItems,
                        itemCount,
                        requirements,
                        placeableOn,
                        maxHeight
                );
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void serialize(Type type, @Nullable Supplier<MultiSkyCrop> obj, ConfigurationNode node) throws SerializationException {

        }
    }

}
