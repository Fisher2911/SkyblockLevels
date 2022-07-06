package io.github.fisher2911.skyblocklevels.world;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.database.CreateTableStatement;
import io.github.fisher2911.skyblocklevels.database.DeleteStatement;
import io.github.fisher2911.skyblocklevels.database.InsertStatement;
import io.github.fisher2911.skyblocklevels.database.KeyType;
import io.github.fisher2911.skyblocklevels.database.SelectStatement;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Worlds implements Listener {

    private static final String TABLE = "worlds";
    private static final String WORLD = "world";
    private static final String CHUNK_X = "chunk_x";
    private static final String CHUNK_Z = "chunk_z";
    private static final String BLOCK_ID = "block_id";
    private static final String BLOCK_TYPE = "block_type";
    private static final String TABLE_NAME = "table_name";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String Z = "z";

    static {
        SkyblockLevels.getPlugin(SkyblockLevels.class).getDataManager().addTable(CreateTableStatement.builder(TABLE).
                addField(String.class, WORLD).
                addField(Long.class, BLOCK_ID).
                addField(String.class, BLOCK_TYPE).
                addField(String.class, TABLE_NAME).
                addField(Integer.class, CHUNK_X).
                addField(Integer.class, CHUNK_Z).
                addField(Integer.class, X).
                addField(Integer.class, Y).
                addField(Integer.class, Z).
                groupKeys(KeyType.UNIQUE, WORLD, CHUNK_X, CHUNK_Z, BLOCK_ID, BLOCK_TYPE, TABLE_NAME, X, Y, Z).
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
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            this.saveChunk(world, map);
        });
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
                collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public void addBlock(SkyBlock block, WorldPosition position) {
        final WorldManager worldManager = this.worlds.get(position.getWorld().getUID());
        if (worldManager == null) return;
        worldManager.addBlock(block, position);
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            this.saveBlocks(Map.of(position, block));
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
        final SelectStatement statement = SelectStatement.builder(TABLE).
                selectAll().
                condition(CHUNK_X, chunkX).
                condition(CHUNK_Z, chunkY).
                condition(WORLD, world.toString()).
                build();
        statement.execute(this.plugin.getDataManager().getConnection(), result -> {
            final String tableName = result.getString(TABLE_NAME);
            final String itemId = result.getString(BLOCK_TYPE);
            final long id = result.getLong(BLOCK_ID);
            final int x = result.getInt(X);
            final int y = result.getInt(Y);
            final int z = result.getInt(Z);
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
            DeleteStatement.builder(TABLE_NAME).
                    condition(WORLD, worldPosition.getWorld().getUID().toString()).
                    condition(CHUNK_X, position.getChunkX()).
                    condition(CHUNK_Z, position.getChunkZ()).
                    condition(BLOCK_ID, block.getId()).
                    condition(BLOCK_TYPE, block.getItemId()).
                    condition(X, position.getX()).
                    condition(Y, position.getY()).
                    condition(Z, position.getZ()).
                    build().execute(this.plugin.getDataManager().getConnection());
        });
    }

    private void saveBlocks(Map<WorldPosition, SkyBlock> blocks) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin,
                () -> {
                    final InsertStatement.Builder builder = InsertStatement.builder(TABLE);
                    final int batchSize = 50;
                    Class<?> clazz = null;
                    for (var entry : blocks.entrySet()) {
                        final WorldPosition worldPosition = entry.getKey();
                        final Position position = worldPosition.getPosition();
                        final SkyBlock block = entry.getValue();
                        if (clazz == null) clazz = block.getClass();
                        builder.newEntry().
                                addEntry(WORLD, worldPosition.getWorld().getUID().toString()).
                                addEntry(BLOCK_ID, block.getId()).
                                addEntry(BLOCK_TYPE, block.getItemId()).
                                addEntry(TABLE_NAME, block.getTableName()).
                                addEntry(CHUNK_X, position.getChunkX()).
                                addEntry(CHUNK_Z, position.getChunkZ()).
                                addEntry(X, position.getX()).
                                addEntry(Y, position.getY()).
                                addEntry(Z, position.getZ()).
                                batchSize(batchSize).
                                build().
                                execute(this.plugin.getDataManager().getConnection());
                    }
                    if (clazz == null) return;
                    this.plugin.getDataManager().saveItems(blocks.values(), clazz);
                });
    }
}
