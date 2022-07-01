package io.github.fisher2911.skyblocklevels.user;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class UserManager {

    private static final String FILE_NAME = "user-settings.yml";
    private static final Path FILE_PATH = SkyblockLevels.getPlugin(SkyblockLevels.class).getDataFolder().toPath().resolve(FILE_NAME);

    private final Map<UUID, BukkitUser> users;
    private final Set<String> shouldStore;

    public UserManager(Map<UUID, BukkitUser> users, Set<String> shouldStore) {
        this.users = users;
        this.shouldStore = shouldStore;
    }

    @Nullable
    public BukkitUser getUser(UUID uuid) {
        return this.users.get(uuid);
    }

    @Nullable
    public BukkitUser getUser(Player player) {
        return this.users.get(player.getUniqueId());
    }

    public void addUser(BukkitUser user) {
        this.users.put(user.getId(), user);
    }

    public void removeUser(UUID uuid) {
        this.users.remove(uuid);
    }

    private static final String SHOULD_STORE_PATH = "should-store";

    public void load(SkyblockLevels plugin) {
        try {
            final File file = FILE_PATH.toFile();
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                plugin.saveResource(FILE_NAME, false);
            }
            final YamlConfigurationLoader loader = YamlConfigurationLoader.
                    builder().
                    path(FILE_PATH).
                    build();
            final ConfigurationNode source = loader.load();
            final ConfigurationNode shouldStore = source.node(SHOULD_STORE_PATH);
            this.shouldStore.clear();
            this.shouldStore.addAll(shouldStore.getList(String.class, new ArrayList<>()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

