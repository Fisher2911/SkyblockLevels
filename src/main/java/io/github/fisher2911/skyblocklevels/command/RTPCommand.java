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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

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
                                user.sendMessage("<red>You must wait " + user.getCooldowns().getTimePassed(COOLDOWN_ID).toSeconds() + " before using this command again.");
                                return;
                            }
                            Location teleportTo = this.getRandLocation();
                            while (this.plugin.getLands().isClaimed(teleportTo)) {
                                teleportTo = this.getRandLocation();
                            }
                            final WorldPosition position = WorldPosition.fromLocation(teleportTo);
                            this.manager.taskRecipe().begin(context).synchronous(c -> {
                                user.getCooldowns().addCooldown(COOLDOWN_ID, cooldownDuration);
                                final World world = position.getWorld();
                                final Position pos = position.getPosition();
                                final TeleportData data = new TeleportData(user, position);
                                if (world.isChunkLoaded(pos.getChunkX(), pos.getChunkZ())) {
                                    this.teleportUser(data);
                                    return;
                                }
                                final Location location = position.toLocation();
                                this.toTeleport.put(new Position2D(pos.getChunkX(), pos.getChunkZ()), data);
                                position.getWorld().loadChunk(location.getChunk());
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
        user.sendMessage("<green>You have been randomly teleported to " + position.getPosition().getX() + " " + position.getPosition().getY() + " " + position.getPosition().getZ());
    }

    private Location getRandLocation() {
        final int x = Random.nextInt(-10_000, 10_000);
        final int y = 64;
        final int z = Random.nextInt(-10_000, 10_000);
        return new Location(this.plugin.getWorld().getWorld(), x, y, z);
    }

    private static class TeleportData {

        private final BukkitUser user;
        private final WorldPosition position;

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
    }
}
