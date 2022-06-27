package io.github.fisher2911.skyblocklevels.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class MineSpeeds {

    private final Map<String, MineSpeedModifier> skyItemModifiers;
    private final Map<Material, MineSpeedModifier> itemModifiers;

    public MineSpeeds(Map<String, MineSpeedModifier> skyItemModifiers, Map<Material, MineSpeedModifier> itemModifiers) {
        this.skyItemModifiers = skyItemModifiers;
        this.itemModifiers = itemModifiers;
    }

    public MineSpeedModifier getModifier(SpecialSkyItem item) {
        return skyItemModifiers.getOrDefault(item.getItemId(), MineSpeedModifier.SELF);
    }

    public MineSpeedModifier getModifier(ItemStack item) {
        return itemModifiers.getOrDefault(item.getType(), MineSpeedModifier.SELF);
    }

    public MineSpeedModifier getModifier(ItemManager itemManager, ItemStack itemStack) {
        final SpecialSkyItem item = itemManager.getItem(itemStack);
        if (item == SpecialSkyItem.EMPTY) return this.getModifier(itemStack);
        return this.getModifier(item);
    }

    public static Serializer serializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements TypeSerializer<MineSpeeds> {

        private static final Serializer INSTANCE = new Serializer();

        private Serializer() {}

        private static final String SKY_ITEMS = "sky-items";
        private static final String MATERIALS = "materials";

        @Override
        public MineSpeeds deserialize(Type type, ConfigurationNode node) throws SerializationException {
            final var skyItemsNode = node.node(SKY_ITEMS);
            final Map<String, MineSpeedModifier> skyItems = new HashMap<>();
            for (var entry : skyItemsNode.childrenMap().entrySet()) {
                if (!(entry.getKey() instanceof final String key)) continue;
                final MineSpeedModifier modifier = MineSpeedModifier.serializer().deserialize(MineSpeedModifier.class, entry.getValue());
                skyItems.put(key, modifier);
            }
            final var materialsNode = node.node(MATERIALS);
            final Map<Material, MineSpeedModifier> materials = new EnumMap<>(Material.class);
            for (var entry : materialsNode.childrenMap().entrySet()) {
                if (!(entry.getKey() instanceof final String key)) continue;
                final MineSpeedModifier modifier = MineSpeedModifier.serializer().deserialize(MineSpeedModifier.class, entry.getValue());
                materials.put(Material.valueOf(key), modifier);
            }
            return new MineSpeeds(skyItems, materials);
        }

        @Override
        public void serialize(Type type, @Nullable MineSpeeds obj, ConfigurationNode node) throws SerializationException {

        }
    }
}
