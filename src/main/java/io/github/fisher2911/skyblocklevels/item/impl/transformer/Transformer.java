package io.github.fisher2911.skyblocklevels.item.impl.transformer;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
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
import io.github.fisher2911.skyblocklevels.user.CollectionCondition;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.util.DirectionUtil;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import io.github.fisher2911.skyblocklevels.world.Worlds;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Transformer implements SkyBlock, Delayed {

    private static final String TABLE = Transformer.class.getSimpleName().toLowerCase();
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

        dataManager.registerItemSaveConsumer(Transformer.class, (conn, collection) -> {
            collection.forEach(item -> InsertStatement.builder(TABLE).
                    newEntry().
                    addEntry(ID, item.getId()).
                    addEntry(ITEM_ID, item.getItemId()).
                    addEntry(TICK_COUNTER, ((Transformer) item).tickCounter).
                    build().
                    execute(conn));
        });

        dataManager.registerItemLoadFunction(TABLE, (conn, id) -> {
            final SelectStatement.Builder builder = SelectStatement.builder(TABLE).
                    selectAll().
                    condition(ID, String.valueOf(id));
            final List<Transformer> list = builder.build().execute(conn, results -> {
                final String itemId = results.getString(ITEM_ID);
                if (!(plugin.getItemManager().getItem(itemId) instanceof final Transformer item)) return null;
                final int tickCounter = results.getInt(TICK_COUNTER);
                final Transformer transformer = new Transformer(plugin, id, itemId, item.itemSupplier, item.tickDelay, item.collectionRequirements, item.requirements, item.rewards);
                transformer.tickCounter = tickCounter;
                return transformer;
            });
            if (list.isEmpty()) return SpecialSkyItem.EMPTY;
            return list.get(0);
        });

        dataManager.registerItemDeleteConsumer(Transformer.class, (conn, item) -> {
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
    private final int tickDelay;
    private final CollectionCondition collectionRequirements;
    private final Map<ItemSupplier, Integer> requirements;
    private final Map<ItemSupplier, Integer> rewards;
    private Collection<ItemStack> previousBlocks;

    private int tickCounter = 0;

    public Transformer(
            SkyblockLevels plugin,
            long id,
            String itemId,
            ItemSupplier itemSupplier,
            int tickDelay,
            CollectionCondition collectionRequirements,
            Map<ItemSupplier, Integer> requirements,
            Map<ItemSupplier, Integer> rewards
    ) {
        this.plugin = plugin;
        this.id = id;
        this.itemId = itemId;
        this.itemSupplier = itemSupplier;
        this.tickDelay = tickDelay;
        this.collectionRequirements = collectionRequirements;
        this.requirements = requirements;
        this.rewards = rewards;
        this.previousBlocks = new HashSet<>();
    }

    @Override
    public String getTableName() {
        return TABLE;
    }

    @Override
    public void onBreak(User user, BlockBreakEvent event) {
        final Block block = event.getBlock();
        event.setDropItems(false);
        if (block.getState() instanceof Container container) {
            final Location location = block.getLocation();
            final Inventory inventory = container.getSnapshotInventory();
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                for (ItemStack itemStack : inventory) {
                    if (itemStack == null) continue;
                    final ItemStack item = itemStack.clone();
                    location.getWorld().dropItem(location, item);
                }
            }, 1);
        }
        this.plugin.getWorlds().removeBlock(WorldPosition.fromLocation(block.getLocation()));
        this.plugin.getItemManager().giveItem(user, this);
    }

    @Override
    public void onMineBlock(User user, Block block) {

    }

    @Override
    public void onBlockDamage(User user, BlockDamageEvent event) {

    }

    @Override
    public void onPlace(User user, BlockPlaceEvent event) {
        if (!this.collectionRequirements.isAllowed(user.getCollection())) {
            user.sendMessage("<red>You do not have the required collection to place this block.");
            event.setCancelled(true);
            return;
        }
        final Block block = event.getBlock();
        final Location location = block.getLocation();
        final WorldPosition position = WorldPosition.fromLocation(location);
        this.plugin.getWorlds().addBlock(this, position);
        block.setType(Material.TRAPPED_CHEST);
        DirectionUtil.setBlockDirection(block, event.getPlayer());
    }

    @Override
    public void onClick(User user, PlayerInteractEvent event) {
        if (!this.collectionRequirements.isAllowed(user.getCollection())) {
            user.sendMessage("<red>You do not have the required collection to place this block.");
            event.setCancelled(true);
        }
    }

    @Override
    public void tick(WorldPosition worldPosition) {
        if (this.tickCounter++ < this.tickDelay) return;
        final Block block = worldPosition.toLocation().getBlock();
        if (!(block.getState() instanceof Container container)) return;
        final Collection<ItemStack> items = new HashSet<>();
        for (ItemStack itemStack : container.getInventory()) items.add(itemStack);
        if (this.previousBlocks.equals(items)) {
            this.tickCounter = 0;
            return;
        }
        this.previousBlocks = items;
        final Map<Integer, Integer> slotsToRemove = new HashMap<>();
        this.tickCounter = 0;
        for (var entry : this.requirements.entrySet()) {
            final ItemSupplier itemSupplier = entry.getKey();
            final int amount = entry.getValue();
            final ItemStack created = itemSupplier.get();
            boolean found = false;
            for (int i = 0; i < container.getInventory().getSize(); i++) {
                final ItemStack itemStack = container.getInventory().getItem(i);
                if (itemStack == null) continue;
                if (itemStack.getAmount() < amount) continue;
                if (!itemStack.isSimilar(created)) continue;
                slotsToRemove.put(i, amount);
                found = true;
                break;
            }
            if (!found) return;
        }
        if (slotsToRemove.isEmpty()) return;
        for (var entry : slotsToRemove.entrySet()) {
            final int slot = entry.getKey();
            final int amount = entry.getValue();
            final ItemStack itemStack = container.getInventory().getItem(slot);
            final int setAmount = itemStack.getAmount() - amount;
            if (setAmount <= 0) {
                itemStack.setType(Material.AIR);
                container.getInventory().setItem(slot, itemStack);
                continue;
            }
            itemStack.setAmount(setAmount);
            container.getInventory().setItem(slot, itemStack);
        }
        for (var entry : this.rewards.entrySet()) {
            final ItemSupplier itemSupplier = entry.getKey();
            final int amount = entry.getValue();
            final ItemStack created = itemSupplier.get();
            created.setAmount(amount);
            container.getInventory().addItem(created);
        }
    }

    @Override
    public void onDestroy(BlockDestroyEvent event) {
        final Location location = event.getBlock().getLocation();
        final WorldPosition worldPosition = WorldPosition.fromLocation(location);
        this.plugin.getWorlds().removeBlock(worldPosition);
        location.getWorld().dropItem(location, this.itemSupplier.get());
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
    public boolean uniqueInInventory() {
        return false;
    }

    @Override
    public int getTickDelay() {
        return this.tickDelay;
    }

    public static Serializer serializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements TypeSerializer<Supplier<Transformer>> {

        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {
        }

        private static final String ITEM_ID = "item-id";
        private static final String TICK_DELAY = "tick-delay";
        private static final String ITEM = "item";
        private static final String REQUIREMENTS = "requirements";
        private static final String AMOUNT = "amount";
        private static final String REWARDS = "rewards";
        private static final String COLLECTION_REQUIREMENTS = "collection-requirements";


        @Override
        public Supplier<Transformer> deserialize(Type type, ConfigurationNode node) {
            try {
                final String itemId = node.node(ITEM_ID).getString();
                final int tickDelay = node.node(TICK_DELAY).getInt();
                final ItemSupplier itemSupplier = ItemSerializer.deserialize(node.node(ITEM));
                final Map<ItemSupplier, Integer> requirements = new HashMap<>();
                for (ConfigurationNode requirementNode : node.node(REQUIREMENTS).childrenMap().values()) {
                    final ItemSupplier requirementSupplier = ItemSerializer.deserialize(requirementNode);
                    final int amount = requirementNode.node(AMOUNT).getInt(1);
                    requirements.put(requirementSupplier, amount);
                }
                final Map<ItemSupplier, Integer> rewards = new HashMap<>();
                for (ConfigurationNode rewardsNode : node.node(REWARDS).childrenMap().values()) {
                    final ItemSupplier rewardSupplier = ItemSerializer.deserialize(rewardsNode);
                    final int amount = rewardsNode.node(AMOUNT).getInt(1);
                    rewards.put(rewardSupplier, amount);
                }
                final CollectionCondition collectionCondition = CollectionCondition.serializer().deserialize(CollectionCondition.class, node.node(COLLECTION_REQUIREMENTS));
                final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
                return () -> new Transformer(plugin, plugin.getDataManager().generateNextId(), itemId, itemSupplier, tickDelay, collectionCondition, requirements, rewards);
            } catch (SerializationException e) {
                throw new RuntimeException();
            }
        }

        @Override
        public void serialize(Type type, @Nullable Supplier<Transformer> obj, ConfigurationNode node) throws SerializationException {

        }
    }

}
