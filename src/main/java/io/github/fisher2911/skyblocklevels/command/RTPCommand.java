package io.github.fisher2911.skyblocklevels.command;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.user.BukkitUser;
import io.github.fisher2911.skyblocklevels.util.Random;
import io.github.fisher2911.skyblocklevels.world.Position;
import io.github.fisher2911.skyblocklevels.world.Position2D;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class RTPCommand extends SkyCommand implements Listener {

    public RTPCommand(SkyblockLevels plugin) {
        super(plugin);
    }

    private static final Duration cooldownDuration = Duration.ofSeconds(30);
    private static final String COOLDOWN_ID = "rtp";

    private final Map<Position2D, TeleportData> toTeleport = new HashMap<>();

    @Override
    public void register() {
        this.manager.command(
                this.manager.commandBuilder("rtp").
                        permission(Permission.RANDOM_TP).
                        senderType(BukkitUser.class).
                        handler(context -> {
                            final BukkitUser user = (BukkitUser) context.getSender();
                            if (user.getCooldowns().isOnCooldown(COOLDOWN_ID)) {
                                user.sendMessage("<red>You must wait " + (30 - user.getCooldowns().getTimePassed(COOLDOWN_ID).toSeconds()) + " before using this command again.");
                                return;
                            }
                            final Player player = user.getPlayer();
                            if (player == null) return;
                            final World playerWorld = player.getWorld();
                            if (!playerWorld.getName().contains("SpawnWorld")) {
                                user.sendMessage("<red>You must be in the spawn world to use this command.");
                                return;
                            }
                            Location teleportTo = this.getRandLocation();
                            while (this.plugin.getLands().isClaimed(teleportTo)) {
                                teleportTo = this.getRandLocation();
                            }
                            final WorldPosition position = WorldPosition.fromLocation(teleportTo);
                            this.manager.taskRecipe().begin(context).synchronous(c -> {
                                final World world = position.getWorld();
                                final Position pos = position.getPosition();
                                final TeleportData data = new TeleportData(user, position);
                                if (world.isChunkLoaded(pos.getChunkX(), pos.getChunkZ())) {
                                    user.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 3, 5));
                                    this.teleportUser(data);
                                    user.getCooldowns().addCooldown(COOLDOWN_ID, cooldownDuration);
                                    return;
                                }
                                world.loadChunk(pos.getChunkX(), pos.getChunkZ());
                                position.toLocation().getBlock().getRelative(BlockFace.DOWN).setType(Material.DIRT);
                                this.plugin.getTeleportManager().startTask(
                                        user,
                                        position,
                                        3,
                                        t -> {
                                            user.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 8, 5));
                                            user.getCooldowns().addCooldown(COOLDOWN_ID, cooldownDuration);
                                        }
                                );
                            }).execute();
                        })
        );
    }

    @EventHandler
    public void onChunkLoad(PlayerTeleportEvent event) {
        final Chunk chunk = event.getTo().getChunk();
        final TeleportData data = this.toTeleport.remove(Position2D.fromChunk(chunk));
        if (data == null) return;
        teleportUser(data);
    }

    private void teleportUser(TeleportData data) {
        final BukkitUser user = data.getUser();
        final WorldPosition position = data.getPosition();
        position.toLocation().getBlock().getRelative(BlockFace.DOWN).setType(Material.DIRT);
        user.teleport(position);
        user.sendMessage("<green>Teleported successfully!");
    }

    private Location getRandLocation() {
        final int x = Random.nextInt(-10_000, 10_000);
        final int y = 64;
        final int z = Random.nextInt(-10_000, 10_000);
        return new Location(this.plugin.getWorld().getWorld(), x + 0.5, y, z + 0.5);
    }

    private static class TeleportData {

        private final BukkitUser user;
        private final WorldPosition position;
        private boolean cancelled;

        public TeleportData(BukkitUser user, WorldPosition position) {
            this.user = user;
            this.position = position;
        }

        public BukkitUser getUser() {
            return user;
        }

        public WorldPosition getPosition() {
            return position;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }
}
