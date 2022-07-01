package io.github.fisher2911.skyblocklevels.entity;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.database.CreateTableStatement;
import io.github.fisher2911.skyblocklevels.database.DataManager;
import io.github.fisher2911.skyblocklevels.database.DeleteStatement;
import io.github.fisher2911.skyblocklevels.database.KeyType;
import io.github.fisher2911.skyblocklevels.item.ItemSerializer;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.item.impl.bridger.LimitedBridger;
import io.github.fisher2911.skyblocklevels.util.Range;
import io.github.fisher2911.skyblocklevels.util.weight.WeightedList;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
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
                addField(String.class, ENTITY_TYPE).
                addField(String.class, UUID, KeyType.UNIQUE).
                build());
        dataManager.registerItemDeleteConsumer(LimitedBridger.class, (conn, item) -> {
            DeleteStatement.builder(TABLE).
                    condition(ID, String.valueOf(item.getId())).
                    build().
                    execute(conn);
        });
    }

    private final String type;
    private final EntityType entityType;
    private final UUID entityUUID;
    private final WeightedList<ItemSupplier> drops;
    private final Range dropsOnDeath;

    public CustomEntity(String type, EntityType entityType, UUID entityUUID, WeightedList<ItemSupplier> drops, Range dropsOnDeath) {
        this.type = type;
        this.entityType = entityType;
        this.entityUUID = entityUUID;
        this.drops = drops;
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
    public Entity getEntity() {
        return Bukkit.getEntity(this.entityUUID);
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
        event.getDrops().clear();
        final Location location = entity.getLocation();
        final World world = location.getWorld();
        int i = this.dropsOnDeath.getRandom();

        while (i > 0) {
            final ItemSupplier itemSupplier = this.drops.getRandom();
            if (itemSupplier == null) break;
            world.dropItem(location, itemSupplier.get());
            i--;
        }
    }

    public static Serializer serializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements TypeSerializer<Function<UUID, CustomEntity>> {

        private static final Serializer INSTANCE = new Serializer();

        private static final String ID = "id";
        private static final String TYPE = "type";
        private static final String DROPS = "drops";
        private static final String DROP_RANGE = "drop-range";

        @Override
        public Function<UUID, CustomEntity> deserialize(Type type, ConfigurationNode node) throws SerializationException {
            final String entityId = node.node(ID).getString("");
            final EntityType entityType = EntityType.valueOf(node.node(TYPE).getString(""));
            final TypeSerializer<WeightedList<ItemSupplier>> serializer = WeightedList.serializer(ItemSupplier.class, ItemSerializer.INSTANCE);
            final WeightedList<ItemSupplier> drops = serializer.deserialize(WeightedList.class, node.node(DROPS));
            final Range dropsOnDeath = Range.serializer().deserialize(Range.class, node.node(DROP_RANGE));
            return (uuid) -> new CustomEntity(entityId, entityType, uuid, drops, dropsOnDeath);
        }

        @Override
        public void serialize(Type type, @org.checkerframework.checker.nullness.qual.Nullable Function<UUID, CustomEntity> obj, ConfigurationNode node) throws SerializationException {

        }
    }
}
