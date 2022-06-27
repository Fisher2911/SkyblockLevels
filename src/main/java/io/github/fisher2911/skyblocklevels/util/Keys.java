package io.github.fisher2911.skyblocklevels.util;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

public class Keys {

    private static final SkyblockLevels PLUGIN = SkyblockLevels.getPlugin(SkyblockLevels.class);
    private static final NamespacedKey ITEM_KEY = new NamespacedKey(PLUGIN, "item_id");
    private static final NamespacedKey ID_KEY = new NamespacedKey(PLUGIN, "item_unique_id");

    public static NamespacedKey create(String id) {
        return new NamespacedKey(PLUGIN, id);
    }

    public static long getSkyItem(ItemStack itemStack) {
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return -1;
        return Objects.requireNonNullElse(itemMeta.getPersistentDataContainer().get(ID_KEY, PersistentDataType.LONG), -1L);
    }

    public static String getSkyItemId(ItemStack itemStack) {
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return "";
        return Objects.requireNonNullElse(itemMeta.getPersistentDataContainer().get(ITEM_KEY, PersistentDataType.STRING), "");
    }

    public static boolean isSkyItem(ItemStack itemStack) {
        return getSkyItem(itemStack) != -1;
    }

    public static void setSkyItem(ItemStack itemStack, String id, long uniqueId) {
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return;
        itemMeta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING, id);
        itemMeta.getPersistentDataContainer().set(ID_KEY, PersistentDataType.LONG, uniqueId);
        itemStack.setItemMeta(itemMeta);
    }

    public static void setSkyItem(ItemStack itemStack, String id) {
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return;
        itemMeta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING, id);
        itemStack.setItemMeta(itemMeta);
    }
}
