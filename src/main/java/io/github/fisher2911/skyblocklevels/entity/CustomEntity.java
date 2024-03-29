package io.github.fisher2911.skyblocklevels.entity;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.database.statement.CreateTableStatement;
import io.github.fisher2911.skyblocklevels.database.DataManager;
import io.github.fisher2911.skyblocklevels.database.statement.DeleteStatement;
import io.github.fisher2911.skyblocklevels.database.statement.KeyType;
import io.github.fisher2911.skyblocklevels.database.statement.VarChar;
import io.github.fisher2911.skyblocklevels.item.ItemSerializer;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.item.impl.bridger.LimitedBridger;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.util.Range;
import io.github.fisher2911.skyblocklevels.util.weight.WeightedList;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.function.Function;

public class CustomEntity implements SkyEntity {

    private static final String TABLE = "custom_entity";
    private static final String ID = "id";
    private static final String ENTITY_TYPE = "entity_type";
    private static final String UUID = "uuid";

    static {
        final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
        final DataManager dataManager = plugin.getDataManager();
        dataManager.addTable(CreateTableStatement.builder(TABLE).
                addField(Long.class, ID, KeyType.PRIMARY).
                addField(VarChar.ITEM_ID, ENTITY_TYPE).
                addField(VarChar.UUID, UUID, KeyType.UNIQUE).
                build());
        dataManager.registerItemDeleteConsumer(LimitedBridger.class, (conn, item) -> {
            DeleteStatement.builder(TABLE).
                    condition(ID, String.valueOf(item.getId())).
                    build().
                    execute(conn);
        });
    }

    private final SkyblockLevels plugin;
    private final String type;
    private final EntityType entityType;
    private final UUID entityUUID;
    @Nullable
    private Entity entity;
    @Nullable
    private final String displayName;
    private final WeightedList<ItemSupplier> drops;
    private final WeightedList<ItemSupplier> bonusItems;
    private final Range dropsOnDeath;

    public CustomEntity(
            SkyblockLevels plugin,
            String type,
            EntityType entityType,
            UUID uuid,
            @Nullable Entity entity,
            @Nullable String displayName,
            WeightedList<ItemSupplier> drops,
            WeightedList<ItemSupplier> bonusItems,
            Range dropsOnDeath
    ) {
        this.plugin = plugin;
        this.type = type;
        this.entityType = entityType;
        this.entityUUID = uuid;
        this.entity = entity;
        this.displayName = displayName;
        this.drops = drops;
        this.bonusItems = bonusItems;
        this.dropsOnDeath = dropsOnDeath;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public EntityType getEntityType() {
        return this.entityType;
    }

    @Override
    public UUID getUUID() {
        return this.entityUUID;
    }

    @Override
    @Nullable
    public String getDisplayName() {
        return displayName;
    }

    @Override
    @Nullable
    public Entity getEntity() {
        if (this.entity != null) return this.entity;
        this.entity = Bukkit.getEntity(this.entityUUID);
        return this.entity;
    }

    @Override
    @Nullable
    public WorldPosition getWorldPosition() {
        final Entity entity = this.getEntity();
        if (entity == null) return null;
        return WorldPosition.fromLocation(entity.getLocation());
    }

    @Override
    public boolean isAlive() {
        final Entity entity = this.getEntity();
        if (entity == null) return false;
        return !entity.isDead();
    }

    @Override
    public double getHealth() {
        final Entity entity = this.getEntity();
        if (!(entity instanceof final LivingEntity livingEntity)) return -1;
        return livingEntity.getHealth();
    }

    @Override
    public void onDamage(EntityDamageEvent event) {

    }

    @Override
    public void onDeath(EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();
        final Player killer = entity.getKiller();
        if (killer != null) {
            final User user = this.plugin.getUserManager().getUser(killer.getUniqueId());
            if (user != null) this.plugin.getUserManager().addCollectionAmount(user, this.type, 1);
        }
        this.dropBonus(entity.getLocation());
        if (this.dropsOnDeath.getMax() == 0 || this.drops.size() == 0) return;
        event.getDrops().clear();
        final Location location = entity.getLocation();
        final World world = location.getWorld();
        int i = this.dropsOnDeath.getRandom();

        while (i > 0) {
            final ItemSupplier itemSupplier = this.drops.getRandom();
            if (itemSupplier == null) break;
            final ItemStack item = itemSupplier.get();
            if (item == null || item.getType() == Material.AIR || item.getAmount() == 0) return;
            world.dropItem(location, item);
            i--;
        }
    }

    private void dropBonus(Location location) {
        final ItemSupplier itemSupplier = this.bonusItems.getRandom();
        if (itemSupplier == null) return;
        final ItemStack item = itemSupplier.get();
        if (item == null || item.getType() == Material.AIR || item.getAmount() == 0) return;
        location.getWorld().dropItem(location, item);
    }

    public static Serializer serializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements TypeSerializer<Function<Entity, CustomEntity>> {

        private static final Serializer INSTANCE = new Serializer();

        private static final String ID = "id";
        private static final String TYPE = "type";
        private static final String DISPLAY_NAME = "display-name";
        private static final String DROPS = "drops";
        private static final String BONUS_ITEMS = "bonus-items";
        private static final String DROP_RANGE = "drop-range";

        @Override
        public Function<Entity, CustomEntity> deserialize(Type type, ConfigurationNode node) throws SerializationException {
            final String entityId = node.node(ID).getString("");
            final EntityType entityType = EntityType.valueOf(node.node(TYPE).getString(""));
            final String displayName = node.node(DISPLAY_NAME).getString("");
            final TypeSerializer<WeightedList<ItemSupplier>> serializer = WeightedList.serializer(ItemSupplier.class, ItemSerializer.INSTANCE);
            final WeightedList<ItemSupplier> drops = serializer.deserialize(WeightedList.class, node.node(DROPS));
            final WeightedList<ItemSupplier> bonusItems = serializer.deserialize(WeightedList.class, node.node(BONUS_ITEMS));
            final Range dropsOnDeath = Range.serializer().deserialize(Range.class, node.node(DROP_RANGE));
            return (entity) -> new CustomEntity(SkyblockLevels.getPlugin(SkyblockLevels.class), entityId, entityType, entity.getUniqueId(), entity, displayName, drops, bonusItems, dropsOnDeath);
        }

        @Override
        public void serialize(Type type, @org.checkerframework.checker.nullness.qual.Nullable Function<Entity, CustomEntity> obj, ConfigurationNode node) throws SerializationException {

        }
    }
}
