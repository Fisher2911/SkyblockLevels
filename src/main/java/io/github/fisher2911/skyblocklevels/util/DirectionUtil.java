package io.github.fisher2911.skyblocklevels.util;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;

public class DirectionUtil {

    public static boolean isFacingUp(final Player player) {
        return player.getLocation().getPitch() < -70;
    }

    public static boolean isFacingDown(final Player player) {
        return player.getLocation().getPitch() > 70;
    }

    public static BlockFace getPlayerFace(final Player player) {
        if (isFacingUp(player)) return BlockFace.UP;
        if (isFacingDown(player)) return BlockFace.DOWN;
        return player.getFacing();
    }

    public static void setBlockDirection(Block block, Player player) {
        setBlockDirection(block, getPlayerFace(player).getOppositeFace());
    }

    public static void setBlockDirection(Block block, BlockFace face) {
        if (!(block.getBlockData() instanceof Directional directional)) return;
        directional.setFacing(face);
        block.setBlockData(directional);
    }

}
