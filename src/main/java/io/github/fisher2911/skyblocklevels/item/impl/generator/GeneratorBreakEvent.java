package io.github.fisher2911.skyblocklevels.item.impl.generator;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

public class GeneratorBreakEvent extends BlockBreakEvent {

    public GeneratorBreakEvent(@NotNull Block theBlock, @NotNull Player player) {
        super(theBlock, player);
    }
}
