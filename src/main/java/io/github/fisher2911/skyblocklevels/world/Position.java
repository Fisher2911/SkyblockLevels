package io.github.fisher2911.skyblocklevels.world;

import org.bukkit.World;
import org.bukkit.block.BlockFace;

import java.util.Objects;

public class Position {

    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public Position(double x, double y, double z) {
        this(x, y, z, 0, 0);
    }

    public Position(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public int distanceSquared(Position other) {
        return (int) (Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2) + Math.pow(z - other.z, 2));
    }

    public Position getRelative(BlockFace face) {
        return new Position(x + face.getModX(), y + face.getModY(), z + face.getModZ());
    }

    public int getChunkX() {
        return (int) Math.floor(x / 16);
    }

    public int getChunkZ() {
        return (int) Math.floor(z / 16);
    }

    public WorldPosition toWorldPosition(World world) {
        return new WorldPosition(world, this);
    }

    public boolean blocksEqual(Position other) {
        return (int) x == (int) other.x && (int) y == (int) other.y && (int) z == (int) other.z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Position position = (Position) o;
        return Double.compare(position.getX(), getX()) == 0 && Double.compare(position.getY(), getY()) == 0 && Double.compare(position.getZ(), getZ()) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getX(), getY(), getZ());
    }

    @Override
    public String toString() {
        return "Position{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}
