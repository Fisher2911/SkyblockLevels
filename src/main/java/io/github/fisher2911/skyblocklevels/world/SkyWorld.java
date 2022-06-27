package io.github.fisher2911.skyblocklevels.world;

import org.bukkit.World;
import org.bukkit.WorldCreator;

public class SkyWorld {

    private World world;

    public void createWorld() {
        final WorldCreator creator = new WorldCreator("SkyWorld");
        creator.generator(new EmptyWorldGenerator());
        this.world = creator.createWorld();
    }

    public World getWorld() {
        return this.world;
    }

}
