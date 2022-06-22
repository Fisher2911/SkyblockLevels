package io.github.fisher2911.skyblocklevels.util;

import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class WorldUtil {

    public static void addItemToInventory(ItemStack itemStack, User user) {
        final WorldPosition position = user.getPosition();
        final Inventory inventory = user.getInventory();
        inventory.addItem(itemStack).forEach((i, item) -> position.getWorld().dropItem(position.toLocation(), item));
    }

}
