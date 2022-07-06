package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.util.Range;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ItemSerializer implements TypeSerializer<ItemSupplier> {

    public static final ItemSerializer INSTANCE = new ItemSerializer();

    private ItemSerializer() {}

    public static ItemSupplier deserialize(ConfigurationNode node) throws SerializationException {
        return INSTANCE.deserialize(ItemSupplier.class, node);
    }

    private static final String ID = "id";
    private static final String MATERIAL = "material";
    private static final String AMOUNT = "amount";
    private static final String AMOUNT_RANGE = "amount-range";
    private static final String NAME = "name";
    private static final String LORE = "lore";
    private static final String ENCHANTMENTS = "enchantments";
    private static final String FLAGS = "flags";
    private static final String UNBREAKABLE = "unbreakable";
    private static final String GLOW = "glow";


    @Override
    public ItemSupplier deserialize(Type type, ConfigurationNode node) throws SerializationException {
        final var idNode = node.node(ID);
        if (!idNode.virtual()) {
            return new IdItemSupplier(SkyblockLevels.getPlugin(SkyblockLevels.class).getItemManager(), idNode.getString(""));
        }
        final Material material = Material.valueOf(node.node(MATERIAL).getString("").toUpperCase());
        final int amount = node.node(AMOUNT).getInt(1);
        final Range range = Objects.requireNonNullElse(Range.serializer().deserialize(Range.class, node.node(AMOUNT_RANGE)), Range.constant(amount));
        final String name = node.node(NAME).getString();
        final List<String> lore = node.node(LORE).getList(String.class);
        final boolean glow = node.node(GLOW).getBoolean();
        final Set<Map.Entry<Enchantment, Integer>> enchantments = node.node(ENCHANTMENTS).childrenMap().
                entrySet().
                stream().
                filter(e -> e.getKey() instanceof String).
                map(e -> Map.entry((String) e.getKey(), e.getValue().getInt())).
                map(e -> {
                    final Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(e.getKey().toString()));
                    if (enchantment == null) return null;
                    return Map.entry(enchantment, e.getValue());
                }).
                filter(e -> e != null).
                collect(Collectors.toSet());
        final Set<ItemFlag> flags = node.node(FLAGS).getList(String.class, new ArrayList<>()).
                stream().
                map(ItemFlag::valueOf).
                collect(Collectors.toSet());
        final boolean unbreakable = node.node(UNBREAKABLE).getBoolean();
        final ItemBuilder itemBuilder = ItemBuilder.from(material).amount(range);
        if (name != null && !name.isEmpty()) itemBuilder.name(name);
        if (lore != null && !lore.isEmpty()) itemBuilder.loreStr(lore);
        if (glow) itemBuilder.glow();
        for (var entry : enchantments) {
            itemBuilder.enchant(entry.getKey(), entry.getValue());
        }
        itemBuilder.addFlags(flags);
        if (unbreakable) itemBuilder.unbreakable();
        return itemBuilder;
    }

    @Override
    public void serialize(Type type, @Nullable ItemSupplier obj, ConfigurationNode node) throws SerializationException {

    }
}
