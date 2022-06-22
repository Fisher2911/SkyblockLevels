package io.github.fisher2911.skyblocklevels.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemBuilder {

    public static ItemBuilder EMPTY = ItemBuilder.from(Material.AIR);

    private final Material material;
    private int amount;
    private ItemMeta itemMeta;

    private ItemBuilder(Material material) {
        this.material = material;
        this.amount = 1;
        this.itemMeta = Bukkit.getItemFactory().getItemMeta(material);
    }

    public static ItemBuilder from(Material material) {
        return new ItemBuilder(material);
    }

    public ItemStack build() {
        final ItemStack itemStack = new ItemStack(this.material, this.amount);
        itemStack.setItemMeta(this.itemMeta);
        return itemStack;
    }

}
