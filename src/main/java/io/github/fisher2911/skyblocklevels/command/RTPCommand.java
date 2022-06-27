package io.github.fisher2911.skyblocklevels.command;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.user.BukkitUser;
import io.github.fisher2911.skyblocklevels.util.Random;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;

import java.time.Duration;

public class RTPCommand extends SkyCommand {

    public RTPCommand(SkyblockLevels plugin) {
        super(plugin);
    }

    private static final Duration cooldownDuration = Duration.ofSeconds(30);
    private static final String COOLDOWN_ID = "rtp";

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
                                position.toLocation().getBlock().getRelative(BlockFace.DOWN).setType(Material.DIRT);
                                user.teleport(position);
                                user.sendMessage("<green>You have been randomly teleported to " + position.getPosition().getX() + " " + position.getPosition().getY() + " " + position.getPosition().getZ());
                                user.getCooldowns().addCooldown(COOLDOWN_ID, cooldownDuration);
                            }).execute();
                        })
        );
    }

    private Location getRandLocation() {
        final int x = Random.nextInt(-10_000, 10_000);
        final int y = 64;
        final int z = Random.nextInt(-10_000, 10_000);
        return new Location(this.plugin.getWorld().getWorld(), x, y, z);
    }
}
