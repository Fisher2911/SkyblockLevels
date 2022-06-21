package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.util.ItemBuilder;
import org.bukkit.inventory.ItemStack;

public class SkyItem {

    private final String id;
    private final ItemBuilder itemBuilder;

    public SkyItem(String id, ItemBuilder itemBuilder) {
        this.id = id;
        this.itemBuilder = itemBuilder;
    }

    public String getId() {
        return id;
    }

    public ItemStack getItem() {
        return this.itemBuilder.build();
    }

}
