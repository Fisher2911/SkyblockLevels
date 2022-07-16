package io.github.fisher2911.skyblocklevels.item.impl;

import com.google.common.collect.Multimaps;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.ItemBuilder;
import io.github.fisher2911.skyblocklevels.item.ItemSerializer;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.item.SpecialSkyItem;
import io.github.fisher2911.skyblocklevels.item.Usable;
import io.github.fisher2911.skyblocklevels.placeholder.Transformer;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.util.Keys;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Supplier;

public class DurableItem implements Usable, SpecialSkyItem {

    private final SkyblockLevels plugin;
    private final long id;
    private final String itemId;
    private final ItemSupplier itemSupplier;
    private final int maxDurability;

    private final Map<Class<?>, Transformer<Object>> placeholders;

    public DurableItem(SkyblockLevels plugin, long id, String itemId, ItemSupplier itemSupplier, int maxDurability) {
        this.plugin = plugin;
        this.id = id;
        this.itemId = itemId;
        this.itemSupplier = itemSupplier;
        this.maxDurability = maxDurability;
        this.placeholders = Map.of(ItemStack.class,
                Transformer.builder(Multimaps.newSetMultimap(new HashMap<>(), HashSet::new)).
                        with("%durability%", i -> {
                            ItemStack itemStack = (ItemStack) i;
                            if (itemStack.getType() == Material.AIR) return this.maxDurability;
                            return Keys.getDurability((ItemStack) i, this.maxDurability);
                        }).
                        build()
        );
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
        final int calculateDurability = this.calculateDurability(durability, maxDamage);
        damageable.setDamage(maxDamage - calculateDurability);
        this.updateLoreAndName(itemStack, itemMeta);
        itemStack.setItemMeta(itemMeta);
        if (durability < 0) {
            this.plugin.getItemManager().delete(this);
            itemStack.setAmount(0);
        }
        Keys.setDurability(itemStack, durability);
    }

    private void updateLoreAndName(ItemStack itemStack, ItemMeta itemMeta) {
        final ItemStack thisItem = this.itemSupplier.get(this.placeholders, itemStack);
        final ItemMeta otherMeta = thisItem.getItemMeta();
        itemMeta.displayName(otherMeta.displayName());
        itemMeta.lore(otherMeta.lore());
    }

    @Override
    public int getDurability() {
        return 0;
    }

    private int calculateDurability(int durability, int itemMaxDurability) {
        final float damagePercent = (float) durability / (float) this.maxDurability;
        return (int) (damagePercent * itemMaxDurability);
    }

    @Override
    public ItemStack getItemStack() {
        return this.itemSupplier.get(this.placeholders, ItemBuilder.EMPTY.build());
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

    public static Serializer serializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements TypeSerializer<Supplier<DurableItem>> {


        private static final Serializer INSTANCE = new Serializer();

        private Serializer() {
        }

        private static final String ITEM_ID = "item-id";
        private static final String ITEM = "item";
        private static final String UNIQUE = "unique";
        private static final String DURABILITY = "durability";

        @Override
        public Supplier<DurableItem> deserialize(Type type, ConfigurationNode node) {
            try {
                final String itemId = node.node(ITEM_ID).getString();
                final ItemSupplier itemSupplier = ItemSerializer.deserialize(node.node(ITEM));
                final boolean unique = node.node(UNIQUE).getBoolean();
                final int maxDurability = node.node(DURABILITY).getInt();
                final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
                return () -> new DurableItem(plugin, unique ? plugin.getDataManager().generateNextId() : -1, itemId, itemSupplier, maxDurability);
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void serialize(Type type, @Nullable Supplier<DurableItem> obj, ConfigurationNode node) throws SerializationException {

        }
    }

}
