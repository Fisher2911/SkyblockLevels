package io.github.fisher2911.skyblocklevels.listener;

import io.github.fisher2911.skyblocklevels.entity.EntityManager;
import io.github.fisher2911.skyblocklevels.entity.SkyEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityListener implements Listener {

    private final EntityManager entityManager;

    public EntityListener(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        final SkyEntity entity = this.entityManager.getEntity(event.getEntity().getUniqueId());
        entity.onDamage(event);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        final SkyEntity entity = this.entityManager.getEntity(event.getEntity().getUniqueId());
        entity.onDeath(event);
    }

}
