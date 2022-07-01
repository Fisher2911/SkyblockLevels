package io.github.fisher2911.skyblocklevels.entity;

import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface SkyEntity {

    SkyEntity EMPTY = new SkyEntity() {
        @Override
        public String getType() { return ""; }
        @Override
        public EntityType getEntityType() { return EntityType.UNKNOWN; }
        @Override
        public UUID getUUID() { return null; }
        @Override
        @Nullable
        public Entity getEntity() { return null; }
        @Override
        @Nullable
        public WorldPosition getWorldPosition() { return null; }
        @Override
        public boolean isAlive() { return false; }
        @Override
        public double getHealth() { return 0; }
        @Override
        public void onDamage(EntityDamageEvent event) {}
        @Override
        public void onDeath(EntityDeathEvent event) {}
    };

    String getType();
    EntityType getEntityType();
    UUID getUUID();
    @Nullable Entity getEntity();
    @Nullable WorldPosition getWorldPosition();
    boolean isAlive();
    double getHealth();
    void onDamage(EntityDamageEvent entity);
    void onDeath(EntityDeathEvent entity);

}
