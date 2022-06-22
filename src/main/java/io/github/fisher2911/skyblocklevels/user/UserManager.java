package io.github.fisher2911.skyblocklevels.user;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public class UserManager {

    private final Map<UUID, BukkitUser> users;

    public UserManager(Map<UUID, BukkitUser> users) {
        this.users = users;
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

}

