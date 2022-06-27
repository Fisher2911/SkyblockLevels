package io.github.fisher2911.skyblocklevels.item.impl.bridger;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.ItemSerializer;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.user.User;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.function.Supplier;

public class LimitedBridger extends Bridger {

    private final Material type;
    private final int size;
    private int storedBlocks;

    public LimitedBridger(SkyblockLevels plugin, long id, String itemId, ItemSupplier itemSupplier, Material type, int size, int storedBlocks) {
        super(plugin, id, itemId, itemSupplier);
        this.type = type;
        this.size = size;
        this.storedBlocks = storedBlocks;
    }

    @Override
    public void onUse(User user, PlayerInteractEvent event) {
        event.setCancelled(true);
        final int placed = this.place(event, this.type, Math.min(this.storedBlocks, this.size));
        if (placed <= 0) return;
        user.sendMessage("Placed " + placed + " blocks");
        this.storedBlocks -= placed;
        this.storedBlocks = Math.max(0, this.storedBlocks);
        event.getPlayer().getInventory().setItemInMainHand(this.plugin.getItemManager().getItem(this));
    }

    @Override
    protected String getUsesLeft() {
        return String.valueOf(this.storedBlocks);
    }

    @Override
    public boolean uniqueInInventory() {
        return true;
    }

    public static Serializer serializer() {
        return Serializer.INSTANCE;
    }

    public static final class Serializer implements TypeSerializer<Supplier<LimitedBridger>> {

        private static final Serializer INSTANCE = new Serializer();

        private Serializer() {}

        private static final String ITEM_ID = "item-id";
        private static final String ITEM = "item";
        private static final String TYPE = "type";
        private static final String SIZE = "size";
        private static final String STORED_BLOCKS = "stored-blocks";

        @Override
        public Supplier<LimitedBridger> deserialize(Type type, ConfigurationNode node) {
            try {
                final String itemId = node.node(ITEM_ID).getString();
                final ItemSupplier itemSupplier = ItemSerializer.deserialize(node.node(ITEM));
                final Material material = Material.valueOf(node.node(TYPE).getString());
                final int size = node.node(SIZE).getInt();
                final int storedBlocks = node.node(STORED_BLOCKS).getInt();
                final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
                return () -> new LimitedBridger(plugin, plugin.getItemManager().generateNextId(), itemId, itemSupplier, material, size, storedBlocks);
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void serialize(Type type, @Nullable Supplier<LimitedBridger> obj, ConfigurationNode node) throws SerializationException {

        }
    }
}
