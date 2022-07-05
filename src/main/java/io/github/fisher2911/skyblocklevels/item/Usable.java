package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.user.User;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

public interface Usable {

    void onUse(User user, PlayerInteractEvent event);
    void onItemDamage(User user, PlayerItemDamageEvent event);
    int getDurability();
    void takeDamage(ItemStack itemStack, int damage);

}
