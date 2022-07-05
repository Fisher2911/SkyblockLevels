package io.github.fisher2911.skyblocklevels.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAnimation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockBreakAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRemoveEntityEffect;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.SkyBlock;
import io.github.fisher2911.skyblocklevels.user.BukkitUser;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.world.BlockBreakManager;
import io.github.fisher2911.skyblocklevels.world.Position;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PacketHelper {

    private static final Queue<PacketInfo> packetQueue = new ConcurrentLinkedQueue<>();
    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

    public static void sendBlockBreakAnimation(Player player, Location location, int entityId, byte damage) {
        packetQueue.add(new PacketInfo(
                player,
                new WrapperPlayServerBlockBreakAnimation(
                        entityId,
                        new Vector3i(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                        damage
                ),
                false
        ));
        sendPacket();
    }

    public static void sendMiningFatiguePacket(Player player) {
        packetQueue.add(new PacketInfo(
                player,
                new WrapperPlayServerEntityEffect(
                        player.getEntityId(),
                        PotionTypes.MINING_FATIGUE,
                        -1,
                        Integer.MAX_VALUE,
                        (byte) 0
                ),
                false
        ));
        sendPacket();
    }

    public static void removeMiningFatiguePacket(Player player) {
        packetQueue.add(new PacketInfo(player, new WrapperPlayServerRemoveEntityEffect(player.getEntityId(), PotionTypes.MINING_FATIGUE), true));
        sendPacket();
    }

    private static void sendPacket() {
        final PacketInfo packetInfo = packetQueue.poll();
        if (packetInfo == null) return;
        if (packetInfo.silent) {
            sendPacketSilentlyAsync(packetInfo);
        } else {
            sendPacketAsync(packetInfo);
        }
    }

    private static void sendPacketAsync(PacketInfo info) {
        EXECUTOR.execute(() -> PacketEvents.getAPI().getPlayerManager().sendPacket(info.player, info.packet));
    }

    private static void sendPacketSilentlyAsync(PacketInfo info) {
        EXECUTOR.execute(() -> PacketEvents.getAPI().getPlayerManager().sendPacketSilently(info.player, info.packet));
    }

    private static int lastBlockPlaceTick;
    private static int lastAnimationTick;

    public static void registerListeners(SkyblockLevels plugin) {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE) return;
                if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION) return;
                if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) return;
                if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) return;
                final int currentTick = Bukkit.getCurrentTick();
                if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
                    lastBlockPlaceTick = currentTick;
                    return;
                }
                if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
                    if (lastBlockPlaceTick == currentTick) return;
                    lastAnimationTick = currentTick;
                    final WrapperPlayClientAnimation packet = new WrapperPlayClientAnimation(event);
                    if (packet.getHand() != InteractionHand.MAIN_HAND) return;
                    if (!(event.getPlayer() instanceof final Player player)) return;
                    final RayTraceResult result = player.rayTraceBlocks(5);
                    if (result == null) return;
                    final Block target = result.getHitBlock();
                    if (target == null || !target.getType().isSolid()) return;
                    final WorldPosition position = WorldPosition.fromLocation(target.getLocation());
                    final SkyBlock skyBlock = plugin.getWorlds().getBlockAt(position);
                    if (skyBlock == SkyBlock.EMPTY) return;
                    final BlockBreakManager.BlockTickData tickData = plugin.getBlockBreakManager().tick(player.getUniqueId(), position);
                    if (tickData.getFirstTick() != tickData.getLastTick()) return;
                    final User user = plugin.getUserManager().getUser(player);
                    if (user == null) return;
                    handleMine(player, user, position, skyBlock);
                    return;
                }
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
                    removeMiningFatiguePacket(player);
                    if (SkyBlock.isEmpty(block)) {
//                        sendMiningFatiguePacket(player);
                        return;
                    }
                    plugin.getBlockBreakManager().cancel(worldPosition);
                    return;
                }
                if (packet.getAction() == DiggingAction.START_DIGGING) {
                    if (currentTick == lastAnimationTick) return;
                    final SkyBlock block = plugin.getWorlds().getBlockAt(worldPosition);
                    if (SkyBlock.isEmpty(block)) {
//                        removeMiningFatiguePacket(player);
                        return;
                    }
                    handleMine(player, user, worldPosition, block);
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

    private static void handleMine(Player player, User user, WorldPosition position, SkyBlock block) {
        sendBlockBreakAnimation(player, position.toLocation(), player.getEntityId(), (byte) -1);
        sendMiningFatiguePacket(player);
        block.onMineBlock(user, position.toLocation().getBlock());
    }

    private static record PacketInfo(Player player, PacketWrapper<?> packet, boolean silent) {
    }
}
