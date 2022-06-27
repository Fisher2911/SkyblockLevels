package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.placeholder.Transformer;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class IdItemSupplier implements ItemSupplier {

    private final ItemManager itemManager;
    private final String id;

    public IdItemSupplier(ItemManager itemManager, String id) {
        this.itemManager = itemManager;
        this.id = id;
    }

    @Override
    public ItemStack get() {
        return this.itemManager.getItem(this.itemManager.getItem(this.id));
    }

    @Override
    public ItemStack get(Map<Class<?>, Transformer<Object>> transformers, Object... args) {
        return this.itemManager.getItem(this.itemManager.getItem(this.id));
    }
}
