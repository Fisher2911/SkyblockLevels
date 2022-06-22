package io.github.fisher2911.skyblocklevels.user;

import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.inventory.Inventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface User {

    UUID getId();
    Collection getCollection();
    void sendMessage(String message);
    void forceChat(String message);
    void forceCommand(String command);
    void teleport(WorldPosition position);
    @Nullable
    WorldPosition getPosition();
    Inventory getInventory();
    void setHealth(double health);
    double getHealth();
    void setFood(int food);
    void addPotionEffect(PotionEffect potionEffect);
    void removePotionEffect(PotionEffectType potionEffectType);

}
