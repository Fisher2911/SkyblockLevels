package io.github.fisher2911.skyblocklevels.item;

import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageEvent;

public interface Spawner extends SkyBlock {

    void onSpawn(Entity entity);
    void onDamage(EntityDamageEvent entity);

}
