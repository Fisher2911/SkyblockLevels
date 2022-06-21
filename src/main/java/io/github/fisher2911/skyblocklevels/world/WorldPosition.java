package io.github.fisher2911.skyblocklevels.world;

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
}
