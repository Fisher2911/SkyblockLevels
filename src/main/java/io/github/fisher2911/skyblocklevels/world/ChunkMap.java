package io.github.fisher2911.skyblocklevels.world;

import io.github.fisher2911.skyblocklevels.item.SkyBlock;

import java.util.Map;

public class ChunkMap {

    private final int chunkX;
    private final int chunkZ;
    private final Map<Position, SkyBlock> blocks;

    public ChunkMap(int chunkX, int chunkZ, Map<Position, SkyBlock> blocks) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.blocks = blocks;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public Map<Position, SkyBlock> getBlocks() {
        return blocks;
    }

    public SkyBlock getBlockAt(Position position) {
        return blocks.getOrDefault(position, SkyBlock.EMPTY);
    }

    public SkyBlock getBlockAt(int x, int y, int z) {
        return this.getBlockAt(new Position(x, y, z));
    }
}
