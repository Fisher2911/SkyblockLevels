package io.github.fisher2911.skyblocklevels.world;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.SkyBlock;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class WorldManager {

    private final SkyblockLevels plugin;
    private final World world;
    private final Map<Position2D, ChunkMap> chunks;
    private BukkitTask task;

    public WorldManager(SkyblockLevels plugin, World world, Map<Position2D, ChunkMap> chunks) {
        this.plugin = plugin;
        this.world = world;
        this.chunks = chunks;
    }

    public void onChunkLoad(ChunkLoadEvent event) {
        final Chunk chunk = event.getChunk();
        final Position2D position = new Position2D(chunk.getX(), chunk.getZ());
        this.chunks.put(position, new ChunkMap(chunk.getX(), chunk.getZ(), new ConcurrentHashMap<>()));
    }

    @Nullable
    public ChunkMap onChunkUnload(ChunkUnloadEvent event) {
        final Chunk chunk = event.getChunk();
        return this.chunks.remove(new Position2D(chunk.getX(), chunk.getZ()));
    }

    public World getWorld() {
        return world;
    }

    public Map<Position2D, ChunkMap> getChunks() {
        return chunks;
    }

    public ChunkMap getChunkAt(int chunkX, int chunkZ) {
        return this.chunks.get(new Position2D(chunkX, chunkZ));
    }

    public SkyBlock getBlockAt(int x, int y, int z) {
        return this.getBlockAt(new Position(x, y, z));
    }
    public SkyBlock getBlockAt(Position position) {
        final int x = (int) position.getX();
        final int y = (int) position.getY();
        final int z = (int) position.getZ();
        final ChunkMap chunk = this.getChunkAt(x >> 4, z >> 4);
        if (chunk == null) return SkyBlock.EMPTY;
        return chunk.getBlockAt(x, y, z);
    }

    public void addBlock(SkyBlock block, WorldPosition worldPosition) {
        final Position position = worldPosition.getPosition();
        ChunkMap chunk = this.getChunkAt((int) position.getX() >> 4, (int) position.getZ() >> 4);
        if (chunk == null) {
            chunk = new ChunkMap((int) position.getX() >> 4, (int) position.getZ() >> 4, new ConcurrentHashMap<>());
            this.chunks.put(new Position2D(chunk.getChunkX(), chunk.getChunkZ()), chunk);
        }
        chunk.addBlock(position, block);
    }

    public SkyBlock removeBlock(WorldPosition worldPosition) {
        final Position position = worldPosition.getPosition();
        final ChunkMap chunk = this.getChunkAt((int) position.getX() >> 4, (int) position.getZ() >> 4);
        if (chunk == null) return SkyBlock.EMPTY;
        return Objects.requireNonNullElse(chunk.removeBlock(position), SkyBlock.EMPTY);
    }

    public void startTicking() {
        if (this.task != null) return;
        this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(
                this.plugin,
                () -> {
                    final Map<WorldPosition, SkyBlock> syncBlocks = new HashMap<>();
                    for (ChunkMap chunk : this.chunks.values()) {
                        chunk.getBlocks().forEach((position, block) -> {
                            final WorldPosition worldPosition = position.toWorldPosition(this.world);
                            if (!block.isAsync()) {
                                syncBlocks.put(worldPosition, block);
                                return;
                            }
                            block.tick(worldPosition);
                        });
                    }
                    Bukkit.getScheduler().runTask(
                            this.plugin,
                            () -> syncBlocks.forEach((worldPosition, block) -> block.tick(worldPosition))
                    );
                },
                1,
                1
        );
    }

    public void shutdown() {
        this.task.cancel();
        this.task = null;
    }

}
