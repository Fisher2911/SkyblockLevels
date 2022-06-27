package io.github.fisher2911.skyblocklevels.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockBreakAnimation;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.user.BukkitUser;
import io.github.fisher2911.skyblocklevels.world.Position;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PacketHelper {

    public static void sendBlockBreakAnimation(Player player, Location location, byte damage) {
        final WrapperPlayServerBlockBreakAnimation packet = new WrapperPlayServerBlockBreakAnimation(
                player.getEntityId(),
                new Vector3i(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                damage
        );
        sendPacketAsync(player, packet);
    }

    public static void sendPacketAsync(Player player, PacketWrapper<?> packet) {
        Bukkit.getScheduler().runTaskAsynchronously(
                SkyblockLevels.getPlugin(SkyblockLevels.class),
                () -> PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet)
        );
    }

    public static void registerListeners(SkyblockLevels plugin) {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) return;
                final WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
                if (!(event.getPlayer() instanceof final Player player)) return;
                final BukkitUser user = plugin.getUserManager().getUser(player);
                if (user == null) return;
                final Vector3i vector3i = packet.getBlockPosition();
                final Position position = new Position(vector3i.getX(), vector3i.getY(), vector3i.getZ());
                final WorldPosition worldPosition = new WorldPosition(player.getWorld(), position);
                if (packet.getAction() == DiggingAction.CANCELLED_DIGGING) {
                    plugin.getBlockBreakManager().cancel(worldPosition);
                    return;
                }
                if (packet.getAction() == DiggingAction.START_DIGGING) {
                    plugin.getWorlds().getBlockAt(worldPosition).onMineBlock(user, worldPosition.toLocation().getBlock());
                }
            }
        });
    }

}
