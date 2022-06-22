package io.github.fisher2911.skyblocklevels.item.impl;

import io.github.fisher2911.skyblocklevels.item.SkyBlock;
import io.github.fisher2911.skyblocklevels.item.SkyItem;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.util.ItemBuilder;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import io.github.fisher2911.skyblocklevels.world.Worlds;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class HealBlock implements SkyBlock {

    private final SkyItem skyItem = new SkyItem("heal-block", ItemBuilder.from(Material.BEACON));
    private final Worlds worlds;

    public HealBlock(Worlds worlds) {
        this.worlds = worlds;
    }

    @Override
    public void onBreak(User user, BlockBreakEvent event) {
        final WorldPosition worldPosition = WorldPosition.fromLocation(event.getBlock().getLocation());
        worlds.removeBlock(worldPosition);
        user.sendMessage("No more healing for you!");
    }

    @Override
    public void onPlace(User user, BlockPlaceEvent event) {
        final WorldPosition worldPosition = WorldPosition.fromLocation(event.getBlock().getLocation());
        worlds.addBlock(this, worldPosition);
        user.sendMessage("You placed a healing block!");
    }

    @Override
    public void onClick(User user, PlayerInteractEvent event) {
        final WorldPosition position = WorldPosition.fromLocation(event.getClickedBlock().getLocation());
        final SkyBlock block = this.worlds.getBlockAt(position);
        if (!block.getClass().equals(this.getClass())) return;
        user.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 1));
        user.sendMessage("You have been healed!");
    }

    private static final List<Material> GLASS = List.of(Material.ORANGE_STAINED_GLASS, Material.RED_STAINED_GLASS, Material.BLUE_STAINED_GLASS);

    @Override
    public void tick(WorldPosition worldPosition) {
        final Block above = worldPosition.toLocation().getBlock().getRelative(BlockFace.UP);
        above.getWorld().spawnParticle(Particle.HEART, above.getLocation().add(0, 1, 0), 1);
        above.setType(GLASS.get((int) (Math.random() * GLASS.size())));
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public SkyItem getSkyItem() {
        return this.skyItem;
    }
}
