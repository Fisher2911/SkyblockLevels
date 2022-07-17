package io.github.fisher2911.skyblocklevels.world;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.database.CreateTableStatement;
import io.github.fisher2911.skyblocklevels.database.DeleteStatement;
import io.github.fisher2911.skyblocklevels.database.InsertStatement;
import io.github.fisher2911.skyblocklevels.database.KeyType;
import io.github.fisher2911.skyblocklevels.database.SelectStatement;
import io.github.fisher2911.skyblocklevels.database.VarChar;
import io.github.fisher2911.skyblocklevels.item.SkyBlock;
import io.github.fisher2911.skyblocklevels.item.SpecialSkyItem;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Worlds implements Listener {

    public static final String DATABASE_TABLE_COLUMN = "worlds";
    public static final String DATABASE_WORLD_COLUMN = "world";
    public static final String DATABASE_CHUNK_X_COLUMN = "chunk_x";
    public static final String DATABASE_CHUNK_Z_COLUMN = "chunk_z";
    public static final String DATABASE_BLOCK_ID_COLUMN = "block_id";
    public static final String DATABASE_BLOCK_TYPE_COLUMN = "block_type";
    public static final String DATABASE_TABLE_NAME_COLUMN = "table_name";
    public static final String DATABASE_X_COLUMN = "x";
    public static final String DATABASE_Y_COLUMN = "y";
    public static final String DATABASE_Z_COLUMN = "z";

    static {
        SkyblockLevels.getPlugin(SkyblockLevels.class).getDataManager().addTable(CreateTableStatement.builder(DATABASE_TABLE_COLUMN).
                addField(VarChar.UUID, DATABASE_WORLD_COLUMN).
                addField(Long.class, DATABASE_BLOCK_ID_COLUMN).
                addField(VarChar.ITEM_ID, DATABASE_BLOCK_TYPE_COLUMN).
                addField(VarChar.of(50), DATABASE_TABLE_NAME_COLUMN).
                addField(Integer.class, DATABASE_CHUNK_X_COLUMN).
                addField(Integer.class, DATABASE_CHUNK_Z_COLUMN).
                addField(Integer.class, DATABASE_X_COLUMN).
                addField(Integer.class, DATABASE_Y_COLUMN).
                addField(Integer.class, DATABASE_Z_COLUMN).
                groupKeys(KeyType.UNIQUE, DATABASE_WORLD_COLUMN, DATABASE_CHUNK_X_COLUMN, DATABASE_CHUNK_Z_COLUMN, DATABASE_X_COLUMN, DATABASE_Y_COLUMN, DATABASE_Z_COLUMN).
                build());
    }

    private final SkyblockLevels plugin;
    private final Map<UUID, WorldManager> worlds;

    public Worlds(SkyblockLevels plugin, Map<UUID, WorldManager> worlds) {
        this.plugin = plugin;
        this.worlds = worlds;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        if (this.plugin.isShuttingDown()) return;
        final World world = event.getWorld();
        final WorldManager worldManager = new WorldManager(this.plugin, world, new ConcurrentHashMap<>());
        this.worlds.put(world.getUID(), worldManager);
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            for (Chunk chunk : world.getLoadedChunks()) {
                this.loadChunk(world.getUID(), chunk.getX(), chunk.getZ());
            }
            worldManager.startTicking();
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (this.plugin.isShuttingDown()) return;
        final Chunk chunk = event.getChunk();
        final World world = chunk.getWorld();
        final WorldManager worldManager = this.worlds.computeIfAbsent(world.getUID(), k -> new WorldManager(this.plugin, world, new ConcurrentHashMap<>()));
        worldManager.onChunkLoad(event);
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.loadChunk(world.getUID(), chunk.getX(), chunk.getZ()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        final Chunk chunk = event.getChunk();
        final World world = chunk.getWorld();
        final WorldManager worldManager = this.worlds.get(world.getUID());
        if (worldManager == null) return;
        final ChunkMap map = worldManager.onChunkUnload(event);
        if (map == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.saveChunk(world, map));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        final World world = event.getWorld();
        final WorldManager worldManager = this.worlds.remove(world.getUID());
        if (worldManager == null) return;
        worldManager.shutdown();
        worldManager.getChunks().forEach((position2D, chunkMap) -> this.saveChunk(world, chunkMap));
    }

    private void saveChunk(World world, ChunkMap chunkMap) {
        this.saveBlocks(chunkMap.getBlocks().entrySet().stream().
                map(e -> Map.entry(new WorldPosition(world, e.getKey()), e.getValue())).
                collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)), this.plugin.isShuttingDown());
    }

    public void addBlock(SkyBlock block, WorldPosition position) {
        if (block == null) return;
        final WorldManager worldManager = this.worlds.get(position.getWorld().getUID());
        if (worldManager == null) return;
        worldManager.addBlock(block, position);
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            this.saveBlocks(Map.of(position, block), this.plugin.isShuttingDown());
        });
    }

    public void removeBlock(WorldPosition worldPosition) {
        final WorldManager worldManager = this.worlds.get(worldPosition.getWorld().getUID());
        if (worldManager == null) return;
        final SkyBlock block = worldManager.removeBlock(worldPosition);
        if (block == SkyBlock.EMPTY) return;
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            this.deleteBlock(block, worldPosition);
        });
    }

    public SkyBlock getBlockAt(WorldPosition worldPosition) {
        final WorldManager worldManager = this.worlds.get(worldPosition.getWorld().getUID());
        if (worldManager == null) return SkyBlock.EMPTY;
        return worldManager.getBlockAt(worldPosition.getPosition());
    }

    public boolean isSkyBlock(WorldPosition worldPosition) {
        return !SkyBlock.isEmpty(this.getBlockAt(worldPosition));
    }

    private void loadChunk(UUID world, int chunkX, int chunkY) {
        final SelectStatement statement = SelectStatement.builder(DATABASE_TABLE_COLUMN).
                selectAll().
                condition(DATABASE_CHUNK_X_COLUMN, chunkX).
                condition(DATABASE_CHUNK_Z_COLUMN, chunkY).
                condition(DATABASE_WORLD_COLUMN, world.toString()).
                build();
        statement.execute(this.plugin.getDataManager().getConnection(), result -> {
            final String tableName = result.getString(DATABASE_TABLE_NAME_COLUMN);
            final String itemId = result.getString(DATABASE_BLOCK_TYPE_COLUMN);
            final long id = result.getLong(DATABASE_BLOCK_ID_COLUMN);
            final int x = result.getInt(DATABASE_X_COLUMN);
            final int y = result.getInt(DATABASE_Y_COLUMN);
            final int z = result.getInt(DATABASE_Z_COLUMN);
            final WorldPosition position = new WorldPosition(Bukkit.getWorld(world), new Position(x, y, z));
            final SpecialSkyItem item = this.plugin.getDataManager().loadItem(tableName, itemId, id);
            if (!(item instanceof final SkyBlock block)) return null;
            this.addBlock(block, position);
            return block;
        });
    }

    public void deleteBlock(SkyBlock block, WorldPosition worldPosition) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            final Position position = worldPosition.getPosition();
            DeleteStatement.builder(DATABASE_TABLE_COLUMN).
                    condition(DATABASE_WORLD_COLUMN, worldPosition.getWorld().getUID().toString()).
                    condition(DATABASE_CHUNK_X_COLUMN, position.getChunkX()).
                    condition(DATABASE_CHUNK_Z_COLUMN, position.getChunkZ()).
                    condition(DATABASE_BLOCK_TYPE_COLUMN, block.getItemId()).
                    condition(DATABASE_X_COLUMN, position.getX()).
                    condition(DATABASE_Y_COLUMN, position.getY()).
                    condition(DATABASE_Z_COLUMN, position.getZ()).
                    build().
                    execute(this.plugin.getDataManager().getConnection());
            this.plugin.getDataManager().deleteItem(block, block.getClass());
        });
    }

    private void saveBlocks(Map<WorldPosition, SkyBlock> blocks) {
//        final InsertStatement.Builder builder = ;
        final int batchSize = 50;
        final Multimap<Class<?>, SkyBlock> toSave = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);
        for (var entry : blocks.entrySet()) {
            final WorldPosition worldPosition = entry.getKey();
            final Position position = worldPosition.getPosition();
            final SkyBlock block = entry.getValue();
            toSave.put(block.getClass(), block);
            InsertStatement.builder(DATABASE_TABLE_COLUMN).
                    newEntry().
                    addEntry(DATABASE_WORLD_COLUMN, worldPosition.getWorld().getUID().toString()).
                    addEntry(DATABASE_BLOCK_ID_COLUMN, block.getId()).
                    addEntry(DATABASE_BLOCK_TYPE_COLUMN, block.getItemId()).
                    addEntry(DATABASE_TABLE_NAME_COLUMN, block.getTableName()).
                    addEntry(DATABASE_CHUNK_X_COLUMN, position.getChunkX()).
                    addEntry(DATABASE_CHUNK_Z_COLUMN, position.getChunkZ()).
                    addEntry(DATABASE_X_COLUMN, (int) position.getX()).
                    addEntry(DATABASE_Y_COLUMN, (int) position.getY()).
                    addEntry(DATABASE_Z_COLUMN, (int) position.getZ()).
                    batchSize(batchSize).
                    build().
                    execute(this.plugin.getDataManager().getConnection());
        }
        if (toSave.isEmpty()) return;
        for (var entry : toSave.asMap().entrySet()) {
            this.plugin.getDataManager().saveItems(entry.getValue(), entry.getKey());
        }
    }

    private void saveBlocks(Map<WorldPosition, SkyBlock> blocks, boolean async) {
        if (!async) {
            this.saveBlocks(blocks);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.saveBlocks(blocks));
    }
}
