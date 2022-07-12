package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.placeholder.Transformer;
import io.github.fisher2911.skyblocklevels.util.Range;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class IdItemSupplier implements ItemSupplier {

    private final ItemManager itemManager;
    private final String id;
    private final Range amount;

    public IdItemSupplier(ItemManager itemManager, String id, Range amount) {
        this.itemManager = itemManager;
        this.id = id;
        this.amount = amount;
    }

    @Override
    public ItemStack get() {
        final ItemStack itemStack = this.itemManager.getItem(this.itemManager.getItem(this.id)).clone();
        itemStack.setAmount(this.amount.getRandom());
        return itemStack;
    }

    @Override
    public ItemStack get(Map<Class<?>, Transformer<Object>> transformers, Object... args) {
        final ItemBuilder itemBuilder = ItemBuilder.from(this.get());
        return itemBuilder.get(transformers, args);
    }
}
