package io.github.fisher2911.skyblocklevels.world;

import org.bukkit.Chunk;

import java.util.Objects;

public class Position2D {

    private final int x;
    private final int y;

    public static Position2D fromChunk(Chunk chunk) {
        return new Position2D(chunk.getX(), chunk.getZ());
    }

    public Position2D(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Position2D that = (Position2D) o;
        return getX() == that.getX() && getY() == that.getY();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getX(), getY());
    }
}
