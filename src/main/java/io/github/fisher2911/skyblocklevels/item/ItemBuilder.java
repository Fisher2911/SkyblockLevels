package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.message.Adventure;
import io.github.fisher2911.skyblocklevels.placeholder.Placeholder;
import io.github.fisher2911.skyblocklevels.placeholder.Transformer;
import io.github.fisher2911.skyblocklevels.util.Range;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ItemBuilder implements ItemSupplier {

    public static ItemBuilder EMPTY = ItemBuilder.from(Material.AIR);

    private final ItemStack itemStack;
    private Range itemRange;
    private final ItemMeta itemMeta;
    private boolean glow;

    private ItemBuilder(Material material) {
        this(new ItemStack(material));
    }

    private ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.itemRange = Range.constant(itemStack.getAmount());
        this.itemMeta = this.itemStack.getItemMeta();
    }

    public static ItemBuilder from(Material material) {
        return new ItemBuilder(material);
    }

    public static ItemBuilder from(ItemStack itemStack) {
        return new ItemBuilder(itemStack);
    }

    @Override
    public ItemStack get() {
        return this.build();
    }

    @Override
    public ItemStack get(Map<Class<?>, Transformer<Object>> transformers, Object... args) {
        return this.build(transformers, args);
    }

    public ItemStack build() {
        final ItemStack itemStack = this.itemStack.clone();
        final int amount = this.itemRange.getRandom();
        itemStack.setAmount(amount);
        if (this.itemMeta == null) return this.itemStack.clone();
        itemStack.setItemMeta(this.itemMeta.clone());
        return itemStack;
    }

    public ItemStack build(Map<Class<?>, Transformer<Object>> transformers, Object... args) {
        final ItemStack itemStack = this.itemStack.clone();
        final int amount = this.itemRange.getRandom();
        itemStack.setAmount(amount);
        if (this.itemMeta == null) return itemStack;
        final ItemMeta meta = this.itemMeta.clone();
        final List<Component> lore = meta.lore();
        if (lore != null) {
            meta.lore(lore.stream().map(
                    component -> {
                        final Placeholder placeholder = Placeholder.builder(component).transformers(transformers).build();
                        return placeholder.parse(args).get();
                    }
            ).collect(Collectors.toList()));
        }
        final Component displayName = meta.displayName();
        if (displayName != null) {
            final Placeholder placeholder = Placeholder.builder(displayName).transformers(transformers).build();
            meta.displayName(placeholder.parse(args).get());
        }
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public ItemBuilder amount(int amount) {
        this.itemRange = Range.constant(amount);
        return this;
    }

    public ItemBuilder amount(Range amount) {
        this.itemRange = amount;
        return this;
    }

    public ItemBuilder name(String name) {
        this.name(Adventure.parse(name));
        return this;
    }

    public ItemBuilder name(Component name) {
        if (this.itemMeta == null) return this;
        this.itemMeta.displayName(name);
        return this;
    }

    public ItemBuilder lore(List<Component> lore) {
        if (this.itemMeta == null) return this;
        this.itemMeta.lore(lore);
        return this;
    }

    public ItemBuilder lore(Component... lore) {
        if (this.itemMeta == null) return this;
        this.itemMeta.lore(Arrays.asList(lore));
        return this;
    }

    public ItemBuilder lore(String... lore) {
        if (this.itemMeta == null) return this;
        this.itemMeta.lore(Arrays.stream(lore).map(Adventure::parse).collect(Collectors.toList()));
        return this;
    }

    public ItemBuilder loreStr(List<String> lore) {
        if (this.itemMeta == null) return this;
        this.itemMeta.lore(lore.stream().map(Adventure::parse).collect(Collectors.toList()));
        return this;
    }

    public ItemBuilder lore(String line) {
        this.lore(Adventure.parse(line));
        return this;
    }

    public ItemBuilder lore(Component line) {
        if (this.itemMeta == null) return this;
        if (this.itemMeta.lore() == null) this.itemMeta.lore(new ArrayList<>());
        final List<Component> lore = this.itemMeta.lore();
        lore.add(line);
        this.itemMeta.lore(lore);
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        if (this.itemMeta == null) return this;
        this.glow = glow;
        if (glow) {
            this.itemMeta.addEnchant(Enchantment.LUCK, 1, true);
            if (this.itemMeta.getEnchants().size() <= 1) this.itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            return this;
        }
        this.itemMeta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
        this.itemMeta.removeEnchant(Enchantment.LUCK);
        return this;
    }

    public ItemBuilder glow() {
        this.glow(true);
        return this;
    }

    public ItemBuilder unbreakable(boolean unbreakable) {
        if (this.itemMeta == null) return this;
        this.itemMeta.setUnbreakable(unbreakable);
        return this;
    }

    public ItemBuilder unbreakable() {
        this.unbreakable(true);
        return this;
    }

    public ItemBuilder addFlags(ItemFlag... flag) {
        if (this.itemMeta == null) return this;
        this.itemMeta.addItemFlags(flag);
        return this;
    }

    public ItemBuilder addFlags(Collection<ItemFlag> flag) {
        if (this.itemMeta == null) return this;
        this.itemMeta.addItemFlags(flag.toArray(new ItemFlag[0]));
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        if (this.itemMeta == null) return this;
        if (this.glow) this.itemMeta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
        this.itemMeta.addEnchant(enchantment, level, true);
        return this;
    }

    public <T> ItemBuilder pdc(NamespacedKey key, PersistentDataType<T, T> type, T value) {
        if (this.itemMeta == null) return this;
        this.itemMeta.getPersistentDataContainer().set(key, type, value);
        return this;
    }

    public <T> ItemBuilder pdc(String key, T value) {
        this.pdc(
                new NamespacedKey(SkyblockLevels.getPlugin(SkyblockLevels.class), key),
                (PersistentDataType<T, T>) getType(value.getClass()),
                value
        );
        return this;
    }

    private static <T> PersistentDataType<? extends T, ? extends T> getType(Class<T> clazz) {
        if (clazz.isAssignableFrom(String.class)) return (PersistentDataType<T, T>) PersistentDataType.STRING;
        if (clazz.isAssignableFrom(Integer.class)) return (PersistentDataType<T, T>) PersistentDataType.INTEGER;
        if (clazz.isAssignableFrom(Long.class)) return (PersistentDataType<T, T>) PersistentDataType.LONG;
        if (clazz.isAssignableFrom(Double.class)) return (PersistentDataType<T, T>) PersistentDataType.DOUBLE;
        if (clazz.isAssignableFrom(Float.class)) return (PersistentDataType<T, T>) PersistentDataType.FLOAT;
        if (clazz.isAssignableFrom(Byte.class)) return (PersistentDataType<T, T>) PersistentDataType.BYTE;
        if (clazz.isAssignableFrom(Short.class)) return (PersistentDataType<T, T>) PersistentDataType.SHORT;
        if (clazz.isAssignableFrom(Byte[].class)) return (PersistentDataType<T, T>) PersistentDataType.BYTE_ARRAY;
        if (clazz.isAssignableFrom(Integer[].class)) return (PersistentDataType<T, T>) PersistentDataType.INTEGER_ARRAY;
        if (clazz.isAssignableFrom(Long[].class)) return (PersistentDataType<T, T>) PersistentDataType.LONG_ARRAY;
        throw new IllegalArgumentException("Unsupported type: " + clazz.getName());
    }
}
