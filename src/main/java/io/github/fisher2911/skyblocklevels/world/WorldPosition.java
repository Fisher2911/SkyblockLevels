package io.github.fisher2911.skyblocklevels.world;

import org.bukkit.Location;
import org.bukkit.World;

public class WorldPosition {

    private final World world;
    private final Position position;

    public WorldPosition(World world, Position position) {
        this.world = world;
        this.position = position;
    }

    public World getWorld() {
        return world;
    }

    public Position getPosition() {
        return position;
    }

    public Location toLocation() {
        return new Location(world, position.getX(), position.getY(), position.getZ());
    }

    public static WorldPosition fromLocation(Location location) {
        return new WorldPosition(location.getWorld(), new Position(location.getX(), location.getY(), location.getZ()));
    }
}
