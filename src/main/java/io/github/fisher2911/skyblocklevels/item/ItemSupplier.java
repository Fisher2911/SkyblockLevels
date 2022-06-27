package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.placeholder.Transformer;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public interface ItemSupplier {

    ItemStack get();
    ItemStack get(Map<Class<?>, Transformer<Object>> transformers, Object... args);

}
