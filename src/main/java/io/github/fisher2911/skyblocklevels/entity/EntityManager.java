package io.github.fisher2911.skyblocklevels.entity;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.database.CreateTableStatement;
import io.github.fisher2911.skyblocklevels.database.DataManager;
import io.github.fisher2911.skyblocklevels.database.DeleteStatement;
import io.github.fisher2911.skyblocklevels.database.InsertStatement;
import io.github.fisher2911.skyblocklevels.database.KeyType;
import io.github.fisher2911.skyblocklevels.database.SelectStatement;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class EntityManager implements Listener {

    private static final String TABLE = "entities";
    private static final String UUID = "uuid";
    private static final String ENTITY_TYPE = "entity_type";

    static {
        final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
        final DataManager dataManager = plugin.getDataManager();
        dataManager.addTable(CreateTableStatement.builder(TABLE).
                addField(String.class, UUID, KeyType.PRIMARY).
                addField(String.class, ENTITY_TYPE).
                build());
    }

    private static final Path FILE_PATH = SkyblockLevels.getPlugin(SkyblockLevels.class).getDataFolder().toPath().resolve("entities.yml");

    private final SkyblockLevels plugin;
    private final Map<UUID, SkyEntity> entityMap;
    private final Map<String, Function<UUID, ? extends SkyEntity>> entityProvider;

    public EntityManager(SkyblockLevels plugin, Map<UUID, SkyEntity> entityMap, Map<String, Function<UUID, ? extends SkyEntity>> entityProvider) {
        this.plugin = plugin;
        this.entityMap = entityMap;
        this.entityProvider = entityProvider;
    }

    public void registerEntityProvider(String type, Function<UUID, ? extends SkyEntity> provider) {
        this.entityProvider.put(type, provider);
    }

    public SkyEntity create(String type, UUID uuid) {
        final Function<UUID, ? extends SkyEntity> provider = this.entityProvider.get(type);
        if (provider == null) return SkyEntity.EMPTY;
        final SkyEntity entity = provider.apply(uuid);
        this.addEntity(entity);
        return entity;
    }

    public void addEntity(SkyEntity skyEntity) {
        this.entityMap.put(skyEntity.getUUID(), skyEntity);
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.saveEntity(skyEntity));
    }

    public SkyEntity getEntity(UUID uuid) {
        return this.entityMap.getOrDefault(uuid, SkyEntity.EMPTY);
    }

    public SkyEntity getEntity(Entity entity) {
        return this.getEntity(entity.getUniqueId());
    }

    // @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        final UUID uuid = event.getEntity().getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.loadEntity(uuid));
    }

    // @EventHandler
    public void onEntitySpawn(EntityAddToWorldEvent event) {
        final UUID uuid = event.getEntity().getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.loadEntity(uuid));
    }

    // @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        final Collection<Entity> entities = event.getWorld().getEntities();
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            for (Entity entity : entities) {
                final UUID uuid = entity.getUniqueId();
                this.loadEntity(uuid);
            }
        });
    }

    // @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        final Collection<Entity> entities = event.getWorld().getEntities();
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            for (Entity entity : entities) {
                final UUID uuid = entity.getUniqueId();
                this.loadEntity(uuid);
            }
        });
    }

    // @EventHandler
    public void onEntityRemove(EntityDeathEvent event) {
        final UUID uuid = event.getEntity().getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.deleteEntity(uuid));
    }

    // @EventHandler
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        final UUID uuid = event.getEntity().getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.deleteEntity(uuid));
    }

    // @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            for (Entity entity : event.getWorld().getEntities()) {
                final UUID uuid = entity.getUniqueId();
                final SkyEntity skyEntity = this.getEntity(uuid);
                if (skyEntity == SkyEntity.EMPTY) continue;
                this.saveEntity(skyEntity);
            }
        });
    }

    // @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            for (Entity entity : event.getChunk().getEntities()) {
                final UUID uuid = entity.getUniqueId();
                final SkyEntity skyEntity = this.getEntity(uuid);
                if (skyEntity == SkyEntity.EMPTY) continue;
                this.saveEntity(skyEntity);
            }
        });
    }

    public void loadEntity(UUID uuid) {
        final List<SkyEntity> entity = SelectStatement.builder(TABLE).
                condition(UUID, uuid.toString()).
                selectAll().
                build().
                execute(this.plugin.getDataManager().getConnection(), results -> {
                    final String type = results.getString(ENTITY_TYPE);
                    final SkyEntity skyEntity = this.create(type, uuid);
                    if (skyEntity == SkyEntity.EMPTY) return null;
                    return skyEntity;
                });
        if (entity.isEmpty()) return;
        this.addEntity(entity.get(0));
    }

    public void saveEntity(SkyEntity entity) {
        InsertStatement.builder(TABLE).
                addEntry(UUID, entity.getUUID().toString()).
                addEntry(ENTITY_TYPE, entity.getType()).
                condition(UUID, entity.getUUID().toString()).
                build().
                execute(this.plugin.getDataManager().getConnection());
    }

    public void deleteEntity(UUID uuid) {
        DeleteStatement.builder(TABLE).
                condition(UUID, uuid.toString()).
                build().
                execute(this.plugin.getDataManager().getConnection());
    }

    public void loadTypes() {
        this.entityProvider.clear();
        final File file = FILE_PATH.toFile();
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            this.plugin.saveResource(file.getName(), false);
        }
        final YamlConfigurationLoader loader = YamlConfigurationLoader.
                builder().
                path(FILE_PATH).
                build();

        try {
            final ConfigurationNode root = loader.load();
            for (var node : root.childrenMap().values()) {
                final String type = node.node("type").getString();
                final Function<UUID, CustomEntity> provider = CustomEntity.serializer().deserialize(CustomEntity.class, node);
                if (provider == null) continue;
                this.entityProvider.put(type, provider);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
