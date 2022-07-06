package io.github.fisher2911.skyblocklevels.listener;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class VoidDamageListener implements Listener {

    private final SkyblockLevels plugin;

    public VoidDamageListener(SkyblockLevels plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onVoidDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) return;
        if (!(event.getEntity() instanceof final Player player)) return;
        final World world = player.getWorld();
        if (!world.getName().equalsIgnoreCase("spawnworld")) return;
        event.setCancelled(true);
        player.teleport(world.getSpawnLocation());
    }
}
