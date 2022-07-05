package io.github.fisher2911.skyblocklevels;

import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.item.SpecialSkyItem;
import io.github.fisher2911.skyblocklevels.item.Usable;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.util.Keys;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class DurableItem implements Usable, SpecialSkyItem {

    private final SkyblockLevels plugin;
    private final long id;
    private final String itemId;
    private final ItemSupplier itemSupplier;
    private final int maxDurability;

    public DurableItem(SkyblockLevels plugin, long id, String itemId, ItemSupplier itemSupplier, int maxDurability) {
        this.plugin = plugin;
        this.id = id;
        this.itemId = itemId;
        this.itemSupplier = itemSupplier;
        this.maxDurability = maxDurability;
    }

    @Override
    public void onUse(User user, PlayerInteractEvent event) {

    }

    @Override
    public void onItemDamage(User user, PlayerItemDamageEvent event) {
        if (event.isCancelled()) return;
        final ItemStack itemStack = event.getItem();
        this.updateDurability(itemStack, 1);
    }

    public void takeDamage(ItemStack itemStack, int damage) {
        this.updateDurability(itemStack, damage);
    }

    public void updateDurability(ItemStack itemStack, int damage) {
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (!(itemMeta instanceof final Damageable damageable)) return;
        final int durability = Keys.getDurability(itemStack, this.maxDurability) - damage;
        final int maxDamage = itemStack.getType().getMaxDurability();
        final int calculateDurability = this.calculateDurability(maxDamage);
        damageable.setDamage(maxDamage - calculateDurability);
        Keys.setDurability(itemStack, durability);
    }

    @Override
    public int getDurability() {
        return 0;
    }

    private int calculateDurability(int itemMaxDurability) {
        final float damagePercent = (float) itemMaxDurability / (float) this.maxDurability;
        return (int) (damagePercent * itemMaxDurability);
    }

    @Override
    public ItemStack getItemStack() {
        return this.itemSupplier.get();
    }

    @Override
    public String getItemId() {
        return this.itemId;
    }

    @Override
    public long getId() {
        return this.id;
    }

    @Override
    public boolean uniqueInInventory() {
        return true;
    }


}
