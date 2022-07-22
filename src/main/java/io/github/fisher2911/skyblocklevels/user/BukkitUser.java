package io.github.fisher2911.skyblocklevels.user;

import io.github.fisher2911.skyblocklevels.booster.Booster;
import io.github.fisher2911.skyblocklevels.booster.BoosterType;
import io.github.fisher2911.skyblocklevels.booster.Boosters;
import io.github.fisher2911.skyblocklevels.message.Adventure;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BukkitUser implements User {

    private final UUID uuid;
    private final Collection collection;
    private final Cooldowns cooldowns;
    private final Boosters boosters;
    private WorldPosition lastInteractPosition;
    private int samePositionCount;

    public BukkitUser(UUID uuid, Collection collection, Cooldowns cooldowns, Boosters boosters) {
        this.uuid = uuid;
        this.collection = collection;
        this.cooldowns = cooldowns;
        this.boosters = boosters;
    }

    @Nullable
    public Player getPlayer() {
        return Bukkit.getPlayer(this.uuid);
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

    @Override
    public void sendMessage(Component message) {
        final Player player = this.getPlayer();
        if (player == null) return;
        player.sendMessage(message);
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

    public Boosters getBoosters() {
        return boosters;
    }

    public java.util.Collection<Booster> getBoosters(BoosterType boosterType) {
        return boosters.getBoosters(boosterType);
    }

    @Override
    public Cooldowns getCooldowns() {
        return this.cooldowns;
    }

    public WorldPosition getLastInteractPosition() {
        return lastInteractPosition;
    }

    @Override
    public String getName() {
        final Player player = this.getPlayer();
        if (player == null) return null;
        return player.getName();
    }

    public void setLastInteractPosition(WorldPosition lastInteractPosition, boolean onlyCountPitchAndYaw) {
        if (lastInteractPosition.equals(this.lastInteractPosition) && !onlyCountPitchAndYaw) {
            this.samePositionCount++;
        } else if (
                this.lastInteractPosition != null &&
                onlyCountPitchAndYaw &&
                lastInteractPosition.getPosition().getPitch() == this.lastInteractPosition.getPosition().getPitch() &&
                lastInteractPosition.getPosition().getYaw() == this.lastInteractPosition.getPosition().getYaw()
        ) {
            this.samePositionCount++;
        } else {
            this.samePositionCount = 0;
        }
        this.lastInteractPosition = lastInteractPosition;
    }

    public int getSamePositionCount() {
        return samePositionCount;
    }
}
