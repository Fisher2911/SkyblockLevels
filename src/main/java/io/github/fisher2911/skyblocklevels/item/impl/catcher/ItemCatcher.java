package io.github.fisher2911.skyblocklevels.item.impl.catcher;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.database.CreateTableStatement;
import io.github.fisher2911.skyblocklevels.database.DataManager;
import io.github.fisher2911.skyblocklevels.database.DeleteStatement;
import io.github.fisher2911.skyblocklevels.database.InsertStatement;
import io.github.fisher2911.skyblocklevels.database.KeyType;
import io.github.fisher2911.skyblocklevels.database.SelectStatement;
import io.github.fisher2911.skyblocklevels.database.VarChar;
import io.github.fisher2911.skyblocklevels.item.Delayed;
import io.github.fisher2911.skyblocklevels.item.ItemSerializer;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.item.SkyBlock;
import io.github.fisher2911.skyblocklevels.item.SpecialSkyItem;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.util.DirectionUtil;
import io.github.fisher2911.skyblocklevels.util.weight.Weight;
import io.github.fisher2911.skyblocklevels.util.weight.WeightedList;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import io.github.fisher2911.skyblocklevels.world.Worlds;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.Directional;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ItemCatcher implements SkyBlock, Delayed {

    private static final String TABLE = ItemCatcher.class.getSimpleName().toLowerCase();
    private static final String ID = "id";
    private static final String ITEM_ID = "item_id";
    private static final String TICK_COUNTER = "tick_counter";

    static {
        final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
        final DataManager dataManager = plugin.getDataManager();
        dataManager.addTable(CreateTableStatement.builder(TABLE).
                addField(Long.class, ID, KeyType.PRIMARY).
                addField(VarChar.UUID, ITEM_ID).
                addField(Integer.class, TICK_COUNTER).
                foreignKey(ID, Worlds.DATABASE_TABLE_COLUMN, Worlds.DATABASE_BLOCK_ID_COLUMN).
                build());

        dataManager.registerItemSaveConsumer(ItemCatcher.class, (conn, collection) -> {
            final InsertStatement.Builder builder = InsertStatement.builder(TABLE);
            collection.forEach(item -> {
                builder.newEntry().
                        addEntry(ID, item.getId()).
                        addEntry(ITEM_ID, item.getItemId()).
                        addEntry(TICK_COUNTER, ((ItemCatcher) item).tickCounter).
                        build().
                        execute(conn);
            });
            builder.build().execute(conn);
        });
        
        dataManager.registerItemLoadFunction(TABLE, (conn, id) -> {
            final SelectStatement.Builder builder = SelectStatement.builder(TABLE).
                    selectAll().
                    condition(ID, String.valueOf(id));
            final List<ItemCatcher> list = builder.build().execute(conn, results -> {
                final String itemId = results.getString(ITEM_ID);
                if (!(plugin.getItemManager().getItem(itemId) instanceof final ItemCatcher item)) return null;
                final int tickCounter = results.getInt(TICK_COUNTER);
                final ItemCatcher itemCatcher = new ItemCatcher(plugin, id, itemId, item.itemSupplier, item.tickDelay, item.items);
                itemCatcher.tickCounter = tickCounter;
                return itemCatcher;
            });
            if (list.isEmpty()) return SpecialSkyItem.EMPTY;
            return list.get(0);
        });

        dataManager.registerItemDeleteConsumer(ItemCatcher.class, (conn, item) -> {
            DeleteStatement.builder(TABLE).
                    condition(ID, String.valueOf(item.getId())).
                    build().
                    execute(conn);
        });
    }

    private final SkyblockLevels plugin;
    private final long id;
    private final String itemId;
    private final ItemSupplier itemSupplier;
    private int tickDelay;
    private final WeightedList<Supplier<ItemStack>> items;

    private int tickCounter = 0;

    public ItemCatcher(SkyblockLevels plugin, long id, String itemId, ItemSupplier itemSupplier, int tickDelay, WeightedList<Supplier<ItemStack>> items) {
        this.plugin = plugin;
        this.id = id;
        this.itemId = itemId;
        this.itemSupplier = itemSupplier;;
        this.tickDelay = tickDelay;
        this.items = items;
    }

    public ItemCatcher(SkyblockLevels plugin, long id, String itemId, ItemSupplier itemSupplier, int tickDelay, List<Weight<ItemStack>> items) {
        this.plugin = plugin;
        this.id = id;
        this.itemId = itemId;
        this.itemSupplier = itemSupplier;;
        this.tickDelay = tickDelay;
        final List<Weight<Supplier<ItemStack>>> weights = new ArrayList<>();
        for (var weight : items) {
            weights.add(new Weight<>(weight::getValue, weight.getWeight()));
        }
        this.items = new WeightedList<>(weights);
    }

    @Override
    public void onBreak(User user, BlockBreakEvent event) {
        this.plugin.getWorlds().removeBlock(WorldPosition.fromLocation(event.getBlock().getLocation()));
        this.plugin.getItemManager().giveItem(user, this);
    }

    @Override
    public void onBlockDamage(User user, BlockDamageEvent event) {

    }

    @Override
    public void onPlace(User user, BlockPlaceEvent event) {
        final Block block = event.getBlock();
        this.plugin.getWorlds().addBlock(this, WorldPosition.fromLocation(block.getLocation()));
        block.setType(Material.DROPPER);
        final Directional directional = (Directional) block.getBlockData();
        directional.setFacing(DirectionUtil.getPlayerFace(event.getPlayer()).getOppositeFace());
        block.setBlockData(directional);
        event.getItemInHand().setAmount(event.getItemInHand().getAmount() - 1);
    }

    @Override
    public void onClick(User user, PlayerInteractEvent event) {

    }

    @Override
    public void tick(WorldPosition worldPosition) {
        if (this.tickCounter++ < this.tickDelay) return;
        this.tickCounter = 0;
        final var supplier = this.items.getRandom();
        if (supplier == null) return;
        final Block block = worldPosition.toLocation().getBlock();
        if (!(block.getState() instanceof final Container container)) return;
        container.getInventory().addItem(supplier.get());
    }

    @Override
    public void onMineBlock(User user, Block block) {
        user.sendMessage("Mining");
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public ItemStack getItemStack() {
        return this.itemSupplier.get(PLACEHOLDERS, this);
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
    public int getTickDelay() {
        return this.tickDelay;
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

    public static final class Serializer implements TypeSerializer<Supplier<ItemCatcher>> {

        private static final Serializer INSTANCE = new Serializer();

        private Serializer() {
        }

        private static final String ITEM_ID = "item-id";
        private static final String ITEM = "item";
        private static final String TICK_DELAY = "tick-delay";
        private static final String ITEMS = "items";

        @Override
        public Supplier<ItemCatcher> deserialize(Type type, ConfigurationNode node) {
            try {
                final String itemId = node.node(ITEM_ID).getString();
                final ItemSupplier itemSupplier = ItemSerializer.deserialize(node.node(ITEM));
                final int tickDelay = node.node(TICK_DELAY).getInt();
                final TypeSerializer<WeightedList<ItemSupplier>> serializer = WeightedList.serializer(ItemSupplier.class, ItemSerializer.INSTANCE);
                final WeightedList<Supplier<ItemStack>> items =
                        new WeightedList<>(
                                serializer.deserialize(WeightedList.class, node.node(ITEMS)).
                                        getWeightList().
                                        stream().
                                        map(w -> new Weight<>((Supplier<ItemStack>) () -> w.getValue().get(), w.getWeight())).
                                        collect(Collectors.toList()));

                final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
                return () -> new ItemCatcher(plugin, plugin.getDataManager().generateNextId(), itemId, itemSupplier, tickDelay, items);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void serialize(Type type, @Nullable Supplier<ItemCatcher> obj, ConfigurationNode node) throws SerializationException {

        }
    }
}
