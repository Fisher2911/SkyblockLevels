package io.github.fisher2911.skyblocklevels.user;

import io.github.fisher2911.skyblocklevels.message.Adventure;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.UUID;

public class BukkitUser implements User {

    private final UUID uuid;
    private final Collection collection;
    private final Cooldowns cooldowns;
    private WeakReference<Player> playerReference;

    public BukkitUser(UUID uuid, Collection collection, Cooldowns cooldowns) {
        this.uuid = uuid;
        this.collection = collection;
        this.cooldowns = cooldowns;
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
    @NotNull
    public Audience getAudience() {
        final Player player = this.getPlayer();
        if (player == null) return Audience.empty();
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
        player.sendMessage(Adventure.MINI_MESSAGE.deserialize(message));
    }

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

    public void teleport(WorldPosition position) {
        final Player player = this.getPlayer();
        if (player == null) return;
        player.teleport(position.toLocation());
    }

    @Nullable
    public WorldPosition getPosition() {
        final Player player = this.getPlayer();
        if (player == null) return null;
        return WorldPosition.fromLocation(player.getLocation());
    }

    public Inventory getInventory() {
        final Player player = this.getPlayer();
        if (player == null) return null;
        return player.getInventory();
    }

    public void setHealth(double health) {
        final Player player = this.getPlayer();
        if (player == null) return;
        player.setHealth(health);
    }

    public double getHealth() {
        final Player player = this.getPlayer();
        if (player == null) return -1;
        return player.getHealth();
    }

    public void setFood(int food) {
        final Player player = this.getPlayer();
        if (player == null) return;
        player.setFoodLevel(food);
    }

    public void addPotionEffect(PotionEffect potionEffect) {
        final Player player = this.getPlayer();
        if (player == null) return;
        player.addPotionEffect(potionEffect);
    }

    public void removePotionEffect(PotionEffectType potionEffectType) {
        final Player player = this.getPlayer();
        if (player == null) return;
        player.removePotionEffect(potionEffectType);
    }

    @Override
    public Cooldowns getCooldowns() {
        return this.cooldowns;
    }
}
