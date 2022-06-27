package io.github.fisher2911.skyblocklevels.world;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.packet.PacketHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class BlockBreakManager implements Listener {

    private final SkyblockLevels plugin;
    private final Map<WorldPosition, BlockBreakData> blockBreakData;

    public BlockBreakManager(SkyblockLevels plugin) {
        this.plugin = plugin;
        this.blockBreakData = new ConcurrentHashMap<>();
    }

    public void startMining(int totalTicks, Player player, WorldPosition position, Consumer<WorldPosition> onBreak) {
        this.blockBreakData.putIfAbsent(position, new BlockBreakData(player, player.getInventory().getItemInMainHand(), totalTicks, onBreak));
    }

    public void updateTicks(int totalTicks, WorldPosition position) {
        final BlockBreakData data = this.blockBreakData.get(position);
        if (data == null) return;
        data.setTotalTicks(totalTicks);
    }

    public void cancel(WorldPosition position) {
        final BlockBreakData data = this.blockBreakData.remove(position);
        if (data == null) return;
        PacketHelper.sendBlockBreakAnimation(data.getPlayer(), position.toLocation(), (byte) 0);
    }

    @EventHandler
    private void onTickEnd(ServerTickEndEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () ->
                        this.blockBreakData.entrySet().removeIf(entry -> {
                            final WorldPosition position = entry.getKey();
                            final BlockBreakData data = entry.getValue();
//                            data.getPlayer().sendMessage("Updating");
                            Bukkit.getScheduler().runTask(
                                    this.plugin,
                                    () -> data.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 5, -1, false, false)));
                            data.send(position);
                            data.tick();
                            if (data.isBroken()) {
                                data.getOnBreak().accept(position);
                                PacketHelper.sendBlockBreakAnimation(data.getPlayer(), position.toLocation(), (byte) 0);
                                data.getPlayer().sendMessage("Broken");
                                return true;
                            }
                            return false;
                        })
        );
    }
}
