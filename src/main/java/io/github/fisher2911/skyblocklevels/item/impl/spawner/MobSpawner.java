package io.github.fisher2911.skyblocklevels.item.impl.spawner;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.Delayed;
import io.github.fisher2911.skyblocklevels.item.ItemSerializer;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.item.Spawner;
import io.github.fisher2911.skyblocklevels.user.BukkitUser;
import io.github.fisher2911.skyblocklevels.user.CollectionCondition;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.util.Range;
import io.github.fisher2911.skyblocklevels.util.weight.WeightedList;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.Supplier;

public class MobSpawner implements Spawner, Delayed {

    private static final String TABLE = MobSpawner.class.getSimpleName().toLowerCase();

    private final SkyblockLevels plugin;
    private final long id;
    private final String itemId;
    private final EntityType entityType;
    private final String name;
    private final ItemSupplier itemSupplier;
    private final Range tickDelay;
    private final WeightedList<String> entityTypes;
    private final CollectionCondition collectionCondition;

    public MobSpawner(SkyblockLevels plugin, long id, String itemId, EntityType entityType, String name, ItemSupplier itemSupplier, Range tickDelay, WeightedList<String> entityTypes, CollectionCondition collectionCondition) {
        this.plugin = plugin;
        this.id = id;
        this.itemId = itemId;
        this.entityType = entityType;
        this.name = name;
        this.itemSupplier = itemSupplier;
        this.tickDelay = tickDelay;
        this.entityTypes = entityTypes;
        this.collectionCondition = collectionCondition;
    }

    @Override
    public void onBreak(User user, BlockBreakEvent event) {
        if (!(user instanceof final BukkitUser bukkitUser)) return;
        event.setExpToDrop(0);
        final WorldPosition position = WorldPosition.fromLocation(event.getBlock().getLocation());
        this.plugin.getWorlds().removeBlock(position);
        this.plugin.getItemManager().giveItem(bukkitUser, this);
    }

    @Override
    public void onDestroy(BlockDestroyEvent event) {
        event.setCancelled(true);
        final Block block = event.getBlock();
        block.setBlockData(event.getNewState(), true);
        block.getWorld().dropItem(block.getLocation(), this.plugin.getItemManager().getItem(this));
        this.plugin.getWorlds().removeBlock(WorldPosition.fromLocation(block.getLocation()));
    }

    @Override
    public void onMineBlock(User user, Block block) {

    }

    @Override
    public void onBlockDamage(User user, BlockDamageEvent event) {

    }

    @Override
    public void onPlace(User user, BlockPlaceEvent event) {
        if (!this.collectionCondition.isAllowed(user.getCollection())) {
            user.sendMessage("<red>You do not meet the requirements to place this spawner.");
            event.setCancelled(true);
            return;
        }
        final Block block = event.getBlock();
        final WorldPosition worldPosition = WorldPosition.fromLocation(block.getLocation());
        event.getItemInHand().setAmount(event.getItemInHand().getAmount() - 1);
        block.setType(Material.SPAWNER);
        if (block.getState() instanceof final CreatureSpawner spawner) {
            spawner.setSpawnedType(this.entityType);
            spawner.setMinSpawnDelay(this.tickDelay.getMin());
            spawner.setMaxSpawnDelay(this.tickDelay.getMax());
            spawner.setSpawnCount(2);
            spawner.setSpawnCount(1);
            spawner.update(true, false);
        }
        this.plugin.getWorlds().addBlock(this, worldPosition);
        user.sendMessage("<green>You placed a " + this.name + " mob spawner!");
    }

    @Override
    public void onClick(User user, PlayerInteractEvent event) {

    }

    @Override
    public void tick(WorldPosition worldPosition) {

    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public void onSpawn(Entity entity) {
        final String type = this.entityTypes.getRandom();
        if (type == null) return;
        this.plugin.getEntityManager().create(type, entity);
    }

    @Override
    public void onDamage(EntityDamageEvent e) {
        if (!(e instanceof final EntityDamageByEntityEvent event)) return;
        if (!(event.getDamager() instanceof final Player player)) return;
        final User user = this.plugin.getUserManager().getUser(player);
        if (user == null) return;
        if (!this.collectionCondition.isAllowed(user.getCollection())) {
            user.sendMessage("<red>You do not meet the requirements to attack this entity.");
            event.setCancelled(true);
        }
    }

    @Override
    public int getTickDelay() {
        return this.tickDelay.getMax();
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
    public String getTableName() {
        return TABLE;
    }

    public static Serializer serializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements TypeSerializer<Supplier<MobSpawner>> {

        private static final Serializer INSTANCE = new Serializer();

        private Serializer() {}

        private static final String ITEM_ID = "item-id";
        private static final String ENTITY_TYPE = "entity-type";
        private static final String NAME = "name";
        private static final String ITEM = "item";
        private static final String TICK_DELAY = "tick-delay";
        private static final String ENTITIES = "entities";
        private static final String COLLECTION_REQUIREMENTS = "collection-requirements";

        @Override
        public Supplier<MobSpawner> deserialize(Type type, ConfigurationNode node) {
            try {
                final String itemId = node.node(ITEM_ID).getString();
                final EntityType entityType = EntityType.valueOf(node.node(ENTITY_TYPE).getString(""));
                final String name = node.node(NAME).getString("");
                final ItemSupplier itemSupplier = ItemSerializer.deserialize(node.node(ITEM));
                final Range tickDelay = Objects.requireNonNullElse(Range.serializer().deserialize(Range.class, node.node(TICK_DELAY)), new Range(10 * 20, 40 * 20));
                final TypeSerializer<WeightedList<String>> serializer = WeightedList.serializer(String.class, null);
                final WeightedList<String> entities = serializer.deserialize(WeightedList.class, node.node(ENTITIES));
                final CollectionCondition requirements = CollectionCondition.serializer().deserialize(CollectionCondition.class, node.node(COLLECTION_REQUIREMENTS));
                final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
                return () -> new MobSpawner(
                        plugin,
                        -1,
                        itemId,
                        entityType,
                        name,
                        itemSupplier,
                        tickDelay,
                        entities,
                        requirements
                );
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void serialize(Type type, @Nullable Supplier<MobSpawner> obj, ConfigurationNode node) throws SerializationException {

        }
    }
}
