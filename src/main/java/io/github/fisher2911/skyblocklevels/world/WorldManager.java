package io.github.fisher2911.skyblocklevels.world;

import io.github.fisher2911.skyblocklevels.item.SkyBlock;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class WorldManager {

    private final World world;
    private final Map<Position2D, ChunkMap> chunks;

    public WorldManager(World world, Map<Position2D, ChunkMap> chunks) {
        this.world = world;
        this.chunks = chunks;
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

    @Nullable
    public SkyBlock getBlockAt(int x, int y, int z) {
        ChunkMap chunk = this.getChunkAt(x >> 4, z >> 4);
        if (chunk == null) {
            return null;
        }
        return chunk.getBlockAt(x & 15, y, z & 15);
    }

}
