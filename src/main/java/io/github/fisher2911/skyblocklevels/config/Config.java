package io.github.fisher2911.skyblocklevels.config;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.world.Position;
import org.bukkit.Location;

public class Config {

    private final SkyblockLevels plugin;

    private String spawnWorldName;
    private Position spawnPosition;

    public Config(SkyblockLevels plugin) {
        this.plugin = plugin;
    }

    public void load() {
        this.plugin.saveDefaultConfig();
        spawnWorldName = plugin.getConfig().getString("spawn.world", "SpawnWorld");
        spawnPosition = new Position(plugin.getConfig().getDouble("spawn.x"), plugin.getConfig().getDouble("spawn.y"), plugin.getConfig().getDouble("spawn.z"));
    }

    public SkyblockLevels getPlugin() {
        return plugin;
    }

    public String getSpawnWorldName() {
        return spawnWorldName;
    }

    public Position getSpawnPosition() {
        return spawnPosition;
    }

    public Location getSpawn() {
        return new Location(plugin.getServer().getWorld(spawnWorldName), spawnPosition.getX(), spawnPosition.getY(), spawnPosition.getZ());
    }
}
