package io.github.fisher2911.skyblocklevels.user;

import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.UUID;

public class BukkitUser implements User {

    private final UUID uuid;
    private final Collection collection;
    private WeakReference<Player> playerReference;

    public BukkitUser(UUID uuid, Collection collection) {
        this.uuid = uuid;
        this.collection = collection;
        this.playerReference = new WeakReference<>(Bukkit.getPlayer(this.uuid));
    }

    @Nullable
    public Player getPlayer() {
        Player player = this.playerReference.get();
        if (player == null) {
            player = Bukkit.getPlayer(this.uuid);
            if (player == null) return null;
            this.playerReference = new WeakReference<>(player);
        }
        return player;
    }

    @Override
    public UUID getId() {
        return this.uuid;
    }

    @Override
    public Collection getCollection() {
        return this.collection;
    }

    @Override
    public void sendMessage(String message) {
        final Player player = this.getPlayer();
        if (player == null) return;
        player.sendMessage(message);
    }

    @Override
    public void forceChat(String message) {
        final Player player = this.getPlayer();
        if (player == null) return;
        player.chat(message);
    }

    @Override
    public void forceCommand(String command) {
        final Player player = this.getPlayer();
        if (player == null) return;
        player.performCommand(command);
    }

    @Override
    public void teleport(WorldPosition position) {
        final Player player = this.getPlayer();
        if (player == null) return;
        player.teleport(position.toLocation());
    }

    @Override
    @Nullable
    public WorldPosition getPosition() {
        final Player player = this.getPlayer();
        if (player == null) return null;
        return WorldPosition.fromLocation(player.getLocation());
    }

    @Override
    public Inventory getInventory() {
        final Player player = this.getPlayer();
        if (player == null) return null;
        return player.getInventory();
    }
}
