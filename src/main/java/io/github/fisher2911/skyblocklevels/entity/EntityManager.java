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
import io.github.fisher2911.skyblocklevels.database.VarChar;
import io.github.fisher2911.skyblocklevels.item.SkyBlock;
import io.github.fisher2911.skyblocklevels.message.Adventure;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.util.Keys;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
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
                addField(VarChar.UUID, UUID, KeyType.PRIMARY).
                addField(VarChar.ITEM_ID, ENTITY_TYPE).
                build());
    }

    private static final Path FILE_PATH = SkyblockLevels.getPlugin(SkyblockLevels.class).getDataFolder().toPath().resolve("entities.yml");

    private final SkyblockLevels plugin;
    private final Map<UUID, SkyEntity> entityMap;
    private final Map<String, Function<Entity, ? extends SkyEntity>> entityProvider;

    public EntityManager(SkyblockLevels plugin, Map<UUID, SkyEntity> entityMap, Map<String, Function<Entity, ? extends SkyEntity>> entityProvider) {
        this.plugin = plugin;
        this.entityMap = entityMap;
        this.entityProvider = entityProvider;
    }

    public void registerEntityProvider(String type, Function<Entity, ? extends SkyEntity> provider) {
        this.entityProvider.put(type, provider);
    }

    public SkyEntity create(String type, Entity entity) {
        final Function<Entity, ? extends SkyEntity> provider = this.entityProvider.get(type);
        if (provider == null) return SkyEntity.EMPTY;
        final SkyEntity skyEntity = provider.apply(entity);
        this.addEntity(skyEntity);
        return skyEntity;
    }

    public void addEntity(SkyEntity skyEntity) {
        this.entityMap.put(skyEntity.getUUID(), skyEntity);
        final Entity entity = skyEntity.getEntity();
        if (entity != null) {
            Keys.setEntity(entity, skyEntity.getType());
            final String displayName = skyEntity.getDisplayName();
            if (displayName != null) {
                entity.customName(Adventure.parse(displayName));
                entity.setCustomNameVisible(true);
            }
        }
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.saveEntity(skyEntity));
    }

    public SkyEntity getEntity(UUID uuid) {
        return this.entityMap.getOrDefault(uuid, SkyEntity.EMPTY);
    }

    public SkyEntity getEntity(Entity entity) {
        return this.getEntity(entity.getUniqueId());
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        final Entity entity = event.getEntity();
        if (event instanceof final SpawnerSpawnEvent e) {
            final SkyBlock skyBlock = this.plugin.getWorlds().getBlockAt(WorldPosition.fromLocation(e.getSpawner().getLocation()));
            if (skyBlock == SkyBlock.EMPTY) return;
            this.plugin.getItemManager().handle(User.SERVER, skyBlock, e);
        }
        if (!Keys.isSkyEntity(entity)) return;
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.loadEntity(entity));
    }

    @EventHandler
    public void onEntitySpawn(EntityAddToWorldEvent event) {
        final Entity entity = event.getEntity();
        if (!Keys.isSkyEntity(entity)) return;
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.loadEntity(entity));
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        final Collection<Entity> entities = event.getWorld().getEntities();
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            for (Entity entity : entities) {
                if (!Keys.isSkyEntity(entity)) continue;
                this.loadEntity(entity);
            }
        });
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        final Collection<Entity> entities = event.getWorld().getEntities();
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            for (Entity entity : entities) {
                if (!Keys.isSkyEntity(entity)) continue;
                this.loadEntity(entity);
            }
        });
    }

     @EventHandler
    public void onEntityRemove(EntityDeathEvent event) {
        final Entity entity = event.getEntity();
         if (!Keys.isSkyEntity(entity)) return;
         final UUID uuid = entity.getUniqueId();
         Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.deleteEntity(uuid));
    }

     @EventHandler
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        final Entity entity = event.getEntity();
        if (!Keys.isSkyEntity(entity)) return;
        final UUID uuid = entity.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.deleteEntity(uuid));
    }

     @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
         if (!this.plugin.isShuttingDown()) {
             Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.saveEntities(event.getWorld().getEntities()));
             return;
         }
         this.saveEntities(event.getWorld().getEntities());
    }

     @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (!this.plugin.isShuttingDown()) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.saveEntities(event.getChunk().getEntities()));
            return;
        }
        this.saveEntities(event.getChunk().getEntities());
    }

    private void saveEntities(Collection<Entity> entities) {
        for (Entity entity : entities) {
            if (!Keys.isSkyEntity(entity)) continue;
            final UUID uuid = entity.getUniqueId();
            final SkyEntity skyEntity = this.getEntity(uuid);
            if (skyEntity == SkyEntity.EMPTY) continue;
            this.saveEntity(skyEntity);
        }
    }

    private void saveEntities(Entity... entities) {
        for (Entity entity : entities) {
            if (!Keys.isSkyEntity(entity)) continue;
            final UUID uuid = entity.getUniqueId();
            final SkyEntity skyEntity = this.getEntity(uuid);
            if (skyEntity == SkyEntity.EMPTY) continue;
            this.saveEntity(skyEntity);
        }
    }

    public void loadEntity(Entity entity) {
        final List<SkyEntity> skyEntities = SelectStatement.builder(TABLE).
                condition(UUID, entity.getUniqueId().toString()).
                selectAll().
                build().
                execute(this.plugin.getDataManager().getConnection(), results -> {
                    final String type = results.getString(ENTITY_TYPE);
                    final SkyEntity skyEntity = this.create(type, entity);
                    if (skyEntity == SkyEntity.EMPTY) return null;
                    return skyEntity;
                });
        if (skyEntities.isEmpty()) return;
        this.addEntity(skyEntities.get(0));
    }

    public void saveEntity(SkyEntity entity) {
        InsertStatement.builder(TABLE).
                addEntry(UUID, entity.getUUID().toString()).
                addEntry(ENTITY_TYPE, entity.getType()).
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
                final String id = node.node("id").getString();
                final Function<Entity, CustomEntity> provider = CustomEntity.serializer().deserialize(CustomEntity.class, node);
                if (provider == null) {
                    this.plugin.getLogger().severe("Could not register entity type " + id);
                    continue;
                }
                this.plugin.getLogger().info("Registered entity type: " + id);
                this.entityProvider.put(id, provider);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
