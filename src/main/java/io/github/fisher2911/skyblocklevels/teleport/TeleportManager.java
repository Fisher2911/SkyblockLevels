package io.github.fisher2911.skyblocklevels.teleport;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.user.BukkitUser;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.function.Consumer;

public class TeleportManager {

    private final SkyblockLevels plugin;
    private final Set<TeleportData> teleportDataSet;
    private BukkitTask teleportTask;

    public TeleportManager(SkyblockLevels plugin, Set<TeleportData> teleportDataSet) {
        this.plugin = plugin;
        this.teleportDataSet = teleportDataSet;
    }

    public void startTask(TeleportData teleportData) {
        this.teleportDataSet.add(teleportData);
        if (this.teleportTask != null) return;
        this.startTask();
    }

    public void startTask(BukkitUser user, WorldPosition to, int seconds) {
        final Player player = user.getPlayer();
        if (player == null) return;
        final TeleportData teleportData = new TeleportData(user, WorldPosition.fromLocation(player.getLocation()), to, seconds, null);
        this.startTask(teleportData);
    }

    public void startTask(BukkitUser user, WorldPosition to, int seconds, Consumer<TeleportData> onSuccess) {
        final Player player = user.getPlayer();
        if (player == null) return;
        final TeleportData teleportData = new TeleportData(user, WorldPosition.fromLocation(player.getLocation()), to, seconds, onSuccess);
        this.startTask(teleportData);
    }

    private void startTask() {
        this.teleportTask = Bukkit.getScheduler().runTaskTimer(this.plugin,
                () -> {
                    this.teleportDataSet.removeIf(data -> {
                        final BukkitUser user = data.getUser();
                        final Player player = user.getPlayer();
                        if (player == null) return true;
                        final WorldPosition playerPosition = WorldPosition.fromLocation(player.getLocation());
                        if (!playerPosition.blocksEqual(data.getStartPosition())) data.setCancelled(true);
                        if (data.isCancelled()) {
                            user.sendMessage("<red>Your teleportation was cancelled because you moved.");
                            return true;
                        }
                        if (data.getCurrentSeconds() <= 0) {
                            user.sendMessage("<green>Teleported successfully!");
                            final Consumer<TeleportData> onSuccess = data.getOnSuccess();
                            if (onSuccess != null) onSuccess.accept(data);
                            player.teleport(data.getEndPosition().toLocation());
                            return true;
                        }
                        user.sendMessage("<green>Teleporting in " + data.getCurrentSeconds() + " seconds.");
                        data.decSecond();
                        return false;
                    });
                    if (this.teleportDataSet.isEmpty()) {
                        this.teleportTask.cancel();
                        this.teleportTask = null;
                    }
                }, 0, 20);
    }

}
