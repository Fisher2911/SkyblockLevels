package io.github.fisher2911.skyblocklevels.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockBreakAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRemoveEntityEffect;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.SkyBlock;
import io.github.fisher2911.skyblocklevels.user.BukkitUser;
import io.github.fisher2911.skyblocklevels.world.Position;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PacketHelper {

    public static void sendBlockBreakAnimation(Player player, Location location, int entityId, byte damage) {
        final WrapperPlayServerBlockBreakAnimation packet = new WrapperPlayServerBlockBreakAnimation(
                entityId,
                new Vector3i(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                damage
        );
        sendPacketAsync(player, packet);
    }

    public static void sendMiningFatiguePacket(Player player) {
        final WrapperPlayServerEntityEffect packet = new WrapperPlayServerEntityEffect(
                player.getEntityId(),
                PotionTypes.MINING_FATIGUE,
                -1,
                Integer.MAX_VALUE,
                (byte) 0
        );
        sendPacketAsync(player, packet);
    }

    public static void removeMiningFatiguePacket(Player player) {
        sendPacketSilentlyAsync(player, new WrapperPlayServerRemoveEntityEffect(player.getEntityId(), PotionTypes.MINING_FATIGUE));
    }

    public static void sendPacketAsync(Player player, PacketWrapper<?> packet) {
        Bukkit.getScheduler().runTaskAsynchronously(
                SkyblockLevels.getPlugin(SkyblockLevels.class),
                () -> PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet)
        );
    }

    public static void sendPacketSilentlyAsync(Player player, PacketWrapper<?> packet) {
        Bukkit.getScheduler().runTaskAsynchronously(
                SkyblockLevels.getPlugin(SkyblockLevels.class),
                () -> PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, packet)
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
                if (packet.getAction() == DiggingAction.CANCELLED_DIGGING || packet.getAction() == DiggingAction.FINISHED_DIGGING) {
                    final SkyBlock block = plugin.getWorlds().getBlockAt(worldPosition);
                    if (SkyBlock.isEmpty(block)) {
                        sendMiningFatiguePacket(player);
                        return;
                    }
                    plugin.getBlockBreakManager().cancel(worldPosition);
                    return;
                }
                if (packet.getAction() == DiggingAction.START_DIGGING) {
                    final SkyBlock block = plugin.getWorlds().getBlockAt(worldPosition);
                    if (SkyBlock.isEmpty(block)) {
                        removeMiningFatiguePacket(player);
                        return;
                    }
                    block.onMineBlock(user, worldPosition.toLocation().getBlock());
                }
            }
        });
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
            @Override
            public void onPacketSend(PacketSendEvent event) {
                if (event.getPacketType() != PacketType.Play.Server.REMOVE_ENTITY_EFFECT) return;
                final WrapperPlayServerRemoveEntityEffect packet = new WrapperPlayServerRemoveEntityEffect(event);
                if (!(event.getPlayer() instanceof Player)) return;
                if (packet.getPotionType() == PotionTypes.MINING_FATIGUE) event.setCancelled(true);
            }
        });
    }

}
