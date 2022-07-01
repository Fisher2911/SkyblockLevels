package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.function.Supplier;

public class SkyItem implements SpecialSkyItem {

    public static SkyItem EMPTY = new SkyItem(-1, "", ItemBuilder.EMPTY);

    public SkyItem(long id, String itemId, ItemSupplier itemSupplier) {
        this.id = id;
        this.itemId = itemId;
        this.itemSupplier = itemSupplier;
    }

    private final long id;
    private final String itemId;
    private final ItemSupplier itemSupplier;

    @Override
    public ItemStack getItemStack() {
        return this.itemSupplier.get();
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
        return this.id != -1;
    }

    public static Serializer serializer() {
        return new Serializer();
    }

    public static class Serializer implements TypeSerializer<Supplier<SkyItem>> {

        private static final Serializer INSTANCE = new Serializer();

        private Serializer() {}

        private static final String ITEM_ID = "item_id";
        private static final String ITEM = "item";
        private static final String UNIQUE = "unique";

        @Override
        public Supplier<SkyItem> deserialize(Type type, ConfigurationNode node) {
            try {
                final String itemId = node.node(ITEM_ID).getString();
                final ItemSupplier itemSupplier = ItemSerializer.deserialize(node.node(ITEM));
                final boolean unique = node.node(UNIQUE).getBoolean();
                final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
                return () -> new SkyItem(unique ? plugin.getItemManager().generateNextId() : -1, itemId, itemSupplier);
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void serialize(Type type, @Nullable Supplier<SkyItem> obj, ConfigurationNode node) throws SerializationException {

        }
    }

}
