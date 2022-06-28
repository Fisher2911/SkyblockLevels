package io.github.fisher2911.skyblocklevels.world;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class BlockBreakManager implements Listener {

    private final SkyblockLevels plugin;
    private final Map<WorldPosition, BlockBreakData> blockBreakData;

    public BlockBreakManager(SkyblockLevels plugin) {
        this.plugin = plugin;
        this.blockBreakData = new ConcurrentHashMap<>();
    }

    public void startMining(Function<Player, Integer> tickFunction, Player player, WorldPosition position, Consumer<WorldPosition> onBreak) {
        final BlockBreakData current = this.blockBreakData.get(position);
        final int entityId = current == null ? Random.nextInt(10_000, 20_000) : current.getEntityId();
        this.blockBreakData.put(position, new BlockBreakData(entityId, player, player.getInventory().getItemInMainHand(), tickFunction.apply(player), onBreak));
    }

    public void updateTicks(Function<Player, Integer> tickFunction, WorldPosition position) {
        final BlockBreakData data = this.blockBreakData.get(position);
        if (data == null) return;
        data.setTotalTicks(tickFunction.apply(data.getPlayer()));
    }

    public void reset(WorldPosition position) {
        final BlockBreakData data = this.blockBreakData.get(position);
        if (data == null) return;
        data.reset(position);
    }

    public void cancel(WorldPosition position) {
        final BlockBreakData data = this.blockBreakData.remove(position);
        if (data == null) return;
        data.reset(position);
    }

    @EventHandler
    private void onTickEnd(ServerTickEndEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () ->
                        this.blockBreakData.entrySet().removeIf(entry -> {
                            final WorldPosition position = entry.getKey();
                            final BlockBreakData data = entry.getValue();
//                            data.getPlayer().sendMessage("Updating");
//                            Bukkit.getScheduler().runTask(
//                                    this.plugin,
//                                    () -> data.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 5, -1, false, false)));
                            data.send(position);
                            data.tick(position);
                            if (data.isBroken()) {
                                data.reset(position);
                                data.getOnBreak().accept(position);
                                return false;
                            }
                            return false;
                        })
        );
    }
}
